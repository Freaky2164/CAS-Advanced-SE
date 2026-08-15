#!/usr/bin/env python3
"""Bereitet die Markdown-Seminararbeit fuer einen hochwertigen Word-Export vor."""

from __future__ import annotations

import html
import re
import sys
from pathlib import Path

import fitz
from bs4 import BeautifulSoup


PAGE_BREAK = """```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```"""


def parse_metadata(lines: list[str]) -> tuple[dict[str, str], list[str]]:
    if not lines or lines[0].strip() != "---":
        return {}, lines
    metadata: dict[str, str] = {}
    end = 1
    while end < len(lines) and lines[end].strip() != "---":
        match = re.match(r"^([A-Za-z0-9_-]+):\s*[\"']?(.*?)[\"']?\s*$", lines[end])
        if match:
            metadata[match.group(1)] = match.group(2)
        end += 1
    return metadata, lines[end + 1 :]


def extract_figures(rendered_html: Path, assets: Path) -> int:
    document = rendered_html.read_text(encoding="utf-8")
    soup = BeautifulSoup(document, "html.parser")
    svgs = [str(svg) for svg in soup.select("figure .mermaid-rendered svg")]
    assets.mkdir(parents=True, exist_ok=True)
    for index, svg in enumerate(svgs, start=1):
        svg = html.unescape(svg)
        if "xmlns=" not in svg.split(">", 1)[0]:
            svg = svg.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" ', 1)
        source = fitz.open(stream=svg.encode("utf-8"), filetype="svg")
        pixmap = source[0].get_pixmap(matrix=fitz.Matrix(2.4, 2.4), alpha=False)
        pixmap.save(assets / f"abbildung-{index}.png")
    return len(svgs)


def custom_paragraph(style: str, text: str) -> list[str]:
    return [
        f'::: {{custom-style="{style}"}}',
        text,
        ":::",
        "",
    ]


def strip_heading_number(value: str) -> str:
    return re.sub(r"^\d+(?:\.\d+)*\.?\s*", "", value).strip()


def collect_directory_entries(lines: list[str]) -> dict[str, list[tuple[int, str]]]:
    entries: dict[str, list[tuple[int, str]]] = {
        "toc": [],
        "figures": [],
        "tables": [],
        "listings": [],
    }
    current_heading = "Übersicht"
    figure_number = 0
    table_number = 0
    listing_number = 0
    index = 0
    while index < len(lines):
        line = lines[index]
        stripped = line.strip()
        if stripped.startswith("```") and stripped != "```mermaid":
            index += 1
            while index < len(lines) and not lines[index].strip().startswith("```"):
                index += 1
            index += 1
            continue
        heading = re.match(r"^(#{1,4})\s+(.+)$", line)
        if heading:
            level = len(heading.group(1))
            current_heading = heading.group(2).strip()
            if level <= 3:
                entries["toc"].append((level, current_heading.replace("`", "")))

        if line.strip() == "```mermaid":
            figure_number += 1
            index += 1
            while index < len(lines) and lines[index].strip() != "```":
                index += 1
            index += 1
            while index < len(lines) and not lines[index].strip():
                index += 1
            caption = strip_heading_number(current_heading)
            if index < len(lines):
                match = re.match(r"^\*Abbildung:\s*(.*?)\.?\*$", lines[index].strip())
                if match:
                    caption = match.group(1).rstrip(".")
                    index += 1
            entries["figures"].append((1, f"Abbildung {figure_number}: {caption}"))
            continue

        listing = re.match(r"^\*\*Listing:\s*(.*?)\.?\*\*$", line.strip())
        if listing:
            listing_number += 1
            entries["listings"].append(
                (1, f"Listing {listing_number}: {listing.group(1).rstrip('.')}")
            )

        if line.startswith("|") and index + 1 < len(lines) and lines[index + 1].startswith("|"):
            while index < len(lines) and lines[index].startswith("|"):
                index += 1
            while index < len(lines) and not lines[index].strip():
                index += 1
            caption = strip_heading_number(current_heading)
            if index < len(lines) and lines[index].startswith(": "):
                caption = lines[index][2:].strip()
                index += 1
            table_number += 1
            entries["tables"].append((1, f"Tabelle {table_number}: {caption}"))
            continue
        index += 1
    return entries


def process_body(lines: list[str], assets: Path) -> list[str]:
    output: list[str] = []
    current_heading = "Übersicht"
    figure_number = 0
    table_number = 0
    listing_number = 0
    index = 0

    while index < len(lines):
        line = lines[index]
        stripped = line.strip()
        if stripped.startswith("```") and stripped != "```mermaid":
            output.append(line)
            index += 1
            while index < len(lines) and not lines[index].strip().startswith("```"):
                output.append(lines[index])
                index += 1
            if index < len(lines):
                output.append(lines[index])
                index += 1
            continue
        heading = re.match(r"^(#{1,4})\s+(.+)$", line)
        if heading:
            current_heading = heading.group(2).strip()

        if line.strip() == "```mermaid":
            figure_number += 1
            index += 1
            while index < len(lines) and lines[index].strip() != "```":
                index += 1
            index += 1
            while index < len(lines) and not lines[index].strip():
                index += 1
            caption = strip_heading_number(current_heading)
            if index < len(lines):
                match = re.match(r"^\*Abbildung:\s*(.*?)\.?\*$", lines[index].strip())
                if match:
                    caption = match.group(1).rstrip(".")
                    index += 1
            image = (assets / f"abbildung-{figure_number}.png").as_posix()
            output.extend([f"![]({image}){{width=15.5cm}}", ""])
            output.extend(
                custom_paragraph(
                    "Abbildungsbeschriftung",
                    f"Abbildung {figure_number}: {caption}",
                )
            )
            continue

        listing = re.match(r"^\*\*Listing:\s*(.*?)\.?\*\*$", line.strip())
        if listing:
            listing_number += 1
            output.extend(
                custom_paragraph(
                    "Listingbeschriftung",
                    f"Listing {listing_number}: {listing.group(1).rstrip('.')}",
                )
            )
            index += 1
            continue

        if line.startswith("|") and index + 1 < len(lines) and lines[index + 1].startswith("|"):
            table_lines = []
            while index < len(lines) and lines[index].startswith("|"):
                table_lines.append(lines[index])
                index += 1
            while index < len(lines) and not lines[index].strip():
                index += 1
            caption = strip_heading_number(current_heading)
            if index < len(lines) and lines[index].startswith(": "):
                caption = lines[index][2:].strip()
                index += 1
            table_number += 1
            output.extend(
                custom_paragraph(
                    "Tabellenbeschriftung",
                    f"Tabelle {table_number}: {caption}",
                )
            )
            output.extend(table_lines)
            output.append("")
            continue

        output.append(line)
        index += 1
    return output


def directory_paragraph(level: int, text: str) -> list[str]:
    escaped = re.sub(r"^(\d+)\.\s", r"\1\\. ", text)
    return custom_paragraph(f"DirectoryEntry{level}", f"{escaped}\t??")


def front_matter(
    metadata: dict[str, str],
    entries: dict[str, list[tuple[int, str]]],
) -> list[str]:
    title = metadata.get("title", "Seminararbeit Architektur")
    subtitle = metadata.get("subtitle", "")
    module = metadata.get("module", "")
    professor = metadata.get("professor", "")
    authors = metadata.get("author", "")
    date = metadata.get("date", "")

    result: list[str] = []
    result.extend(custom_paragraph("Institution", "**DHBW** CAS"))
    result.extend(custom_paragraph("InstitutionSub", "Duale Hochschule Baden-Württemberg  \nCenter for Advanced Studies"))
    result.extend(custom_paragraph("TitleRule", "\u00a0"))
    result.extend(custom_paragraph("DocumentKind", "Projekt- und Prüfungsleistung FH_MA"))
    result.extend(custom_paragraph("WordTitle", title))
    result.extend(custom_paragraph("WordSubtitle", subtitle))
    result.extend(custom_paragraph("TitleRule", "\u00a0"))
    result.extend(
        [
            "| | |",
            "|---|---|",
            f"| **Modul** | {module} |",
            f"| **Studiengang / Prüfer** | {professor} |",
            f"| **Autoren** | {authors.replace(' · ', '; ')} |",
            f"| **Datum** | {date} |",
            "",
            PAGE_BREAK,
            "",
        ]
    )

    directories = (
        ("Inhaltsverzeichnis", "toc"),
        ("Abkürzungsverzeichnis", "abbreviations"),
        ("Abbildungsverzeichnis", "figures"),
        ("Tabellenverzeichnis", "tables"),
        ("Listingverzeichnis", "listings"),
    )
    abbreviations = [
        ("ADR", "Architecture Decision Record"),
        ("API", "Application Programming Interface"),
        ("ASVS", "Application Security Verification Standard"),
        ("C4", "Context, Container, Component und Code"),
        ("CI", "Continuous Integration"),
        ("CSRF", "Cross-Site Request Forgery"),
        ("DBMS", "Datenbankmanagementsystem"),
        ("DSGVO", "Datenschutz-Grundverordnung"),
        ("DTO", "Data Transfer Object"),
        ("JPA", "Jakarta Persistence API"),
        ("JDBC", "Java Database Connectivity"),
        ("LAN", "Local Area Network"),
        ("MFA", "Multi-Factor Authentication"),
        ("ORM", "Object-Relational Mapping"),
        ("RBAC", "Role-Based Access Control"),
        ("RLS", "Row-Level Security"),
        ("RPO", "Recovery Point Objective"),
        ("RTO", "Recovery Time Objective"),
        ("SMTP", "Simple Mail Transfer Protocol"),
        ("SQL", "Structured Query Language"),
        ("TLS", "Transport Layer Security"),
        ("UI", "User Interface"),
        ("WAL", "Write-Ahead Log"),
    ]
    for heading, directory_key in directories:
        result.extend(custom_paragraph("FrontHeading", heading))
        if heading == "Abkürzungsverzeichnis":
            result.extend(["| Abkürzung | Bedeutung |", "|---|---|"])
            result.extend(f"| **{key}** | {value} |" for key, value in abbreviations)
            result.append("")
        else:
            for level, text in entries[directory_key]:
                result.extend(directory_paragraph(level, text))
        result.extend([PAGE_BREAK, ""])
    return result


def main() -> int:
    if len(sys.argv) != 5:
        print(
            "Usage: prepare_word_source.py <source.md> <rendered.html> <output.md> <assets-dir>",
            file=sys.stderr,
        )
        return 2
    source, rendered, output, assets = map(Path, sys.argv[1:])
    lines = source.read_text(encoding="utf-8").splitlines()
    metadata, body = parse_metadata(lines)
    count = extract_figures(rendered, assets)
    entries = collect_directory_entries(body)
    prepared = front_matter(metadata, entries) + process_body(body, assets)
    output.write_text("\n".join(prepared) + "\n", encoding="utf-8")
    print(f"{count} Diagramme fuer Word exportiert")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
