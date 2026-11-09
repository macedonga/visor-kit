# visor: the VDK from your computer

A command for your computer. It reaches Visor on the phone through adb, so you can call any method, watch any
stream, see what your app sends, and stand in for the glasses or the band. With no phone at all, `visor fake`
runs a fake Visor with a Display simulator in your browser.

| Needs | |
|---|---|
| Java 17 or later, and adb | Android Studio brings both |
| Developer mode on in Visor | For a phone; `visor fake` needs nothing |

Download `visor-<version>.zip` from the [releases](https://github.com/macedonga/visor-kit/releases), unzip it, and
put its `bin` folder on your PATH. From a clone: `./gradlew :cli:installDist`, then
`tools/cli/build/install/visor/bin/visor`.

## Commands

| Command | What it does |
|---|---|
| `visor status` | Visor, the glasses, the band, and the apps that use the VDK. |
| `visor call METHOD [JSON]` | Runs a method and prints its result. `@FILE` reads the params from a file. |
| `visor watch TOPIC [JSON]` | Prints each event as it comes. |
| `visor trace` | Every call, answer and event between the apps and Visor. |
| `visor simulate TOPIC` | Apps get only the events you type, such as `index double_tap` for `band.gestures`. |
| `visor inject TOPIC EVENT` | Sends one made-up event. |
| `visor record FILE`, `visor replay FILE` | Save what the wearer does once, then play it back. |
| `visor card FILE` | Puts a Briefing card on the Display from a file. |
| `visor live FILE`, `visor live-photo FILE` | Save the live camera, or a photo taken while it runs. |
| `visor apps`, `visor grant`, `visor revoke` | The apps that use the VDK, and their scopes without the consent screen. |
| `visor methods` | What this Visor answers. |
| `visor fake` | FakeVisor on this computer: JSON-RPC and MCP on port 7333, the Display simulator at `/display`, and a console that plays the glasses. |
| `visor docs [QUERY]`, `visor docs-mcp`, `visor agents` | The VDK's docs, searched here or served to a coding assistant over MCP, and guidance to save as `AGENTS.md`. |

`visor --fake ...` runs any command against `visor fake`, and `-s SERIAL` picks a phone, as for adb.

## Read more

[Testing](../../docs/testing.md).
