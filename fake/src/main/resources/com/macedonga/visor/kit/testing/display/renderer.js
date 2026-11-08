// Draws a VDK layout the way the Meta Ray-Ban Display draws it through Meta's toolkit app: the same boxes, sizes,
// colours and focus rules as Visor's DatLayout, which matches DAT's own builder. Used by the simulator page that
// `visor fake` serves and by kit-testing's LayoutPreview.
"use strict";

const VisorDisplay = (() => {
  const PRIMARY = "#FFFFFF";
  const SECONDARY = "#CFD3D9";
  const TEXT = {
    heading: { size: 40, line: 48, weight: 700 },
    title: { size: 24, line: 32, weight: 700 },
    body: { size: 28, line: 36, weight: 400 },
    meta: { size: 22, line: 28, weight: 400 },
  };
  const JUSTIFY = { center: "center", end: "flex-end", space_between: "space-between" };
  const ITEMS = { center: "center", end: "flex-end", stretch: "stretch" };
  const CORNERS = { small: "8px", medium: "16px" };
  const GAP_MARGIN = { column: "marginTop", "column-reverse": "marginBottom", row: "marginLeft", "row-reverse": "marginRight" };
  const BLUE = "#4DA3FF";
  const PAGE_INSET = 24;
  // Meta's weather illustrations, in colour, from the CDN the Display falls back to.
  const ICON_CDN = "https://static.xx.fbcdn.net/assets/?set=wearables_icons&name=";

  // DAT's icon names, drawn with the nearest Material Symbol; the Display draws its own glyphs.
  const GLYPHS = {
    airplane: "flight", arrow_down_shallow_u: "download", arrow_left: "arrow_back", arrow_right: "arrow_forward",
    arrow_u_left: "undo", arrow_up_shallow_u: "upload", avatar: "account_circle", avatar_off: "no_accounts",
    bed_side: "bed", bell: "notifications", bell_diagonal_right_dot: "notifications_unread",
    bell_off: "notifications_off", bike_share: "pedal_bike", bug: "bug_report", bullhorn: "campaign",
    bus: "directions_bus", calendar: "calendar_today", campfire: "local_fire_department",
    caret_down: "keyboard_arrow_down", caret_left: "keyboard_arrow_left", caret_right: "keyboard_arrow_right",
    caret_up: "keyboard_arrow_up", car_front_view: "directions_car", cart: "shopping_cart", checkmark: "check",
    checkmark_circle: "check_circle", circle_8_rays_large: "light_mode", circle_handle: "search", clock: "schedule",
    cloud: "cloud", cloud_crescent_moon: "nights_stay", cloud_dot_four_rays: "partly_cloudy_day",
    cloud_five_dashes: "rainy", cloud_hook_swirl: "air", cloud_lightning: "thunderstorm",
    cocktail_glass: "local_bar", code: "code", coffee_cup: "coffee", compass_north_up_red: "explore",
    container_with_lid: "inventory_2", cross_briefcase: "medical_services", dropper: "colorize",
    envelope_open: "drafts", exclamation_circle: "error", exclamation_triangle: "warning", eye: "visibility",
    fork_knife: "restaurant", four_arcs_up_filled: "wifi", four_arcs_up_grayscale: "wifi",
    four_corner_frame: "crop_free", gear: "settings", globe_western_hemisphere: "public",
    graduation_cap: "school", hashtag: "tag", headphones: "headphones", heart: "favorite", house: "home",
    i_circle: "info", light_bulb: "lightbulb", magic_wand: "auto_fix_high", meta_ai: "auto_awesome",
    mountain_square: "image", mountain_square_stacked: "photo_library", museum_building: "museum",
    music_note: "music_note", nine_squares_grid: "apps", padlock_closed: "lock", padlock_open: "lock_open",
    palette: "palette", paper_airplane: "send", pencil: "edit", pencil_square: "edit_square", person: "person",
    person_circle: "account_circle", phone: "smartphone", phone_handset_arrow_down_left: "call_received",
    phone_handset_arrow_up_right: "call_made", phone_slash: "phone_disabled", pizza_slice: "local_pizza",
    plus: "add", plus_circle: "add_circle", shopping_bag: "shopping_bag", sliders_horizontal: "tune",
    smart_glasses: "eyeglasses", smiley_circle: "sentiment_satisfied", speaker_off: "volume_off",
    speaker_with_one_arc: "volume_mute", speaker_with_three_arcs: "volume_up", speaker_with_two_arcs: "volume_down",
    speech_bubble: "chat_bubble", speech_bubble_off: "comments_disabled", stadium: "stadium", star: "star",
    star_circle_triangle_ai: "auto_awesome", taxi: "local_taxi", three_dot_speech_bubble: "sms",
    three_dots_horizontal: "more_horiz", three_horizontal_lines: "menu",
    three_horizontal_lines_stacked_descending: "sort", three_people_overlapping: "groups", train: "train",
    tree: "park", triangle_left_vertical_line: "skip_previous", triangle_right: "play_arrow",
    triangle_right_circle: "play_circle", triangle_right_vertical_line: "skip_next", two_arrows_clockwise: "sync",
    two_lines_parallel: "pause", two_squares_stacked_right_down: "content_copy", two_triangles_left: "fast_rewind",
    two_triangles_right: "fast_forward", video_camera: "videocam", video_camera_off: "videocam_off",
    wristband: "watch", wristband_slash: "watch_off", x: "close",
  };

  function el(tag, className) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    return node;
  }

  // A kit colour: one of the Display's names or #RRGGBB; null for none or an unknown one.
  function colour(c) {
    if (c === "primary") return PRIMARY;
    if (c === "secondary") return SECONDARY;
    return /^#[0-9a-f]{6}$/i.test(c || "") ? c : null;
  }

  // A glyph [size] pixels square, in [color]; a weather illustration as Meta draws it, untinted.
  function glyph(name, outline, color, size) {
    const side = size || 32;
    if (name.startsWith("bks_")) {
      const img = el("img", "vd-illustration");
      img.src = `${ICON_CDN}${name.replace(/_/g, "-")}&density=1`;
      img.alt = name;
      img.title = name;
      Object.assign(img.style, { width: `${side}px`, height: `${side}px` });
      return img;
    }
    if (!GLYPHS[name]) {
      // A name DAT does not have: Meta's own glyph from its CDN, tinted through a mask, as the Display fetches it.
      const node = el("span", "vd-masked");
      const url = `${ICON_CDN}${name.replace(/_/g, "-")}-${outline ? "outline" : "filled"}-32-light&density=1`;
      Object.assign(node.style, {
        width: `${side}px`, height: `${side}px`, backgroundColor: color || PRIMARY,
        maskImage: `url("${url}")`, webkitMaskImage: `url("${url}")`,
      });
      node.title = name;
      return node;
    }
    const node = el("span", "vd-glyph");
    node.textContent = GLYPHS[name];
    node.title = name;
    Object.assign(node.style, {
      fontVariationSettings: `"FILL" ${outline ? 0 : 1}`, fontSize: `${side}px`, width: `${side}px`, height: `${side}px`,
    });
    if (color) node.style.color = color;
    return node;
  }

  // Draws [layout] into [host], a 600 by 600 element. [on] takes click(id), back() and video(state, error).
  function render(layout, host, on) {
    host.replaceChildren();
    const focusables = [];
    let focused = -1;
    let primary = null;
    let firstPage = null;

    function focusable(node, id) {
      node.classList.add("vd-focusable");
      node.dataset.id = id;
      // A page of a pager takes the focus and sends no click.
      node.addEventListener("click", () => { focus(focusables.indexOf(node)); if (id) on.click(id); });
      focusables.push(node);
    }

    function text(n) {
      const t = TEXT[n.style] || TEXT.body;
      const node = el("div", "vd-text");
      node.textContent = n.text || "";
      Object.assign(node.style, { fontSize: `${t.size}px`, lineHeight: `${t.line}px`, fontWeight: t.weight });
      node.style.color = colour(n.color) || (n.style === "meta" ? SECONDARY : PRIMARY);
      if (n.align === "center" || n.align === "end") node.style.textAlign = n.align;
      if (n.maxLines > 0) {
        node.classList.add("vd-clamp");
        node.style.webkitLineClamp = String(n.maxLines);
      }
      return node;
    }

    // As Visor draws it on the phone: a line from the lowest value to the highest with a wash under it, or bars
    // from zero.
    function chart(n) {
      const values = (n.values || []).filter((v) => Number.isFinite(v));
      if (!values.length) return null;
      const h = Math.max(16, Math.min(600, n.height || 120));
      const color = colour(n.color) || BLUE;
      const w = 1000;
      const ns = "http://www.w3.org/2000/svg";
      const svg = document.createElementNS(ns, "svg");
      svg.setAttribute("class", "vd-chart");
      svg.setAttribute("viewBox", `0 0 ${w} ${h}`);
      svg.setAttribute("preserveAspectRatio", "none");
      svg.style.height = `${h}px`;
      const add = (parent, tag, attrs) => {
        const node = document.createElementNS(ns, tag);
        Object.entries(attrs).forEach(([k, v]) => node.setAttribute(k, v));
        parent.append(node);
        return node;
      };
      if (n.style === "bars") {
        const base = Math.min(0, ...values);
        const span = Math.max(0, ...values) - base || 1;
        const slot = w / values.length;
        values.forEach((v, i) => {
          const top = Math.min(h - 1, Math.max(0, h - ((v - base) / span) * h));
          add(svg, "rect", { x: i * slot + slot * 0.2, y: top, width: slot * 0.6, height: h - top, rx: 6, fill: color });
        });
        return svg;
      }
      const lo = Math.min(...values);
      const span = Math.max(...values) - lo;
      const x = (i) => (values.length === 1 ? w / 2 : 4 + (i * (w - 8)) / (values.length - 1));
      const y = (v) => (span ? h - 4 - ((v - lo) / span) * (h - 8) : h / 2);
      const points = values.map((v, i) => `${x(i)},${y(v)}`).join(" ");
      const id = `vd-wash-${Math.random().toString(36).slice(2)}`;
      const gradient = add(add(svg, "defs", {}), "linearGradient", { id, x1: 0, y1: 0, x2: 0, y2: 1 });
      add(gradient, "stop", { offset: 0, "stop-color": color, "stop-opacity": 0.33 });
      add(gradient, "stop", { offset: 1, "stop-color": color, "stop-opacity": 0 });
      add(svg, "polygon", { points: `${x(0)},${h} ${points} ${x(values.length - 1)},${h}`, fill: `url(#${id})` });
      add(svg, "polyline", {
        points, fill: "none", stroke: color, "stroke-width": 4, "stroke-linecap": "round", "stroke-linejoin": "round",
        "vector-effect": "non-scaling-stroke",
      });
      return svg;
    }

    // Meta's Weather and Stocks details: pages side by side, each a card inset 24 px, dots under them. The arrows
    // move the focus from page to page, as the wearer's swipes do.
    function pager(n) {
      const node = el("div", "vd-pager");
      const strip = el("div", "vd-pages");
      const dots = el("div", "vd-dots");
      const pages = (n.children || []).filter((c) => c.type === "column" || c.type === "row");
      pages.forEach((page, i) => {
        const slot = el("div", "vd-page");
        slot.style.padding = `0 ${PAGE_INSET}px`;
        const own = page.padding != null || page.paddingTop != null || page.paddingBottom != null || page.paddingStart != null || page.paddingEnd != null;
        const card = box(own ? page : { ...page, padding: 24 });
        card.classList.add("vd-card", "vd-page-card");
        focusable(card, "");
        if (!firstPage) firstPage = card;
        slot.append(card);
        strip.append(slot);
        dots.append(el("span", i === 0 ? "vd-dot vd-dot-on" : "vd-dot"));
      });
      strip.addEventListener("scroll", () => {
        const at = Math.round(strip.scrollLeft / strip.clientWidth);
        [...dots.children].forEach((dot, i) => dot.classList.toggle("vd-dot-on", i === at));
      });
      node.append(strip, dots);
      return node;
    }

    function image(n) {
      const icon = n.size === "icon";
      const img = el("img", "vd-image");
      img.src = n.url || "";
      img.alt = "";
      if (icon) Object.assign(img.style, { width: "32px", height: "32px", objectFit: "cover" });
      else Object.assign(img.style, { width: "100%", height: n.height ? `${n.height}px` : "auto", objectFit: "contain" });
      const corners = n.corners || (n.card ? "medium" : "none");
      if (CORNERS[corners]) {
        const clip = el("div", "vd-clip");
        clip.style.borderRadius = CORNERS[corners];
        clip.append(img);
        return clip;
      }
      return img;
    }

    function button(n, grouped) {
      const node = el("div", "vd-button");
      if (n.icon) node.append(glyph(n.icon, n.style === "outline"));
      if (n.text) {
        const label = el("span", "vd-label");
        label.textContent = n.text;
        node.append(label);
      }
      if (grouped) node.classList.add("vd-grouped");
      if (n.id) {
        focusable(node, n.id);
        if (n.primary && !primary) primary = node;
      }
      return node;
    }

    function buttons(n) {
      const bar = el("div", "vd-bar");
      bar.style.justifyContent = JUSTIFY[n.align || "center"] || "flex-start";
      (n.children || []).filter((c) => c.type === "button").forEach((c) => bar.append(button(c, true)));
      return bar;
    }

    function box(n) {
      const direction = (n.type === "row" ? "row" : "column") + (n.reverse ? "-reverse" : "");
      const row = direction.startsWith("row");
      const node = el("div", "vd-box");
      const pad = n.padding || 0;
      const inset = { top: n.paddingTop ?? pad, bottom: n.paddingBottom ?? pad, start: n.paddingStart ?? pad, end: n.paddingEnd ?? pad };
      if (colour(n.background)) node.style.background = colour(n.background);
      if (n.height > 0) node.style.height = `${n.height}px`;
      Object.assign(node.style, {
        flexDirection: direction,
        justifyContent: JUSTIFY[n.align] || "flex-start",
        // A column always stretches its children across, as on the Display.
        alignItems: row ? ITEMS[n.crossAlign] || "flex-start" : "stretch",
        flexWrap: n.wrap ? "wrap" : "nowrap",
        paddingTop: `${n.paddingTop ?? pad}px`, paddingBottom: `${n.paddingBottom ?? pad}px`,
        paddingLeft: `${n.paddingStart ?? pad}px`, paddingRight: `${n.paddingEnd ?? pad}px`,
      });
      let i = 0;
      for (const child of n.children || []) {
        const drawn = element(child);
        if (!drawn) continue;
        const grow = child.grow || 0;
        drawn.style.flexGrow = grow;
        drawn.style.flexShrink = child.shrink || 0;
        if (grow && row) drawn.style.flexBasis = "0";
        if (child.alignSelf) drawn.style.alignSelf = ITEMS[child.alignSelf] || "flex-start";
        const pins = { top: child.pinTop, bottom: child.pinBottom, left: child.pinStart, right: child.pinEnd };
        if (Object.values(pins).some((v) => v != null)) {
          // Pinned from inside the padding, over the other children; it takes no space and no gap.
          const edge = { top: inset.top, bottom: inset.bottom, left: inset.start, right: inset.end };
          drawn.style.position = "absolute";
          Object.entries(pins).forEach(([side, v]) => { if (v != null) drawn.style[side] = `${v + edge[side]}px`; });
          node.append(drawn);
          continue;
        }
        if (i > 0 && n.gap) drawn.style[GAP_MARGIN[direction]] = `${n.gap}px`;
        node.append(drawn);
        i++;
      }
      if (n.card) node.classList.add("vd-card");
      if (n.id) focusable(node, n.id);
      return node;
    }

    function video(n) {
      const v = el("video", "vd-video");
      v.src = n.url || "";
      v.autoplay = true;
      v.muted = true;
      v.playsInline = true;
      v.controls = !!n.controls;
      let paused = false;
      v.addEventListener("pause", () => { if (!v.ended) { paused = true; on.video("paused"); } });
      v.addEventListener("play", () => { if (paused) { paused = false; on.video("playing"); } });
      v.addEventListener("ended", () => on.video("ended"));
      v.addEventListener("error", () => on.video("failed", (n.url || "").startsWith("https://") ? "playback_failed" : "invalid_url"));
      return v;
    }

    function element(n) {
      switch (n.type) {
        case "column": case "row": return box(n);
        case "text": return text(n);
        case "image": return image(n);
        case "icon": return n.icon ? glyph(n.icon, n.style === "outline", colour(n.color), n.height) : null;
        case "pager": return pager(n);
        case "chart": return chart(n);
        case "button": return button(n, false);
        case "button_group": return buttons(n);
        case "video": return video(n);
        default: return null; // A type this renderer does not know is left out, as Visor does.
      }
    }

    function focus(index) {
      if (focused >= 0) focusables[focused]?.classList.remove("vd-focused");
      focused = index;
      const node = focusables[focused];
      if (!node) return;
      node.classList.add("vd-focused");
      node.scrollIntoView({ block: "nearest", inline: "nearest" });
    }

    if (layout) {
      const root = element(layout.root);
      root.classList.add("vd-root");
      host.append(root);
      // A bar shows its buttons' labels only while they fit, as the Display's does; icons stay.
      host.querySelectorAll(".vd-bar").forEach((bar) => {
        if (bar.scrollWidth > bar.clientWidth) bar.classList.add("vd-tight");
      });
      // As Visor's pager focuses its first page when it binds.
      if (primary) focus(focusables.indexOf(primary));
      else if (firstPage) focus(focusables.indexOf(firstPage));
    }

    // The nearest focusable element in [direction], judged from the centres; off-axis distance weighs double.
    function move(direction) {
      if (!focusables.length) return;
      if (focused < 0) { focus(0); return; }
      const from = focusables[focused].getBoundingClientRect();
      const cx = from.left + from.width / 2;
      const cy = from.top + from.height / 2;
      let best = -1;
      let bestScore = Infinity;
      focusables.forEach((node, i) => {
        if (i === focused) return;
        const r = node.getBoundingClientRect();
        const dx = r.left + r.width / 2 - cx;
        const dy = r.top + r.height / 2 - cy;
        const along = { up: -dy, down: dy, left: -dx, right: dx }[direction];
        const across = direction === "up" || direction === "down" ? Math.abs(dx) : Math.abs(dy);
        if (along <= 0) return;
        const score = along + 2 * across;
        if (score < bestScore) { bestScore = score; best = i; }
      });
      if (best >= 0) focus(best);
    }

    return {
      move,
      select() { const node = focusables[focused]; if (node && node.dataset.id) on.click(node.dataset.id); },
      back() { on.back(); },
    };
  }

  const NOTHING = { click() {}, back() {}, video() {} };

  function padded(root) {
    const own = root.padding != null || root.paddingTop != null || root.paddingBottom != null || root.paddingStart != null || root.paddingEnd != null;
    return own ? root : { ...root, padding: 24 };
  }

  function find(node, id) {
    if (!node) return null;
    if (node.id === id) return node;
    for (const child of node.children || []) {
      const hit = find(child, id);
      if (hit) return hit;
    }
    return null;
  }

  // The Display home's Briefing with the app's cards, as Visor serves them: each card in the panel's material, padded
  // 24 px unless it pads itself. Select opens a card's detail full screen, padded as a card unless it is pages alone;
  // back closes it. [on] takes cardClick(card, id), and back() when the wearer leaves the Briefing.
  function briefing(cards, host, on) {
    let focused = 0;
    let open = null;
    let detail = null;

    function list() {
      open = null;
      detail = null;
      host.replaceChildren();
      const strip = el("div", "vd-briefing");
      if (!cards.length) {
        const empty = el("div", "vd-text vd-empty");
        empty.textContent = "The app has no cards on the Briefing.";
        strip.append(empty);
      }
      cards.forEach((card, i) => {
        const frame = el("div", "vd-briefing-card");
        render({ root: padded(card.layout.root) }, frame, NOTHING);
        frame.title = card.detail ? `${card.name}: select to open` : card.name;
        frame.addEventListener("click", () => { focused = i; openCard(); });
        strip.append(frame);
      });
      host.append(strip);
      focused = Math.min(focused, Math.max(0, cards.length - 1));
      mark();
    }

    function mark() {
      const frames = host.querySelectorAll(".vd-briefing-card");
      frames.forEach((frame, i) => frame.classList.toggle("vd-focused", i === focused));
      frames[focused]?.scrollIntoView({ block: "nearest" });
    }

    function toast(text) {
      const note = el("div", "vd-toast");
      note.textContent = text;
      host.append(note);
      setTimeout(() => note.remove(), 2000);
    }

    function openCard() {
      const card = cards[focused];
      if (!card?.detail) return;
      open = card;
      const root = card.detail.root;
      const pages = (root.children || []).length === 1 && root.children[0].type === "pager";
      detail = render({ root: pages ? root : padded(root) }, host, {
        click(id) {
          on.cardClick(card.id, id);
          const element = find(card.detail.root, id);
          if (element?.toast) toast(element.toast);
        },
        back() { list(); },
        video() {},
      });
    }

    list();
    return {
      move(direction) {
        if (detail) { detail.move(direction); return; }
        if (direction === "up" && focused > 0) focused--;
        else if (direction === "down" && focused < cards.length - 1) focused++;
        mark();
      },
      select() { if (detail) detail.select(); else openCard(); },
      back() { if (detail) list(); else on.back(); },
      // The cards changed: the list shows them at once; an open detail stays until the wearer leaves it.
      update(next) {
        cards = next;
        if (!detail) list();
        else if (!cards.some((c) => c.id === open.id)) list();
      },
    };
  }

  return { render, briefing, glyphs: GLYPHS };
})();
