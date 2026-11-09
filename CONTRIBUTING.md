# Contributing to the Visor Development Kit

Thank you for helping. The VDK is a promise to every app built on it, and these rules keep it.

## Before you start

- For a question, ask in [Discussions](https://github.com/macedonga/visor-kit/discussions/categories/q-a).
- For a bug, open an issue with the bug form, with the smallest code that shows it. `visor trace` shows every call
  and event between your app and Visor.
- For a new method, topic or field, open an issue first and say what you want to build. A pull request that nobody
  asked for may wait a long time.
- Read [CLAUDE.md](CLAUDE.md), above all its compatibility rules. A released method never changes its meaning.

## Checks

`./gradlew check` runs the tests and the compatibility checks against the last release. It must pass.

For anything that touches the glasses, also try it with `visor fake` and its Display simulator, and on real glasses
when you can, and say what you tried in the pull request.

## Docs

The docs live only in the site repo, [visor-site](https://github.com/macedonga/visor-site), under
`src/content/docs`. A change that adds or changes what an app can do comes with its docs change there.

## Rules for the code

- Keep it small. Short comments that say why, not what. No planning documents.
- Nothing is sent anywhere: no backend, no analytics, no crash reporting.
- Every capability is a method, a topic or a feed, the same from every language, behind a scope the person grants.
- Dependency versions live only in `gradle/libs.versions.toml`. Use the latest stable release that is at least two
  weeks old. Add a dependency only when the platform and the existing code cannot do the job.
- Match the code around your change: names, structure, the way errors are handled.

## Commits and pull requests

- One logical change per commit. The subject is imperative and under about 70 characters. The body, when there is
  one, says why.
- One topic per pull request. Fill in the template, and add a line to `CHANGELOG.md` under `## Unreleased`.
- The maintainer may squash, edit or close a pull request. Closing one is not a judgment on you.

## License

The VDK is under the [Apache License 2.0](LICENSE). By sending a contribution you agree that it is published under
the same license.

## Conduct

Everyone who takes part follows the [Code of Conduct](CODE_OF_CONDUCT.md).
