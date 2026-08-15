#!/usr/bin/env python3
"""Erzeugt aus der Diagramm-Markdown eine Word-taugliche Arbeitskopie:
- kompakter, einseitiger Titelblock (Pandoc-Titelblock statt separater Deckseite),
- deutscher Inhaltsverzeichnis-Titel,
- Mermaid-Bloecke werden durch eingebettete PNG-Bilder ersetzt; breite Diagramme
  kommen auf eine Querformat-Seite (LibreOffice/Word rendern sie dadurch gross und
  lesbar), schmale/hohe Diagramme bleiben im Hochformat,
- Zeichen, die im LibreOffice-Export ausfallen (Emoji, seltene Pfeile), werden durch
  gut unterstuetzte Textentsprechungen ersetzt,
- Pandoc-Fenced-Divs (::: ...) werden entfernt.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

# US-Letter (Pandoc-Standard-Reference) in Twips.
PORTRAIT_SECTPR = (
    '<w:p><w:pPr><w:sectPr>'
    '<w:pgSz w:w="12240" w:h="15840"/>'
    '<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"'
    ' w:header="720" w:footer="720" w:gutter="0"/>'
    '</w:sectPr></w:pPr></w:p>'
)
LANDSCAPE_SECTPR = (
    '<w:p><w:pPr><w:sectPr>'
    '<w:pgSz w:w="15840" w:h="12240" w:orient="landscape"/>'
    '<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"'
    ' w:header="720" w:footer="720" w:gutter="0"/>'
    '</w:sectPr></w:pPr></w:p>'
)

# Ab diesem Seitenverhaeltnis (Breite/Hoehe) wird ein Diagramm quer gelegt.
LANDSCAPE_RATIO = 1.3

# Zeichen, die LibreOffice beim PDF/DOCX-Export nicht zuverlaessig darstellt.
GLYPH_REPLACEMENTS = {
    "\u2705": "ja",      # weisses Haekchen im gruenen Kasten
    "\u274c": "nein",    # rotes Kreuz
    "\u26a0\ufe0f": "teilw.",
    "\u26a0": "teilw.",
    "\u2194": "und",     # Links-Rechts-Pfeil (UI <-> API)
}


def split_front_matter(text: str) -> tuple[str, str]:
    match = re.match(r"^---\n(.*?)\n---\n(.*)$", text, flags=re.DOTALL)
    if not match:
        return "", text
    return match.group(1), match.group(2)


def yaml_value(front: str, key: str) -> str:
    match = re.search(rf'^{re.escape(key)}:\s*"?(.*?)"?\s*$', front, flags=re.MULTILINE)
    return match.group(1) if match else ""


def build_front_matter(front: str) -> str:
    title = yaml_value(front, "title") or "Architekturdiagramme der Vereinsverwaltung FH_MA"
    subtitle = yaml_value(front, "subtitle")
    lines = [
        "---",
        f'title: "{title}"',
    ]
    if subtitle:
        lines.append(f'subtitle: "{subtitle}"')
    lines += [
        "author:",
        '  - "Nils Firschau (8993076)"',
        '  - "Paul Faller (5567855)"',
        '  - "Robin Steiner (9251426)"',
        '  - "Ole Schildt (3504736)"',
        'date: "Modul CSC1200 „Advanced Software Engineering“ · Prof. Dr. Holger D. Hofmann · 15. August 2026"',
        "lang: de",
        'toc-title: "Inhaltsverzeichnis"',
        "---",
        "",
    ]
    return "\n".join(lines)


def load_manifest(manifest_path):
    ratios = {}
    if not manifest_path:
        return ratios
    p = Path(manifest_path)
    if not p.exists():
        return ratios
    for entry in json.loads(p.read_text(encoding="utf-8")):
        h = entry.get("height") or 1
        ratios[int(entry["n"])] = float(entry["width"]) / float(h)
    return ratios


def transform_body(body: str, imgdir: str, ratios) -> str:
    for src, dst in GLYPH_REPLACEMENTS.items():
        body = body.replace(src, dst)

    # Pandoc-Fenced-Div-Marker entfernen (z. B. ::: {.landscape}) .
    body = re.sub(r"^:::.*$", "", body, flags=re.MULTILINE)

    counter = {"n": 0, "land": 0}

    def replace(_match: re.Match) -> str:
        counter["n"] += 1
        n = counter["n"]
        ratio = ratios.get(n, 0.0)
        if ratio >= LANDSCAPE_RATIO:
            counter["land"] += 1
            img = f"![]({imgdir}/diagram-{n}.png){{ width=22cm }}"
            return (
                f"```{{=openxml}}\n{PORTRAIT_SECTPR}\n```\n\n"
                f"{img}\n\n"
                f"```{{=openxml}}\n{LANDSCAPE_SECTPR}\n```"
            )
        return f"![]({imgdir}/diagram-{n}.png){{ width=16cm }}"

    body = re.sub(r"```mermaid[ \t]*\r?\n[\s\S]*?\r?\n```", replace, body)
    # Mehrfache Leerzeilen zusammenfassen.
    body = re.sub(r"\n{3,}", "\n\n", body)
    print(f"Diagramme ersetzt: {counter['n']} (davon Querformat: {counter['land']})")
    return body


def main() -> int:
    if len(sys.argv) < 3:
        print("Usage: prepare_docx_md.py <input.md> <output.md> [imgdir] [manifest.json]", file=sys.stderr)
        return 2
    source = Path(sys.argv[1]).read_text(encoding="utf-8")
    output = Path(sys.argv[2])
    imgdir = sys.argv[3] if len(sys.argv) > 3 else "img"
    manifest = sys.argv[4] if len(sys.argv) > 4 else None

    ratios = load_manifest(manifest)
    front, body = split_front_matter(source)
    new_text = build_front_matter(front) + transform_body(body, imgdir, ratios).lstrip("\n")
    output.write_text(new_text, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
