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

    // Stops the stream; Visor sends no ended for it.
    oneway void unsubscribe(long id) = 4;

    // Ends the session, its calls and its streams.
    oneway void close() = 5;
}
