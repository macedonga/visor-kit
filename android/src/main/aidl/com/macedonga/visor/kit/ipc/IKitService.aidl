package com.macedonga.visor.kit.ipc;

import com.macedonga.visor.kit.ipc.IKitClient;
import com.macedonga.visor.kit.ipc.IKitSession;

// Visor's VDK service, the first thing an app binds. Frozen: a transaction code never changes, and a new method
// only ever takes the next free code.
interface IKitService {
    // A session for the calling app, which Visor answers on client. Null when Visor turns the app away.
    IKitSession open(IKitClient client) = 1;
}
