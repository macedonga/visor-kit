# Android sample

An app that shows the VDK at work: the glasses' and the band's status as they change, and buttons for a photo, a
video, the captures, a photo while the camera is live, the band's buzz, the sound settings, the assistant's last
questions, the teleprompter and Briefing cards. Each press goes in a log with what Visor answered.

```
./gradlew :sample:installDebug
```

Open it, allow its scopes on Visor's consent screen, and press a button. Run `visor trace` on your computer
alongside to see each exchange with Visor.

The Briefing buttons run the recipes of [Briefing cards](https://visor.macedonga.com/developers/docs/briefing-cards/): a weather card, a stocks
watchlist, and a kitchen timer with buttons in its detail. Their code is in `Cards.kt`.
