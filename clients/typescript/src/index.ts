/**
 * The Visor Development Kit from TypeScript: JSON-RPC through Visor's network door, over a WebSocket, in browsers and
 * in Node 22 or later; or, in a web app on the Meta Ray-Ban Display, through the Display's browser.
 *
 *     const visor = await Visor.connect("ws://127.0.0.1:7333/rpc", token);
 *     console.log(await visor.call("glasses.status"));
 *     visor.subscribe("band.gestures", {}, (gesture) => console.log(gesture));
 *
 *     const onDisplay = await Visor.connectGlasses();   // in a web app on the Display
 */

import { GlassesTransport, onGlasses } from "./glasses.js";

export { onGlasses } from "./glasses.js";

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

/** How messages travel between a client and Visor: whole JSON-RPC messages, in order, both ways. */
export interface Transport {
  send(message: string): void;
  onMessage(receive: (message: string) => void): void;
  onClose(ended: () => void): void;
  close(): void;
}

class SocketTransport implements Transport {
  constructor(private readonly socket: WebSocket) {}

  send(message: string): void {
    this.socket.send(message);
  }

  onMessage(receive: (message: string) => void): void {
    this.socket.addEventListener("message", (e) => receive(String(e.data)));
  }

  onClose(ended: () => void): void {
    this.socket.addEventListener("close", () => ended());
  }

  close(): void {
    this.socket.close();
  }
}

/** One connection to Visor. */
export class Visor {
  /** Visor's answer to the hello: its version, its methods and topics, and the scopes this client holds. */
  welcome: unknown = null;

  private nextId = 1;
  private readonly calls = new Map<number, Pending>();
  private readonly streams = new Map<number, Stream>();

  private constructor(private readonly transport: Transport) {
    transport.onMessage((message) => this.dispatch(JSON.parse(message)));
    transport.onClose(() => {
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
    return Visor.over(new SocketTransport(socket), "typescript");
  }

  /**
   * Connects from a web app on the Meta Ray-Ban Display, through the Display's browser and the phone's Visor. The
   * person allows the app in Visor, Settings > Apps > the app > Visor access.
   *
   * Rejects with a `VisorError`: `unsupported` outside the Display's browser (check `onGlasses()` first), `refused`
   * with the reason `not_allowed` when the app is not allowed, and `visor_gone` when Visor does not answer within
   * `timeoutMs`.
   */
  static async connectGlasses(timeoutMs: number = 10_000): Promise<Visor> {
    if (!onGlasses()) {
      throw new VisorError({ code: "unsupported", message: "This page is not running in the Meta Ray-Ban Display's browser." });
    }
    let transport: GlassesTransport;
    try {
      transport = await GlassesTransport.open();
    } catch (e) {
      throw new VisorError({
        code: "refused",
        reason: "not_allowed",
        message: `The glasses did not let this web app reach Visor (${(e as Error).message}). ` +
          "On the phone, in Visor: Settings > Apps > this app > Visor access.",
      });
    }
    let timer: ReturnType<typeof setTimeout> | undefined;
    const late = new Promise<never>((_, reject) => {
      timer = setTimeout(() => {
        transport.close();
        reject(new VisorError({
          code: "visor_gone",
          message: "Visor did not answer. Check that Visor runs on the phone, and that this web app has Visor access in " +
            "Settings > Apps.",
        }));
      }, timeoutMs);
    });
    try {
      return await Promise.race([Visor.over(transport, "web"), late]);
    } finally {
      clearTimeout(timer);
    }
  }

  /** Says hello over [transport], which any other way to reach Visor can supply. */
  static async over(transport: Transport, kit: string = "typescript"): Promise<Visor> {
    const visor = new Visor(transport);
    visor.welcome = await visor.call("kit.hello", { protocol: PROTOCOL, kit });
    return visor;
  }

  /** Connects to the FakeVisor that `visor fake` runs on this computer, for building without a phone. */
  static connectFake(port: number = 7333): Promise<Visor> {
    return Visor.connect(`ws://127.0.0.1:${port}/rpc`, "fake");
  }

  /**
   * True when this Visor answers the method, topic or feed `name`, as its hello said: check before using something
   * newer than the oldest Visor you support.
   */
  supports(name: string): boolean {
    const w = (this.welcome ?? {}) as { methods?: string[]; topics?: string[]; feeds?: string[] };
    return [w.methods, w.topics, w.feeds].some((list) => list?.includes(name) ?? false);
  }

  /** Runs `method` with `params` and resolves with its result; rejects with a `VisorError`. */
  call<T = unknown>(method: string, params: object = {}): Promise<T> {
    const id = this.nextId++;
    return new Promise<T>((resolve, reject) => {
      this.calls.set(id, { resolve: resolve as (r: unknown) => void, reject });
      this.transport.send(JSON.stringify({ jsonrpc: "2.0", id, method, params }));
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
    this.transport.send(JSON.stringify({ jsonrpc: "2.0", id, method: "kit.subscribe", params: { topic, params } }));
    return {
      close: () => {
        if (this.streams.delete(id)) this.call("kit.unsubscribe", { subscription: id }).catch(() => undefined);
      },
    };
  }

  /**
   * Calls `onRecord` with each record of the feed `name`, such as `camera.live`: its header, and its payload as bytes.
   * Ends as `subscribe` does.
   */
  feed<H = unknown>(name: string, params: object, onRecord: (header: H, payload: Uint8Array) => void, onEnd?: (error?: VisorError) => void): Subscription {
    return this.subscribe<{ header: H; payload: string }>(name, params, (record) => {
      onRecord(record.header, Uint8Array.from(atob(record.payload), (c) => c.charCodeAt(0)));
    }, onEnd);
  }

  close(): void {
    this.transport.close();
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
