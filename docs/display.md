# Display

Scope: `display`. The Meta Ray-Ban Display. On glasses without a display every call fails with `unsupported`.

## Teleprompter

The Display has a teleprompter: one script at a time, which it scrolls or shows a page at a time.

```kotlin
visor.display.prompter.show("Slide 3: the numbers")
visor.display.prompter.show(script, PrompterStyle { paged = true; sizeSp = 40.0 })
visor.display.prompter.close()
```

A second `show` replaces the text on the Display. `show` returns once the Display opened the teleprompter, and
fails with `refused` when the Display turns it down, `timeout` when it does not answer, and `not_connected`.

`PrompterStyle`:

| Field | Default | Meaning |
|---|---|---|
| `sizeSp` | 35 | The text size on the Display. |
| `speed` | 32 | The scroll speed, in the Display's own unit. 32 is its default. |
| `paged` | false | A page at a time instead of scrolling. |
| `followVoice` | false | Scroll as the wearer reads aloud, with the Display's own speech recognition. |

The wearer can close the teleprompter on the Display; the kit does not hear about it. Captions or translation
opening on the Display also take its place.

## JSON-RPC

| | Name | Params | Answer |
|---|---|---|---|
| Method | `display.prompter.show` | `text`, `style` (a `PrompterStyle`, optional) | `{}` |
| Method | `display.prompter.close` | none | `{}` |

```
visor call display.prompter.show '{"text":"Hello","style":{"paged":true}}'
```

More of the Display (cards on its home, web apps that reach the kit) is on the [roadmap](overview.md#roadmap).
