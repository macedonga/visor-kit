// Draws a VDK layout the way the Meta Ray-Ban Display draws it through Meta's toolkit app: the same boxes, sizes,
// colours and focus rules as Visor's DatLayout, which matches DAT's own builder. Used by the simulator page that
// `visor fake` serves and by kit-testing's LayoutPreview.
"use strict";

const VisorDisplay = (() => {
  const PRIMARY = "#FFFFFF";
  const SECONDARY = "#CFD3D9";
  const TEXT = {
    heading: { size: 40, line: 48, weight: 700 },
    body: { size: 28, line: 36, weight: 400 },
    meta: { size: 22, line: 28, weight: 400 },
  };
  const JUSTIFY = { center: "center", end: "flex-end", space_between: "space-between" };
  const ITEMS = { center: "center", end: "flex-end", stretch: "stretch" };
  const CORNERS = { small: "8px", medium: "16px" };
  const GAP_MARGIN = { column: "marginTop", "column-reverse": "marginBottom", row: "marginLeft", "row-reverse": "marginRight" };

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

  function glyph(name, outline) {
    const node = el("span", "vd-glyph");
    node.textContent = GLYPHS[name] || "help";
    node.title = name;
    node.style.fontVariationSettings = `"FILL" ${outline ? 0 : 1}`;
    return node;
  }

  // Draws [layout] into [host], a 600 by 600 element. [on] takes click(id), back() and video(state, error).
  function render(layout, host, on) {
    host.replaceChildren();
    const focusables = [];
    let focused = -1;
    let primary = null;

    function focusable(node, id) {
      node.classList.add("vd-focusable");
      node.dataset.id = id;
      node.addEventListener("click", () => { focus(focusables.indexOf(node)); on.click(id); });
      focusables.push(node);
    }

    function text(n) {
      const t = TEXT[n.style] || TEXT.body;
      const node = el("div", "vd-text");
      node.textContent = n.text || "";
      Object.assign(node.style, { fontSize: `${t.size}px`, lineHeight: `${t.line}px`, fontWeight: t.weight });
      const color = n.color || (n.style === "meta" ? "secondary" : "primary");
      node.style.color = color === "secondary" ? SECONDARY : PRIMARY;
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
        case "icon": return n.icon ? glyph(n.icon, n.style === "outline") : null;
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
      if (primary) focus(focusables.indexOf(primary));
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
      select() { const node = focusables[focused]; if (node) on.click(node.dataset.id); },
      back() { on.back(); },
    };
  }

  return { render, glyphs: GLYPHS };
})();
