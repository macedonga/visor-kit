"""Shows a text file on the Display's teleprompter, one paragraph at a time, turned by the band or the touchpad.

    python pager.py talk.txt            # through adb, with Developer mode on in Visor
    python pager.py talk.txt --fake     # on `visor fake`

Next: a band index tap or swipe right, or a swipe forward on the touchpad.
Back: a band middle tap or swipe left, or a swipe backward on the touchpad.
"""

import asyncio
import sys

import visor_kit

NEXT = {("index", "tap"), ("index", "click"), ("thumb", "right"), ("touchpad", "swipe_forward")}
BACK = {("middle", "tap"), ("middle", "click"), ("thumb", "left"), ("touchpad", "swipe_backward")}


async def turns(visor, moves):
    """Puts each gesture and touch on ``moves`` as (who, what)."""

    async def follow(topic, who, what):
        async for event in visor.subscribe(topic):
            await moves.put((event[who], event.get(what) or event["action"]))

    await asyncio.gather(follow("band.gestures", "finger", "derived"), follow("glasses.inputs", "source", "action"))


async def main(path, fake):
    with open(path, encoding="utf-8") as f:
        pages = [p.strip() for p in f.read().split("\n\n") if p.strip()]
    if not pages:
        sys.exit(f"{path} has no text.")
    visor = await (visor_kit.connect_fake() if fake else visor_kit.connect_adb())
    async with visor:
        moves = asyncio.Queue()
        follower = asyncio.create_task(turns(visor, moves))
        page = 0
        try:
            while True:
                await visor.call("display.prompter.show", {"text": pages[page]})
                print(f"Page {page + 1} of {len(pages)}", flush=True)
                move = await moves.get()
                if move in NEXT and page < len(pages) - 1:
                    page += 1
                elif move in BACK and page > 0:
                    page -= 1
        finally:
            follower.cancel()
            await visor.call("display.prompter.close")


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if a != "--fake"]
    if len(args) != 1:
        sys.exit(__doc__)
    try:
        asyncio.run(main(args[0], "--fake" in sys.argv))
    except KeyboardInterrupt:
        pass
