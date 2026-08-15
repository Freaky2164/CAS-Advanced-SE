#!/usr/bin/env python3
"""Ordnet PDF-Seiten anhand der Hauptkapitel genau einem verantwortlichen Autor zu."""

from __future__ import annotations

import io
import sys
from pathlib import Path

from pypdf import PdfReader, PdfWriter
from reportlab.lib.colors import HexColor
from reportlab.pdfgen import canvas


AUTHORS = {
    "foundations": "Nils Firschau",
    "architecture": "Paul Faller",
    "operations": "Robin Steiner",
    "evaluation": "Ole Schildt",
}


def find_page(texts: list[str], marker: str, start: int) -> int:
    for index in range(start, len(texts)):
        if marker in texts[index]:
            return index
    raise RuntimeError(f"Kapitelanfang nicht gefunden: {marker}")


def seminar_assignments(texts: list[str]) -> list[str]:
    """Seminararbeit: vier zusammenhaengende Kapitelbloecke."""
    body_start = find_page(texts, "Diese Arbeit dokumentiert die Modernisierung", 0)
    architecture_start = find_page(texts, "4. Implementierte Systemarchitektur", body_start)
    operations_start = find_page(texts, "6. Persistenz, Audit und Dokumente", architecture_start)
    evaluation_start = find_page(texts, "9. Risiken und technische Schulden", operations_start)

    assignments: list[str] = []
    for index in range(len(texts)):
        if index < architecture_start:
            assignments.append(AUTHORS["foundations"])
        elif index < operations_start:
            assignments.append(AUTHORS["architecture"])
        elif index < evaluation_start:
            assignments.append(AUTHORS["operations"])
        else:
            assignments.append(AUTHORS["evaluation"])
    return assignments


def diagram_assignments(texts: list[str]) -> list[str]:
    """Architekturdiagramme: Zuordnung nach Themenverantwortung wie in der Seminararbeit."""
    body_start = find_page(texts, "Diese Diagramme dokumentieren die", 0)
    security_start = find_page(texts, "2. Sicherheitsarchitektur (zwei Filterketten", body_start)
    layers_start = find_page(texts, "3. Anwendungsschichten (Bausteinsicht)", security_start)
    auth_start = find_page(texts, "Autorisierung (Sequenz)", layers_start)
    summary_start = find_page(texts, "Zusammenfassung der Abweichungen von der Planungsdoku", auth_start)

    assignments: list[str] = []
    for index in range(len(texts)):
        if index < security_start:
            # Titel, Inhaltsverzeichnis, Einleitung, 1. Systemarchitektur
            assignments.append(AUTHORS["architecture"])
        elif index < layers_start:
            # 2. Sicherheitsarchitektur
            assignments.append(AUTHORS["operations"])
        elif index < auth_start:
            # 3. Anwendungsschichten, 4. Domaenenmodell
            assignments.append(AUTHORS["architecture"])
        elif index < summary_start:
            # 5. Authentifizierung, 6. Dokument-/Report-Erzeugung
            assignments.append(AUTHORS["operations"])
        else:
            # Zusammenfassung der Abweichungen
            assignments.append(AUTHORS["evaluation"])
    return assignments


def build_assignments(texts: list[str]) -> list[str]:
    joined = "\n".join(texts)
    if "Implementierte Systemarchitektur" in joined:
        return seminar_assignments(texts)
    if "Anwendungsschichten (Bausteinsicht)" in joined:
        return diagram_assignments(texts)
    # Fallback: gesamtes Dokument dem Architektur-Verantwortlichen zuordnen.
    return [AUTHORS["architecture"]] * len(texts)


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


def page_label(texts: list[str], page_index: int) -> str:
    if page_index == 0:
        return ""
    joined = "\n".join(texts)
    if "Implementierte Systemarchitektur" in joined:
        body_start = find_page(texts, "Diese Arbeit dokumentiert die Modernisierung", 0)
        chapter_one_start = find_page(texts, "1. Einleitung", body_start)
        if page_index < chapter_one_start:
            return roman(page_index)
        return str(page_index - chapter_one_start + 1)
    return str(page_index)


def page_overlay(width: float, height: float, author: str, label: str):
    buffer = io.BytesIO()
    pdf = canvas.Canvas(buffer, pagesize=(width, height))
    pdf.setFillColor(HexColor("#666666"))
    pdf.setFont("Helvetica", 8)
    pdf.drawRightString(width - 51, height - 24, author)
    if label:
        pdf.drawCentredString(width / 2, 17, label)
    pdf.save()
    buffer.seek(0)
    return PdfReader(buffer).pages[0]


def print_ranges(assignments: list[str]) -> None:
    if not assignments:
        return
    run_author = assignments[0]
    run_start = 0
    for index in range(1, len(assignments) + 1):
        if index == len(assignments) or assignments[index] != run_author:
            print(f"{run_author}: Seiten {run_start + 1}-{index}")
            if index < len(assignments):
                run_author = assignments[index]
                run_start = index


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: apply_author_headers.py <input.pdf> <output.pdf>", file=sys.stderr)
        return 2

    source = Path(sys.argv[1])
    target = Path(sys.argv[2])
    reader = PdfReader(str(source))
    texts = [(page.extract_text() or "") for page in reader.pages]
    assignments = build_assignments(texts)

    writer = PdfWriter()
    for page_index, (page, author) in enumerate(zip(reader.pages, assignments)):
        width = float(page.mediabox.width)
        height = float(page.mediabox.height)
        page.merge_page(
            page_overlay(width, height, author, page_label(texts, page_index))
        )
        writer.add_page(page)

    writer.add_metadata(reader.metadata or {})
    with target.open("wb") as output:
        writer.write(output)

    print_ranges(assignments)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
