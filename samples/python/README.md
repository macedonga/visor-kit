# Python samples

Small scripts on the [Python client](../../clients/python), for Python 3.11 or later. Each one runs through adb,
with Developer mode on in Visor, or on `visor fake` with `--fake`, so you can try it without a phone.

```
pip install "./clients/python[network]"
visor fake                                   # in another terminal, for --fake
```

| Script | What it does |
|---|---|
| `pager.py FILE` | Shows a text file on the Display's teleprompter, a paragraph at a time, turned by the band or the touchpad. |
| `journal.py [FILE] [--history]` | Writes each question asked of the assistant, and its answer, to a Markdown journal. |
| `settings.py list [GROUP]`, `settings.py set KEY VALUE` | Lists the glasses' settings, or changes one and waits for the glasses to confirm. |

On `visor fake`, play the wearer in its console: `gesture index tap` or `input touchpad swipe_forward` turns the
pager, `turn What is on today / Two meetings` adds to the journal.
