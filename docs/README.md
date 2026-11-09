# Visor Development Kit documentation

Start with the overview, then getting started. The rest is reference.

1. [Overview](overview.md): what the VDK is, how it works, how it compares with Meta's toolkit, the roadmap.
2. [Getting started](getting-started.md): from nothing to a photo taken with the glasses.
3. [Scopes and consent](scopes-and-consent.md): what an app may do, and how the person decides.
4. [The connection](connection.md): connecting, Visor restarts, calls and streams, versions.
5. Capabilities:
   - [Glasses](glasses.md): status, pairs, touches and presses, motion.
   - [Camera and captures](camera-and-captures.md): the camera live, photos, videos, imports.
   - [Neural Band](neural-band.md): gestures, status, motion on the wrist, vibration, and sharing the band with the
     phone.
   - [Display](display.md): the teleprompter, layouts, and cards on the Briefing.
   - [Briefing cards](briefing-cards.md): the guide, with recipes for weather, stocks, a live timer and photos.
   - [Web apps on the Display](web-apps.md): the VDK from JavaScript in a web app on the Display, and its tools for
     the assistant.
   - [Voice and the assistant](voice-and-assistant.md): questions for the app, starting it by voice, transcripts,
     tools for the model, notifications.
   - [Settings](settings.md): reading and changing the glasses' settings.
6. [Errors](errors.md): every code, and what to do about it.
7. [Testing](testing.md): the `visor` command, FakeVisor, `visor fake`, the Display simulator, recordings.
8. [JSON-RPC](json-rpc.md): the VDK from any language.
9. [The network door](network.md): JSON-RPC over WebSocket, MCP for AI agents, the Python and TypeScript clients,
   raw access.
10. [Compatibility](compatibility.md): what never breaks, and how the build makes sure.
11. [Porting from Meta's DAT](porting-from-dat.md): each part of Meta's toolkit and its VDK equivalent.
12. [AI tools](ai-tools.md): the docs search and its MCP server, guidance for coding assistants, the Claude Code
    plugin.
