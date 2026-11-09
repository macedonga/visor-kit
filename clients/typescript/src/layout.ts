/**
 * Layouts and Briefing cards for the Display, as the Kotlin builder makes them: plain objects, ready for a call.
 *
 *     import { card, header, stat, progress, page, chart } from "visor-kit/layout";
 *
 *     await visor.call("display.cards.set", card("pasta", "Kitchen timer", [
 *       header("Kitchen timer", "clock"),
 *       stat("4:00", "Pasta"),
 *       progress(0.6),
 *     ], { icon: "clock", pages: [page([chart([20, 60, 95, 100])])] }));
 *
 * The elements, their fields and the colours are in the VDK's docs, page display.md.
 */

export const PRIMARY = "primary";
export const SECONDARY = "secondary";
export const GREEN = "#009914";
export const RED = "#E5484D";
export const BLUE = "#4DA3FF";
export const TRACK = "#2A2D33";

/** A card and its detail take at most this much as JSON, images included: BriefingCard.MAX_BYTES. */
export const MAX_CARD_BYTES = 262_144;

/** The fields any element may carry. */
export interface Placement {
  grow?: number;
  shrink?: number;
  alignSelf?: "start" | "center" | "end" | "stretch";
  pinTop?: number;
  pinBottom?: number;
  pinStart?: number;
  pinEnd?: number;
}

export interface BoxFields extends Placement {
  id?: string;
  /** With an `id`, in a card's detail: a word the Display shows at once when the wearer clicks the box. */
  toast?: string;
  gap?: number;
  padding?: number;
  paddingTop?: number;
  paddingBottom?: number;
  paddingStart?: number;
  paddingEnd?: number;
  align?: "start" | "center" | "end" | "space_between";
  crossAlign?: "start" | "center" | "end" | "stretch";
  reverse?: boolean;
  wrap?: boolean;
  card?: boolean;
  background?: string;
  height?: number;
}

export interface TextFields extends Placement {
  style?: "heading" | "title" | "body" | "meta";
  color?: string;
  maxLines?: number;
  align?: "start" | "center" | "end";
}

export interface ImageFields extends Placement {
  height?: number;
  size?: "fill" | "icon";
  corners?: "none" | "small" | "medium";
}

export interface IconFields extends Placement {
  style?: "filled" | "outline";
  color?: string;
  /** In pixels, 32 unless set. */
  size?: number;
}

export interface ChartFields extends Placement {
  style?: "line" | "bars";
  color?: string;
  height?: number;
}

/** One element of a layout, as on the wire. */
export interface LayoutNode {
  type: string;
  children?: LayoutNode[];
  [field: string]: unknown;
}

export interface Layout {
  root: LayoutNode;
}

export interface BriefingCard {
  id: string;
  name: string;
  icon: string;
  layout: Layout;
  detail?: Layout;
}

/** A colour from `0xRRGGBB`, as `"#RRGGBB"`. */
export function rgb(value: number): string {
  return "#" + (value & 0xffffff).toString(16).padStart(6, "0").toUpperCase();
}

function node(type: string, fields: object, children?: LayoutNode[]): LayoutNode {
  const built: LayoutNode = { type };
  for (const [key, value] of Object.entries(fields)) if (value !== undefined) built[key] = value;
  if (children && children.length) built.children = children;
  return built;
}

export function column(children: LayoutNode[], fields: BoxFields = {}): LayoutNode {
  return node("column", fields, children);
}

export function row(children: LayoutNode[], fields: BoxFields = {}): LayoutNode {
  return node("row", fields, children);
}

export function text(words: string, fields: TextFields = {}): LayoutNode {
  return node("text", { ...fields, text: words });
}

/** An `https://` URL or a `data:` URI. Visor scales a `data:` image larger than the Display's 600 px. */
export function image(url: string, fields: ImageFields = {}): LayoutNode {
  return node("image", { ...fields, url });
}

/** One of the Display's icons by name, such as `"clock"` or `"bks_rainy"`. */
export function icon(name: string, fields: IconFields = {}): LayoutNode {
  const { size, ...rest } = fields;
  return node("icon", { ...rest, icon: name, height: size });
}

export interface ButtonFields extends Placement {
  style?: "primary" | "secondary" | "outline";
  icon?: string;
  primary?: boolean;
  /** In a card's detail: a word the Display shows at once when the wearer clicks the button. */
  toast?: string;
}

export interface VideoFields extends Placement {
  /** True to show play and pause. */
  controls?: boolean;
  height?: number;
}

/** A video, MP4 over `https://`, at most 400 pixels a side. It fills the screen alone: a layout with a video holds nothing else. */
export function video(url: string, fields: VideoFields = {}): LayoutNode {
  return { type: "video", url, ...fields };
}

export function button(id: string, label: string, fields: ButtonFields = {}): LayoutNode {
  return node("button", { ...fields, id, text: label });
}

export function buttons(items: LayoutNode[], fields: { align?: "start" | "center" | "end" } = {}): LayoutNode {
  return node("button_group", fields, items);
}

/** Values as a line, or bars with `style: "bars"`. Visor draws it on the phone. */
export function chart(values: number[], fields: ChartFields = {}): LayoutNode {
  return node("chart", { ...fields, values });
}

/** One page of a pager: a column. */
export function page(children: LayoutNode[], fields: BoxFields = {}): LayoutNode {
  return column(children, fields);
}

/** Pages the wearer swipes through, with dots under them. */
export function pager(pages: LayoutNode[]): LayoutNode {
  return node("pager", {}, pages);
}

/** A card's first line, as Meta's: an icon beside a bold title. */
export function header(title: string, iconName?: string): LayoutNode {
  if (iconName === undefined) return text(title, { style: "title", maxLines: 1 });
  return row([icon(iconName, { size: 24 }), text(title, { style: "title", maxLines: 1, shrink: 1 })], { gap: 8, crossAlign: "center" });
}

/** A figure in large type, with a grey caption under it. */
export function stat(value: string, caption?: string): LayoutNode {
  const figure = text(value, { style: "heading", maxLines: 1 });
  return caption === undefined ? figure : column([figure, text(caption, { style: "meta", maxLines: 1 })]);
}

/** A green triangle up and green text, or a red triangle down and red text. */
export function change(words: string, rising: boolean): LayoutNode {
  const tone = rising ? GREEN : RED;
  return row(
    [icon(rising ? "small_triangle_up" : "small_triangle_down", { size: 20, color: tone }), text(words, { color: tone, maxLines: 1, shrink: 1 })],
    { gap: 4, crossAlign: "center" },
  );
}

/** A grey label and its value on one line, the value at the end. */
export function field(label: string, value: string): LayoutNode {
  return row(
    [text(label, { color: SECONDARY, maxLines: 1, shrink: 1 }), text(value, { align: "end", maxLines: 1 })],
    { align: "space_between", crossAlign: "center", gap: 16 },
  );
}

/** A bar filled `fraction` of the way, from 0 to 1. */
export function progress(fraction: number, color: string = BLUE): LayoutNode {
  const filled = Number.isNaN(fraction) ? 0 : Math.min(1, Math.max(0, fraction));
  return row([row([], { grow: filled, background: color }), row([], { grow: 1 - filled })], {
    height: 8,
    background: TRACK,
    crossAlign: "stretch",
  });
}

/** A layout for `display.layout.show`. */
export function layout(children: LayoutNode[], fields: BoxFields = {}): Layout {
  return { root: column(children, fields) };
}

/** A card for `display.cards.set`: `detail` is what a click opens, `pages` the pages the wearer swipes through instead. */
export function card(
  id: string,
  name: string,
  children: LayoutNode[],
  options: { icon?: string; detail?: LayoutNode[]; pages?: LayoutNode[]; fields?: BoxFields } = {},
): BriefingCard {
  const built: BriefingCard = { id, name, icon: options.icon ?? "bell", layout: layout(children, options.fields) };
  if (options.pages) built.detail = layout([pager(options.pages)]);
  else if (options.detail) built.detail = layout(options.detail);
  return built;
}
