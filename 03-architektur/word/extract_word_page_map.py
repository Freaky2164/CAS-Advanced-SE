#!/usr/bin/env python3
"""Ermittelt die Word-Seitenzahlen fuer die statischen Verzeichnisse."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

from docx import Document
from pypdf import PdfReader


def normalize(value: str) -> str:
    value = value.replace("\u00ad", "").replace("\xa0", " ")
    return re.sub(r"\s+", " ", value).strip()


def roman(number: int) -> str:
    values = (
        (1000, "M"), (900, "CM"), (500, "D"), (400, "CD"),
        (100, "C"), (90, "XC"), (50, "L"), (40, "XL"),
        (10, "X"), (9, "IX"), (5, "V"), (4, "IV"), (1, "I"),
    )
    result = []
    for value, symbol in values:
        while number >= value:
            result.append(symbol)
            number -= value
    return "".join(result)


def find_page(texts: list[str], target: str, start: int) -> int:
    needle = normalize(target).lower()
    for index in range(start, len(texts)):
        if needle in texts[index].lower():
            return index
    short = " ".join(needle.split()[:8])
    for index in range(start, len(texts)):
        if short and short in texts[index].lower():
            return index
    raise RuntimeError(f"Verzeichnisziel nicht im Word-PDF gefunden: {target}")


def group_of(target: str) -> str:
    if target.startswith("Abbildung "):
        return "fig"
    if target.startswith("Tabelle "):
        return "tab"
    if target.startswith("Listing "):
        return "lst"
    return "toc"


def main() -> int:
    if len(sys.argv) != 4:
        print(
            "Usage: extract_word_page_map.py <input.docx> <input.pdf> <output.json>",
            file=sys.stderr,
        )
        return 2

    docx_path, pdf_path, output_path = map(Path, sys.argv[1:])
    document = Document(docx_path)
    targets = [
        re.sub(r"\s+\?\?$", "", paragraph.text).strip()
        for paragraph in document.paragraphs
        if paragraph.style.name.startswith("DirectoryEntry")
    ]

    reader = PdfReader(str(pdf_path))
    texts = [normalize(page.extract_text() or "") for page in reader.pages]
    body_start = next(
        index
        for index, text in enumerate(texts)
        if "Diese Arbeit dokumentiert die Modernisierung" in text
    )
    chapter_one_start = next(
        index
        for index, text in enumerate(texts)
        if index >= body_start and "1. Einleitung" in text
    )

    # Verzeichniseintraege stehen je Gruppe (Inhalt/Abbildungen/Tabellen/Listings)
    # in Dokumentreihenfolge. Die Suche laeuft daher monoton ab der letzten
    # Fundstelle derselben Gruppe; bei Gruppenwechsel wird zurueckgesetzt. So
    # werden Mehrfachvorkommen kurzer Begriffe (z. B. "Integrationstests") korrekt
    # der Ueberschrift statt einer frueheren Textstelle zugeordnet.
    page_map: dict[str, str] = {}
    previous_group: str | None = None
    run_start = body_start
    for target in targets:
        group = group_of(target)
        if group != previous_group:
            run_start = body_start
        try:
            page_index = find_page(texts, target, run_start)
        except RuntimeError:
            page_index = find_page(texts, target, body_start)
        page_map[target] = (
            roman(page_index)
            if page_index < chapter_one_start
            else str(page_index - chapter_one_start + 1)
        )
        run_start = page_index
        previous_group = group

    output_path.write_text(
        json.dumps(page_map, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"{len(page_map)} Word-Seitenverweise ermittelt")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
