package com.macedonga.visor.vdk.ipc;

import com.macedonga.visor.vdk.ipc.IVdkClient;
import com.macedonga.visor.vdk.ipc.IVdkSession;

// Visor's VDK service, the first thing an app binds. Frozen: a transaction code never changes, and a new method
// only ever takes the next free code.
interface IVdkService {
    // A session for the calling app, which Visor answers on client. Null when Visor turns the app away.
    IVdkSession open(IVdkClient client) = 1;
}
