package com.macedonga.visor.vdk.ipc;

// One app's session, from IVdkService.open. Frozen like IVdkService. Every message is JSON, and every name and
// type comes from vdk-protocol; the app picks the ids, unique per session.
interface IVdkSession {
    // Runs a method. Visor answers once, on IVdkClient.result or IVdkClient.failure.
    oneway void call(long id, String method, String params) = 1;

    // Stops the call; Visor answers it no more.
    oneway void cancel(long id) = 2;

    // Streams a topic: IVdkClient.event for each event, IVdkClient.ended once when Visor ends it.
    oneway void subscribe(long id, String topic, String params) = 3;

    // Stops the stream; Visor sends no ended for it.
    oneway void unsubscribe(long id) = 4;

    // Ends the session, its calls and its streams.
    oneway void close() = 5;
}
