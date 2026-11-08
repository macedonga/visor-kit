# AI tools

The VDK gives coding assistants what they need to write code for it, and gives AI agents the glasses themselves.

## The docs, for coding assistants

The `visor` command carries the VDK's docs:

```
visor docs layout button click     # the sections that answer best
visor docs                         # the pages
```

`visor docs-mcp` serves them as an MCP server over standard input and output, with two tools:
`search_vdk_docs` (a query, and `max_results` up to 10) and `read_vdk_doc` (a page, such as `display`). Add it to
Claude Code with:

```
claude mcp add vdk-docs -- visor docs-mcp
```

Any assistant that runs MCP servers as commands takes the same command.

## Guidance and the plugin

`visor agents` prints the rules for code that uses the VDK, the same as [for-agents.md](for-agents.md). Save it as
`AGENTS.md` in your app, which Codex, Cursor, GitHub Copilot and Claude Code read:

```
visor agents > AGENTS.md
```

For Claude Code, this repo is also a plugin marketplace. The `vdk` plugin adds skills for getting started, the
VDK's conventions, the Display, voice, testing and porting from DAT, and sets up the docs MCP server:

```
/plugin marketplace add macedonga/visor-kit
/plugin install vdk@visor-kit
```

`llms.txt` in the docs folder indexes the pages for tools that read that format.

## The glasses, for AI agents

Visor's [network door](network.md) serves MCP, so an agent can use the glasses with a token the person makes for
it. `visor fake` serves the same MCP on `http://127.0.0.1:7333/mcp`, with no glasses and no token.
