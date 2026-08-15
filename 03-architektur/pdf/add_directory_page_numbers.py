#!/usr/bin/env python3
"""Ergaenzt Verzeichniseintraege in einem gerenderten HTML um PDF-Seitenzahlen."""

from __future__ import annotations

import html
import re
import sys
from pathlib import Path

from pypdf import PdfReader


LINK_PATTERN = re.compile(r'<a href="#([^"]+)"[^>]*>(.*?)</a>', re.DOTALL)
TAG_PATTERN = re.compile(r"<[^>]+>")


def normalize(value: str) -> str:
    value = html.unescape(TAG_PATTERN.sub(" ", value))
    value = value.replace(":", " ")
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


def find_page(texts: list[str], needle: str, start: int) -> int | None:
    normalized_needle = normalize(needle).lower().rstrip(".")
    if not normalized_needle:
        return None
    for index in range(start, len(texts)):
        page_text = normalize(texts[index]).lower()
        if normalized_needle in page_text:
            return index
    # Lange Beschriftungen koennen beim PDF-Text-Export unguenstig getrennt sein.
    short = " ".join(normalized_needle.split()[:7])
    for index in range(start, len(texts)):
        if short and short in normalize(texts[index]).lower():
            return index
    return None


def logical_page(index: int, chapter_one_start: int) -> str:
    if index < chapter_one_start:
        return roman(index)
    return str(index - chapter_one_start + 1)


def number_links(
    fragment: str,
    texts: list[str],
    destinations: dict[str, int],
    body_start: int,
    chapter_one_start: int,
) -> str:
    def replace(match: re.Match[str]) -> str:
        anchor, content = match.groups()
        if "toc-page-number" in content:
            return match.group(0)

        target_page = destinations.get("/" + anchor)
        if target_page is None:
            entry = normalize(content)
            target_page = find_page(texts, entry, body_start)
        if target_page is None:
            return match.group(0)
        page = logical_page(target_page, chapter_one_start)
        return (
            f'<a href="#{anchor}">{content}'
            f'<span class="toc-page-number">{page}</span></a>'
        )

    return LINK_PATTERN.sub(replace, fragment)


def main() -> int:
    if len(sys.argv) != 4:
        print(
            "Usage: add_directory_page_numbers.py <input.html> <input.pdf> <output.html>",
            file=sys.stderr,
        )
        return 2

    html_path, pdf_path, output_path = map(Path, sys.argv[1:])
    document = html_path.read_text(encoding="utf-8")
    reader = PdfReader(str(pdf_path))
    texts = [page.extract_text() or "" for page in reader.pages]
    destinations = {
        name: reader.get_destination_page_number(destination)
        for name, destination in reader.named_destinations.items()
    }

    # Dokumentabhaengige Marker fuer Anfang des Fliesstexts bzw. der arabischen Zaehlung.
    joined = "\n".join(texts)
    if "Diese Arbeit dokumentiert die Modernisierung" in joined:
        body_marker, chapter_one_marker = (
            "Diese Arbeit dokumentiert die Modernisierung",
            "1. Einleitung",
        )
    elif "Diese Diagramme dokumentieren die" in joined:
        body_marker, chapter_one_marker = (
            "Diese Diagramme dokumentieren die",
            "1. Systemarchitektur",
        )
    else:
        body_marker, chapter_one_marker = (None, None)

    body_start = next(
        (index for index, text in enumerate(texts)
         if body_marker and body_marker in text),
        0,
    )
    chapter_one_start = next(
        (index for index, text in enumerate(texts)
         if chapter_one_marker and index >= body_start and chapter_one_marker in text),
        body_start,
    )

    regions = (
        (r'(<nav id="TOC".*?</nav>)',),
        (r'(<section id="list-of-figures".*?</section>)',),
        (r'(<section id="list-of-tables".*?</section>)',),
        (r'(<section id="list-of-listings".*?</section>)',),
    )
    for (pattern,) in regions:
        document = re.sub(
            pattern,
            lambda match: number_links(
                match.group(1),
                texts,
                destinations,
                body_start,
                chapter_one_start,
            ),
            document,
            flags=re.DOTALL,
        )

    # Das Snapshot-HTML enthaelt bereits gerenderte Mermaid-SVGs und erzeugte
    # Verzeichnisse. Skripte duerfen im zweiten Durchlauf nicht erneut laufen,
    # sonst entstehen doppelte Tabellen- und Verzeichniseintraege.
    document = re.sub(r"<script\b[^>]*>.*?</script>", "", document, flags=re.DOTALL)

    output_path.write_text(document, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
