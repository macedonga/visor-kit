"""Writes every question asked of the assistant, and its answer, to a Markdown journal, one section per day.

    python journal.py                      # through adb, into journal.md, until Ctrl+C
    python journal.py notes.md --history   # the turns Visor kept first, then each new one
    python journal.py --fake               # on `visor fake`

Needs the `conversations` scope; the adb and fake connections hold every scope.
"""

import asyncio
import datetime
import os
import sys

import visor_kit


def entry(turn):
    when = datetime.datetime.fromtimestamp(turn["time"] / 1000)
    answer = turn["answer"] or f"*(no answer: {turn['failure'] or 'unknown'})*"
    photo = " (with a photo)" if turn.get("photo") else ""
    return when.date().isoformat(), f"**{when:%H:%M}** {turn['question']}{photo}\n\n> {answer}\n\n"


def append(path, turn):
    day, text = entry(turn)
    heading = f"## {day}\n\n"
    have = open(path, encoding="utf-8").read() if os.path.exists(path) else ""
    with open(path, "a", encoding="utf-8") as f:
        if heading not in have:
            f.write(heading)
        f.write(text)


async def main(path, history, fake):
    visor = await (visor_kit.connect_fake() if fake else visor_kit.connect_adb())
    async with visor:
        turns = await visor.open("assistant.turns")
        if history:
            for turn in reversed(await visor.call("assistant.history", {"limit": 500})):
                append(path, turn)
        print(f"Writing to {path}. Ctrl+C stops.", flush=True)
        async for turn in turns:
            append(path, turn)
            print(f"{turn['question']} -> {turn['answer'] or turn['failure']}", flush=True)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    try:
        asyncio.run(main(args[0] if args else "journal.md", "--history" in sys.argv, "--fake" in sys.argv))
    except KeyboardInterrupt:
        pass
