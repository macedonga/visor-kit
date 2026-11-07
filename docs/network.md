# The network door

Visor can serve the kit over the network: JSON-RPC over a WebSocket for programs, and MCP for AI agents. It is off
until the person turns it on, and every client needs a token the person made for it.

## Turning it on

In Visor, open Settings, Advanced settings, Network access:

1. Turn on **Open to clients**. Visor listens on `127.0.0.1:7333`, so only programs on the phone itself reach it
   (Termux, for example).
2. Turn on **From the local network** to let computers on the same Wi-Fi in too. The row shows the phone's address.
3. **Add a client**: give it a name and its scopes. Visor shows the client's token once; copy it then. Visor keeps
   only its hash.

Remove a client to take its access back: its streams end at once.

To reach the door from a computer without the local network, forward the port over adb:
`adb forward tcp:7333 tcp:7333`.

## JSON-RPC over WebSocket

`ws://<address>:7333/rpc`, with the token as `Authorization: Bearer <token>`, or as `?token=<token>` where a client
cannot set headers, as in a browser. The messages are the same as on the adb socket: see [JSON-RPC](json-rpc.md).
The client holds only its own scopes, and the `shell.*` methods are not there.

## MCP for AI agents

`http://<address>:7333/mcp` is an MCP server over Streamable HTTP, with the same token. Each method the client may
call is a tool, named with `_` for `.`: `glasses_status`, `camera_photo`, `display_prompter_show`, and so on. A
tool's answer is the method's result as JSON text. Streams are not tools; use JSON-RPC for them.

With Claude Code:

```
claude mcp add --transport http visor http://127.0.0.1:7333/mcp --header "Authorization: Bearer <token>"
```

Give an agent's client only the scopes it needs. An agent with `camera` takes photos when it decides to; the
glasses' capture light still turns on for each one.

## Webhooks

For a service that cannot hold a connection open, such as a home automation hub, Visor can post events instead. When
you add a client, give it a **webhook URL** and pick its topics: those its scopes allow, without params. While the
network door is open, Visor subscribes as that client and posts each event, in order, as JSON:

```json
{"client": "home", "topic": "glasses.status", "event": {"paired": true, "link": "connected"}, "time": 1794072397747}
```

Each post carries `X-Visor-Signature: sha256=<hex>`, the HMAC-SHA256 of the body. The key is the hex SHA-256 of the
client's token, which Visor keeps instead of the token:

```python
key = hashlib.sha256(token.encode()).hexdigest().encode()
valid = hmac.compare_digest("sha256=" + hmac.new(key, body, hashlib.sha256).hexdigest(), signature)
```

Visor waits 5 seconds for an answer and does not retry. When the receiver keeps failing, Visor's event log says so
once a minute.

## Clients for other languages

- **Python:** [`clients/python`](../clients/python). `connect_adb()` or `connect_network(url, token)`, then
  `await visor.call(...)` and `async for event in visor.subscribe(...)`.
- **TypeScript:** [`clients/typescript`](../clients/typescript), for browsers and Node 22 or later.
  `await Visor.connect(url, token)`, then `visor.call(...)` and `visor.subscribe(...)`.

```typescript
const visor = await Visor.connect("ws://192.168.1.20:7333/rpc", token);
visor.subscribe("glasses.status", {}, (status) => console.log(status));
```

## Raw access

Scope: `raw`. For research into the glasses' protocol: send a message to any service of the glasses and read what
a service sends, as they are.

| | Name | Params | Answer |
|---|---|---|---|
| Method | `raw.send` | `service`, `type`, `payload` (Base64) | `{}` |
| Topic | `raw.messages` | `service` | `{service, type, payload, time}` |

Visor answers only while its Developer mode is on, and refuses the services it keeps to itself: identity and
ownership (36), the link's authentication (54 and 79) and firmware updates (29). A wrong message can still upset
the glasses until they restart.
