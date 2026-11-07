package com.macedonga.visor.kit.ipc;

// One app's session, from IKitService.open. Frozen like IKitService. Every message is JSON, and every name and
// type comes from kit-protocol; the app picks the ids, unique per session.
interface IKitSession {
    // Runs a method. Visor answers once, on IKitClient.result or IKitClient.failure.
    oneway void call(long id, String method, String params) = 1;

    // Stops the call; Visor answers it no more.
    oneway void cancel(long id) = 2;

    // Streams a topic: IKitClient.event for each event, IKitClient.ended once when Visor ends it.
    oneway void subscribe(long id, String topic, String params) = 3;

    // Stops the stream or the feed; Visor sends no ended for it.
    oneway void unsubscribe(long id) = 4;

    // Ends the session, its calls, its streams and its feeds.
    oneway void close() = 5;

    // Sends a feed's records to sink, one end of a socket pair the app reads, framed as KitProtocol.FEED_END says.
    // The last record ends the feed; IKitClient.ended is not called for it.
    oneway void openFeed(long id, String feed, String params, in ParcelFileDescriptor sink) = 6;
}
