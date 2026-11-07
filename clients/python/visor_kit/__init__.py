"""The Visor Development Kit from Python: Visor's methods and topics over JSON-RPC.

Two ways in:

- ``connect_adb()`` reaches Visor's adb socket through ``adb forward``. It needs adb and Developer mode on in Visor,
  and holds every scope.
- ``connect_network(url, token)`` reaches Visor's network door with a client's token. It needs the ``websockets``
  package, and holds the client's scopes.

    import asyncio, visor_kit

    async def main():
        async with await visor_kit.connect_adb() as visor:
            print(await visor.call("glasses.status"))
            async for gesture in visor.subscribe("band.gestures"):
                print(gesture["finger"], gesture["derived"])

    asyncio.run(main())
"""

import asyncio
import base64
import itertools
import json
import os
import shutil
import subprocess

__all__ = ["Visor", "VisorError", "connect_adb", "connect_network"]

PROTOCOL = 1
SOCKET = "visor-kit"

# A feed's record is one JSON message, and a keyframe in Base64 runs to megabytes.
MAX_MESSAGE = 64 << 20


class VisorError(Exception):
    """A failed call or stream. ``code`` is the kit's error code, such as ``scope_denied``."""

    def __init__(self, failure):
        self.code = failure.get("code", "failed")
        self.reason = failure.get("reason")
        self.scope = failure.get("scope")
        super().__init__(f"{self.code}: {failure.get('message', '')}")


class Visor:
    """One connection to Visor. Use ``connect_adb`` or ``connect_network`` to make one."""

    def __init__(self, send, receive, close):
        self._send, self._receive, self._close = send, receive, close
        self._ids = itertools.count(1)
        self._calls = {}
        self._streams = {}
        self._reader = asyncio.ensure_future(self._read())
        self.welcome = None

    async def call(self, method, params=None):
        """Runs ``method`` with ``params`` and returns its result. Raises ``VisorError``."""
        id = next(self._ids)
        answer = asyncio.get_running_loop().create_future()
        self._calls[id] = answer
        try:
            await self._send(json.dumps({"jsonrpc": "2.0", "id": id, "method": method, "params": params or {}}))
            return await answer
        finally:
            self._calls.pop(id, None)

    async def subscribe(self, topic, params=None):
        """Yields each event of ``topic`` until the stream ends; a failure raises ``VisorError``."""
        id = next(self._ids)
        events = asyncio.Queue()
        self._streams[id] = events
        answer = asyncio.get_running_loop().create_future()
        self._calls[id] = answer
        await self._send(json.dumps({"jsonrpc": "2.0", "id": id, "method": "kit.subscribe",
                                     "params": {"topic": topic, "params": params or {}}}))
        try:
            try:
                await answer
            finally:
                self._calls.pop(id, None)
            while True:
                kind, value = await events.get()
                if kind == "event":
                    yield value
                elif value is None:
                    return
                else:
                    raise VisorError(value)
        finally:
            if self._streams.pop(id, None) is not None:
                try:
                    await self.call("kit.unsubscribe", {"subscription": id})
                except VisorError:
                    pass  # Visor is gone, and the stream with it.

    async def feed(self, name, params=None):
        """Yields ``(header, payload)`` for each record of the feed ``name``, such as ``camera.live``: the header as a
        dict, the payload as bytes. Ends as ``subscribe`` does."""
        records = self.subscribe(name, params)
        try:
            async for record in records:
                yield record["header"], base64.b64decode(record["payload"])
        finally:
            await records.aclose()

    async def close(self):
        self._reader.cancel()
        await self._close()

    async def __aenter__(self):
        return self

    async def __aexit__(self, *_):
        await self.close()

    async def _read(self):
        failure = {"code": "visor_gone", "message": "The connection to Visor broke."}
        try:
            while (message := await self._receive()) is not None:
                self._dispatch(json.loads(message))
        finally:
            for answer in self._calls.values():
                if not answer.done():
                    answer.set_exception(VisorError(failure))
            for events in self._streams.values():
                events.put_nowait(("ended", failure))

    def _dispatch(self, message):
        if "method" not in message:
            answer = self._calls.get(message.get("id"))
            if answer is None or answer.done():
                return
            if "error" in message:
                error = message["error"]
                answer.set_exception(VisorError(error.get("data") or {"code": "failed", "message": error.get("message")}))
            else:
                answer.set_result(message.get("result"))
            return
        params = message.get("params") or {}
        events = self._streams.get(params.get("subscription"))
        if events is None:
            return
        if message["method"] == "kit.event":
            events.put_nowait(("event", params.get("event")))
        elif message["method"] == "kit.ended":
            self._streams.pop(params.get("subscription"), None)
            events.put_nowait(("ended", params.get("failure")))


async def _hello(visor, name):
    visor.welcome = await visor.call("kit.hello", {"protocol": PROTOCOL, "kit": name})
    return visor


def _adb():
    for variable in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        home = os.environ.get(variable)
        if home:
            path = os.path.join(home, "platform-tools", "adb.exe" if os.name == "nt" else "adb")
            if os.path.isfile(path):
                return path
    return shutil.which("adb") or "adb"


async def connect_adb(serial=None):
    """Connects to Visor through adb. ``serial`` picks a device, as ``adb -s`` does."""
    adb = [_adb()] + (["-s", serial] if serial else [])
    port = int(subprocess.run(adb + ["forward", "tcp:0", f"localabstract:{SOCKET}"],
                              check=True, capture_output=True, text=True).stdout.strip())
    reader, writer = await asyncio.open_connection("127.0.0.1", port, limit=MAX_MESSAGE)

    async def send(text):
        writer.write(text.encode() + b"\n")
        await writer.drain()

    async def receive():
        line = await reader.readline()
        return line.decode() if line else None

    async def close():
        writer.close()
        subprocess.run(adb + ["forward", "--remove", f"tcp:{port}"], capture_output=True)

    return await _hello(Visor(send, receive, close), "python-adb")


async def connect_network(url, token):
    """Connects to Visor's network door, such as ``ws://127.0.0.1:7333/rpc``, with a client's ``token``."""
    # The asyncio client, from websockets 13 on: the package's top-level connect is the older one there.
    from websockets.asyncio.client import connect
    from websockets.exceptions import ConnectionClosed

    socket = await connect(url, additional_headers={"Authorization": f"Bearer {token}"}, max_size=MAX_MESSAGE)

    async def receive():
        try:
            return await socket.recv()
        except ConnectionClosed:
            return None

    return await _hello(Visor(socket.send, receive, socket.close), "python-network")
