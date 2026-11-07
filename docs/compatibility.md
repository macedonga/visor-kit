# Compatibility

People update Visor and your app at different times, so the kit promises that either one can be the older side.

## The promise

- An app built against any kit release works with every later Visor.
- An app built against a newer kit works with an older Visor, for everything that Visor has. `visor.supports(...)`
  tells you what it has, and anything newer fails with `visor_too_old` instead of misbehaving.
- Upgrading the kit in your app never breaks code that compiled against an earlier release of the same major
  version, at the source level or the binary level.
- A method keeps doing what it did. New behavior comes as a new method, or as an optional field that asks for it.

## Versions

- **Library versions** (`kit`, `kit-protocol`, `kit-testing`) follow semantic versioning.
- **The protocol version** (`KitProtocol.VERSION`) is one number that each release adding a method, topic or field
  raises by one. It only goes up. Each method and topic records the version that added it in `since`.

## What changes, and how

| Change | Allowed? |
|---|---|
| A new method, topic, type or scope | Yes, with a higher protocol version. |
| A new optional field in params, a result or an event | Yes. |
| A new value in an open set (error codes, scopes, gesture actions, capture types) | Yes. Your code must treat values it does not know as unknown. |
| Removing or renaming a method, topic, type or field | No. |
| Changing a field's type, or whether it is required | No. |
| Removing public Kotlin API | Only in a major release, after a deprecation in two minor releases. |

## Writing code that lasts

- Use `else` in every `when` over `ErrorCode`, `GestureAction`, `CaptureType`, `Finger` or `Link`. They are value
  classes, not enums, because they grow.
- Treat a `null` field as "not known". Most fields are null when the glasses are not connected.
- Check `visor.supports(...)` before using a method added after the kit version you require of Visor.

## How it is enforced

The build checks every change against the last release before it can merge:

- **The wire contract.** `protocol/api/openrpc.json` describes every method, topic and type. The build compares it
  with the one in the last release and fails on anything removed or changed, on a new required field, and on
  anything added without a higher protocol version.
- **The Kotlin API.** Each library's public API is dumped to `api/*.api`. The build compares it with the one in the
  last release and fails on any line that disappeared.
- **The binder interface** between apps and Visor is frozen: new methods only ever take new transaction codes.

The full rules for contributors are in [CLAUDE.md](../CLAUDE.md).
