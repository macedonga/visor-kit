"""Layouts and Briefing cards for the Display, as the Kotlin builder makes them: plain dicts, ready for a call.

    from visor_kit.layout import card, header, stat, change, field, progress, chart, page, icon

    await visor.call("display.cards.set", card(
        "pasta", "Kitchen timer",
        header("Kitchen timer", icon="clock"),
        stat("4:00", "Pasta"),
        progress(0.6),
        icon="clock",
        pages=[
            page(header("Started"), field("At", "19:42")),
            page(header("Boiling"), chart([20, 60, 95, 100, 100])),
        ],
    ))

Fields take the wire's names or their snake_case form: ``max_lines=1`` is ``maxLines``, ``pin_top=0`` is ``pinTop``.
The elements, their fields and the colours are in the VDK's docs, page display.md.
"""

import base64
import mimetypes

__all__ = [
    "BLUE", "GREEN", "PRIMARY", "RED", "SECONDARY", "TRACK",
    "button", "buttons", "card", "change", "chart", "column", "field", "header", "icon", "image", "image_file",
    "layout", "page", "pager", "progress", "rgb", "row", "stat", "text",
]

PRIMARY = "primary"
SECONDARY = "secondary"
GREEN = "#009914"
RED = "#E5484D"
BLUE = "#4DA3FF"
TRACK = "#2A2D33"

# A card and its detail take at most this much as JSON, images included: BriefingCard.MAX_BYTES.
MAX_CARD_BYTES = 262_144


def rgb(value):
    """A colour from ``0xRRGGBB``, as ``"#RRGGBB"``."""
    return "#%06X" % (value & 0xFFFFFF)


def _camel(name):
    head, *rest = name.split("_")
    return head + "".join(part.title() for part in rest)


def _node(kind, fields, children=None):
    node = {"type": kind}
    node.update({_camel(k): v for k, v in fields.items() if v is not None})
    if children:
        node["children"] = list(children)
    return node


def column(*children, **fields):
    """A box that lays its children out top to bottom: ``gap``, ``padding``, ``align``, ``background`` and the rest."""
    return _node("column", fields, children)


def row(*children, **fields):
    """A box that lays its children out side by side."""
    return _node("row", fields, children)


def text(words, **fields):
    """A text: ``style`` (heading, title, body, meta), ``color``, ``max_lines``, ``align``."""
    return _node("text", dict(fields, text=words))


def image(url, **fields):
    """An image from an ``https://`` URL or a ``data:`` URI; ``height``, ``corners``, ``size``."""
    return _node("image", dict(fields, url=url))


def image_file(path):
    """A ``data:`` URI of an image file, for ``image()``. Visor scales one larger than the Display's 600 px."""
    mime = mimetypes.guess_type(path)[0] or "image/jpeg"
    with open(path, "rb") as f:
        return "data:%s;base64,%s" % (mime, base64.b64encode(f.read()).decode("ascii"))


def icon(name, size=None, **fields):
    """One of the Display's icons by name, such as ``"clock"`` or ``"bks_rainy"``; ``size`` in pixels, ``color``."""
    return _node("icon", dict(fields, icon=name, height=size))


def button(id, label, **fields):
    """A button whose clicks come with ``id``: ``style``, ``icon``, ``primary``."""
    return _node("button", dict(fields, id=id, text=label))


def buttons(*items, **fields):
    """The Display's own bar of buttons."""
    return _node("button_group", fields, items)


def chart(values, **fields):
    """``values`` as a line (the default) or ``style="bars"``; ``color``, ``height``. Visor draws it on the phone."""
    return _node("chart", dict(fields, values=[float(v) for v in values]))


def page(*children, **fields):
    """One page of a pager: a column."""
    return column(*children, **fields)


def pager(*pages, **fields):
    """Pages the wearer swipes through, with dots under them."""
    return _node("pager", fields, pages)


def header(title, icon=None):
    """A card's first line, as Meta's: an icon beside a bold title."""
    if icon is None:
        return text(title, style="title", max_lines=1)
    return row(
        _node("icon", {"icon": icon, "height": 24}),
        text(title, style="title", max_lines=1, shrink=1.0),
        gap=8, cross_align="center",
    )


def stat(value, caption=None):
    """A figure in large type, with a grey caption under it."""
    figure = text(value, style="heading", max_lines=1)
    if caption is None:
        return figure
    return column(figure, text(caption, style="meta", max_lines=1))


def change(words, rising):
    """A green triangle up and green text, or a red triangle down and red text."""
    tone = GREEN if rising else RED
    glyph = "small_triangle_up" if rising else "small_triangle_down"
    return row(
        _node("icon", {"icon": glyph, "height": 20, "color": tone}),
        text(words, color=tone, max_lines=1, shrink=1.0),
        gap=4, cross_align="center",
    )


def field(label, value):
    """A grey label and its value on one line, the value at the end."""
    return row(
        text(label, color=SECONDARY, max_lines=1, shrink=1.0),
        text(value, align="end", max_lines=1),
        align="space_between", cross_align="center", gap=16,
    )


def progress(fraction, color=BLUE):
    """A bar filled ``fraction`` of the way, from 0 to 1."""
    filled = min(1.0, max(0.0, float(fraction)))
    return row(
        row(grow=filled, background=color),
        row(grow=1.0 - filled),
        height=8, background=TRACK, cross_align="stretch",
    )


def layout(*children, **fields):
    """A layout for ``display.layout.show``: its root column."""
    return {"root": column(*children, **fields)}


def card(id, name, *children, icon="bell", detail=None, pages=None, **fields):
    """A card for ``display.cards.set``. ``detail`` is a list of elements a click opens; ``pages`` a list of
    ``page()``s the wearer swipes through instead."""
    built = {"id": id, "name": name, "icon": icon, "layout": layout(*children, **fields)}
    if pages is not None:
        built["detail"] = layout(pager(*pages))
    elif detail is not None:
        built["detail"] = layout(*detail)
    return built
