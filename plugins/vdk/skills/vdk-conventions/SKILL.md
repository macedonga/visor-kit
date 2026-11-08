---
name: vdk-conventions
description: Rules for code that uses the VDK: scopes, suspend calls and flows, errors, open value sets
---

# VDK conventions

- Every call is a `suspend fun`; every stream is a cold `Flow` that runs while collected. Cancel to stop.
- A call without its scope fails with `KitException` code `scope_denied`; the exception names the `scope`.
- Handle `KitException` by `code` and `reason`; treat unknown codes as `failed`.
- Scopes, error codes, input and gesture actions, and layout types are open sets: never assume the list is
  complete, and ignore values you do not know.
- Methods and topics live in `kit-protocol` as `Methods` and `Topics`. Do not invent others: search the docs with
  `search_vdk_docs`, or run `visor methods` against a phone.

The whole list is in `for-agents` (`read_vdk_doc`).
