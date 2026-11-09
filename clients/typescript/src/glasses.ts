/**
 * The VDK from a web app on the Meta Ray-Ban Display, through the browser's Bluetooth scans. The Display's browser
 * hands a page's `navigator.bluetooth.requestLEScan` to the phone, so Visor reads each scan's filter and answers with
 * advertisements: a JSON-RPC message goes to Visor in a manufacturer-data filter, and comes back in an advertisement's
 * manufacturer data. One long scan carries everything Visor sends; each message to Visor is a scan of its own.
 *
 * The person allows it for each web app, in Visor's Settings > Apps: "Nearby Bluetooth devices", which lets the
 * page scan at all, and "Visor access", which picks the scopes.
 */

import type { Transport } from "./index.js";

/** Bluetooth SIG's company id for tests, which no product uses: Visor's frames ride on it. */
export const COMPANY = 0xffff;

/** "VDK1": the first bytes of every frame. */
const MAGIC = [0x56, 0x44, 0x4b, 0x31];

/** Bytes of message a frame carries, so a message larger than one DataX message goes in pieces. */
const CHUNK = 8000;

const MORE = 1;

interface LeScan {
  stop(): void;
}

interface Bluetooth extends EventTarget {
  requestLEScan(options: object): Promise<LeScan>;
}

/** A frame: MAGIC, a flags byte (1: more frames of this message follow), a message id (4 bytes, big endian), the bytes. */
function frame(flags: number, id: number, bytes: Uint8Array): Uint8Array {
  const out = new Uint8Array(MAGIC.length + 5 + bytes.length);
  out.set(MAGIC, 0);
  out[4] = flags;
  new DataView(out.buffer).setUint32(5, id >>> 0);
  out.set(bytes, 9);
  return out;
}

function bluetooth(): Bluetooth | undefined {
  const b = (globalThis.navigator as { bluetooth?: Bluetooth } | undefined)?.bluetooth;
  return typeof b?.requestLEScan === "function" ? b : undefined;
}

/** True in the Display's browser, where a page can reach Visor through its scans. */
export function onGlasses(): boolean {
  return bluetooth() !== undefined;
}

/** The transport `Visor.connectGlasses` uses. */
export class GlassesTransport implements Transport {
  private readonly session = crypto.randomUUID();
  private nextId = Math.floor(Math.random() * 0x7fffffff);
  private queue: Promise<unknown> = Promise.resolve();
  private readonly pieces = new Map<number, Uint8Array[]>();
  private receive: (message: string) => void = () => undefined;
  private ended: () => void = () => undefined;
  private scan?: LeScan;
  private readonly listener = (e: Event) => this.onAdvertisement(e as Event & { manufacturerData?: Map<number, DataView> });

  private constructor(private readonly bt: Bluetooth) {}

  /** Opens the session scan, which carries Visor's answers and events from here on. */
  static async open(): Promise<GlassesTransport> {
    const bt = bluetooth();
    if (bt === undefined) throw new Error("No navigator.bluetooth.requestLEScan: this page is not in the Display's browser.");
    const transport = new GlassesTransport(bt);
    bt.addEventListener("advertisementreceived", transport.listener);
    try {
      transport.scan = await transport.request({ open: true }, true);
    } catch (e) {
      bt.removeEventListener("advertisementreceived", transport.listener);
      throw e;
    }
    return transport;
  }

  onMessage(receive: (message: string) => void): void {
    this.receive = receive;
  }

  onClose(ended: () => void): void {
    this.ended = ended;
  }

  /** Sends one JSON-RPC message as scans, in order: each scan starts, then stops at once. */
  send(message: string): void {
    const body = { rpc: JSON.parse(message) };
    this.queue = this.queue.then(() => this.request(body, false).then((scan) => scan.stop())).catch(() => this.close());
  }

  close(): void {
    this.bt.removeEventListener("advertisementreceived", this.listener);
    this.scan?.stop();
    this.scan = undefined;
    this.ended();
  }

  // A message to Visor: one scan, or a scan for each piece of a long one, the last of them kept when [keep].
  private async request(body: object, keep: boolean): Promise<LeScan> {
    const bytes = new TextEncoder().encode(JSON.stringify({ v: 1, origin: location.origin, session: this.session, ...body }));
    const id = this.nextId++;
    let scan: LeScan | undefined;
    for (let at = 0; at < bytes.length || at === 0; at += CHUNK) {
      const last = at + CHUNK >= bytes.length;
      scan?.stop();
      scan = await this.bt.requestLEScan({
        filters: [{ manufacturerData: [{ companyIdentifier: COMPANY, dataPrefix: frame(last ? 0 : MORE, id, bytes.subarray(at, at + CHUNK)) }] }],
        keepRepeatedDevices: keep,
      });
      if (last) break;
    }
    return scan!;
  }

  private onAdvertisement(e: { manufacturerData?: Map<number, DataView> }): void {
    const view = e.manufacturerData?.get(COMPANY);
    if (view === undefined || view.byteLength < 9) return;
    const bytes = new Uint8Array(view.buffer, view.byteOffset, view.byteLength);
    if (!MAGIC.every((b, i) => bytes[i] === b)) return;
    const flags = bytes[4];
    const id = view.getUint32(5);
    const got = this.pieces.get(id) ?? [];
    got.push(bytes.slice(9));
    if (flags & MORE) {
      this.pieces.set(id, got);
      return;
    }
    this.pieces.delete(id);
    const whole = new Uint8Array(got.reduce((n, p) => n + p.length, 0));
    let at = 0;
    for (const p of got) {
      whole.set(p, at);
      at += p.length;
    }
    this.receive(new TextDecoder().decode(whole));
  }
}
