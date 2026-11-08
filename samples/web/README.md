# Web sample

`index.html` is a console for the glasses in one page, with no build step: the glasses' status, the touchpad and
buttons, the band's gestures, the questions asked of the assistant, and the teleprompter. It speaks JSON-RPC over
a WebSocket to Visor's [network door](../../docs/network.md) in about 40 lines, which you can copy.

Try it without a phone:

```
visor fake
```

Then open `index.html` in a browser, or serve this folder with any static server, and press Connect. Type in the
fake's console to play the wearer: `gesture index tap`, `input touchpad tap`, `battery 20`,
`turn Is it raining / No`.

With a phone, turn on the network door in Visor (Advanced settings, Network access), make a client with the scopes
the page needs (`state`, `inputs`, `band`, `display`, `conversations`), and use its token. From another computer,
turn on LAN access and use the phone's address.
