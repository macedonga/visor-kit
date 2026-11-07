/**
 * The Visor Development Kit from TypeScript, through Visor's network door: JSON-RPC over a WebSocket. Runs in
 * browsers and in Node 22 or later, which have WebSocket built in.
 *
 *     const visor = await Visor.connect("ws://127.0.0.1:7333/rpc", token);
 *     console.log(await visor.call("glasses.status"));
 *     visor.subscribe("band.gestures", {}, (gesture) => console.log(gesture));
 */

export const PROTOCOL = 1;

/** A failure, as the kit sends it. */
export interface Failure {
  code: string;
  message: string;
  scope?: string;
  reason?: string;
}

/** A failed call or stream. `code` is the kit's error code, such as `scope_denied`. */
export class VisorError extends Error {
  readonly code: string;
  readonly scope?: string;
  readonly reason?: string;

  constructor(failure: Failure) {
    super(`${failure.code}: ${failure.message}`);
    this.code = failure.code;
    this.scope = failure.scope;
    this.reason = failure.reason;
  }
}

export interface Subscription {
  /** Stops the stream. */
  close(): void;
}

interface Pending {
  resolve(result: unknown): void;
  reject(error: VisorError): void;
}

interface Stream {
  onEvent(event: unknown): void;
  onEnd?(error?: VisorError): void;
}

const GONE: Failure = { code: "visor_gone", message: "The connection to Visor broke." };

/** One connection to Visor's network door. */
export class Visor {
  /** Visor's answer to the hello: its version, its methods and topics, and the scopes this client holds. */
  welcome: unknown = null;

  private nextId = 1;
  private readonly calls = new Map<number, Pending>();
  private readonly streams = new Map<number, Stream>();

  private constructor(private readonly socket: WebSocket) {
    socket.addEventListener("message", (e) => this.dispatch(JSON.parse(String(e.data))));
    socket.addEventListener("close", () => {
      this.calls.forEach((c) => c.reject(new VisorError(GONE)));
      this.streams.forEach((s) => s.onEnd?.(new VisorError(GONE)));
      this.calls.clear();
      this.streams.clear();
    });
  }

  /** Connects to `url`, such as `ws://127.0.0.1:7333/rpc`, with a client's `token`, and says hello. */
  static async connect(url: string, token: string): Promise<Visor> {
    const socket = new WebSocket(`${url}?token=${encodeURIComponent(token)}`);
    await new Promise<void>((resolve, reject) => {
      socket.addEventListener("open", () => resolve(), { once: true });
      socket.addEventListener("error", () => reject(new VisorError(GONE)), { once: true });
    });
    const visor = new Visor(socket);
    visor.welcome = await visor.call("kit.hello", { protocol: PROTOCOL, kit: "typescript" });
    return visor;
  }

  /** Runs `method` with `params` and resolves with its result; rejects with a `VisorError`. */
  call<T = unknown>(method: string, params: object = {}): Promise<T> {
    const id = this.nextId++;
    return new Promise<T>((resolve, reject) => {
      this.calls.set(id, { resolve: resolve as (r: unknown) => void, reject });
      this.socket.send(JSON.stringify({ jsonrpc: "2.0", id, method, params }));
    });
  }

  /** Calls `onEvent` with each event of `topic` until the stream ends, which `onEnd` hears with any failure. */
  subscribe<E = unknown>(topic: string, params: object, onEvent: (event: E) => void, onEnd?: (error?: VisorError) => void): Subscription {
    const id = this.nextId++;
    this.streams.set(id, { onEvent: onEvent as (e: unknown) => void, onEnd });
    this.calls.set(id, {
      resolve: () => this.calls.delete(id),
      reject: (error) => {
        this.calls.delete(id);
        this.streams.delete(id);
        onEnd?.(error);
      },
    });
    this.socket.send(JSON.stringify({ jsonrpc: "2.0", id, method: "kit.subscribe", params: { topic, params } }));
    return {
      close: () => {
        if (this.streams.delete(id)) this.call("kit.unsubscribe", { subscription: id }).catch(() => undefined);
      },
    };
  }

  close(): void {
    this.socket.close();
  }

  private dispatch(message: { id?: number; method?: string; params?: any; result?: unknown; error?: { message: string; data?: Failure } }): void {
    if (message.method === undefined) {
      const pending = message.id === undefined ? undefined : this.calls.get(message.id);
      if (pending === undefined) return;
      this.calls.delete(message.id!);
      if (message.error) pending.reject(new VisorError(message.error.data ?? { code: "failed", message: message.error.message }));
      else pending.resolve(message.result);
      return;
    }
    const stream = this.streams.get(message.params?.subscription);
    if (stream === undefined) return;
    if (message.method === "kit.event") {
      stream.onEvent(message.params.event);
    } else if (message.method === "kit.ended") {
      this.streams.delete(message.params.subscription);
      stream.onEnd?.(message.params.failure ? new VisorError(message.params.failure) : undefined);
    }
  }
}
