package com.macedonga.visor.vdk.ipc;

// The app's side of a session, which Visor calls. Frozen like IVdkService.
oneway interface IVdkClient {
    void result(long id, String result) = 1;

    // A Failure, as JSON.
    void failure(long id, String failure) = 2;

    void event(long subscription, String event) = 3;

    // The stream is over: failure is a Failure as JSON, or null when the stream simply ended.
    void ended(long subscription, String failure) = 4;
}
