"""Lists and changes the glasses' settings.

    python settings.py list [GROUP]        # every setting, or one group's, such as Camera
    python settings.py set KEY VALUE       # then waits until the glasses report the new value
    python settings.py ... --fake          # on `visor fake`

Needs the `settings` scope; the adb and fake connections hold every scope.
"""

import asyncio
import sys

import visor_kit

SETTLE_S = 10


def show(s):
    value = s["value"] if s["value"] is not None else "?"
    choice = {o["value"]: o["label"] for o in s.get("options", [])}.get(value)
    label = s.get("label") or s["key"]
    lock = "" if s.get("writable") else "  (read only)"
    return f"{label}: {value}" + (f" ({choice})" if choice else "") + f"  [{s['key']}]{lock}"


async def main(args, fake):
    visor = await (visor_kit.connect_fake() if fake else visor_kit.connect_adb())
    async with visor:
        if args[0] == "list":
            query = {"group": args[1]} if len(args) > 1 else {}
            group = None
            for s in await visor.call("settings.list", query):
                if s.get("group") != group:
                    group = s.get("group")
                    print(f"\n{group or 'Other'}")
                print("  " + show(s))
        elif args[0] == "set" and len(args) == 3:
            changes = await visor.open("settings.changes", {"keys": [args[1]]})
            print("Sent: " + show(await visor.call("settings.set", {"key": args[1], "value": args[2]})))
            try:
                async with asyncio.timeout(SETTLE_S):
                    async for s in changes:
                        if not s["pending"]:
                            print("Now: " + show(s))
                            return
            except TimeoutError:
                print(f"The glasses did not report {args[1]} again within {SETTLE_S} s.")
            finally:
                await changes.aclose()
        else:
            sys.exit(__doc__)


if __name__ == "__main__":
    words = [a for a in sys.argv[1:] if a != "--fake"]
    if not words:
        sys.exit(__doc__)
    try:
        asyncio.run(main(words, "--fake" in sys.argv))
    except visor_kit.VisorError as e:
        sys.exit(f"Visor: {e}")
