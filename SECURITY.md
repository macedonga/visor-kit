# Security policy

## Supported versions

Only the latest release gets security fixes. Builds from `main` get them as soon as they land.

## Report a vulnerability

Do not open a public issue for a security problem.

Report it privately: on this repository, open the **Security** tab and select **Report a vulnerability**. Only you
and the maintainer can read the report.

Include:

- What an attacker can do, and what they need first: an app on the phone, a program on the same network, or a
  nearby device.
- The VDK version, the Visor version, the phone and Android version.
- The steps to reproduce it, or a proof of concept.

Remove serial numbers, Bluetooth addresses and tokens from logs before you attach them.

The maintainer answers as soon as possible and agrees a disclosure date with you. Please wait for a fix or for that
date before you publish.

## Scope

In scope, because the VDK's code decides them:

- Consent: anything that lets an app use a scope the person did not grant it, or keep one they took back.
- The network door: its tokens, and what a client can reach without the right one.
- The `visor` command and `visor fake`: what they open on the computer, and to whom.
- The libraries and clients: how they handle what arrives from Visor.
- The releases: the signed Maven artifacts, the npm and PyPI packages, the `visor` zip and its install scripts, and
  the workflow that builds them (`.github/workflows/release.yml`).

Out of scope:

- Problems in the Visor app itself: report those in [Visor's](https://github.com/macedonga/visor/security/policy)
  Security tab.
- Problems in the glasses' firmware, in Meta's apps or in Meta's servers. Report those to
  [Meta's bug bounty program](https://bugbounty.meta.com/).
- Attacks that need a rooted phone, or a phone that is already unlocked in the attacker's hands.

## Check a release

The Maven artifacts are signed with the release key, and Maven Central checks the signatures on upload. The npm and
PyPI packages and the `visor` zip are published only by this repository's release workflow.
