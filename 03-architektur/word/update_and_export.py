#!/usr/bin/env python3
"""Oeffnet ein DOCX in LibreOffice (UNO), aktualisiert Felder + Verzeichnisse
(damit das Inhaltsverzeichnis echte Seitenzahlen enthaelt), speichert es wieder
als DOCX und exportiert zusaetzlich eine PDF-Vorschau."""

from __future__ import annotations

import sys
import time
from pathlib import Path

import uno
from com.sun.star.beans import PropertyValue


def connect(port: int = 2002, attempts: int = 40):
    local = uno.getComponentContext()
    resolver = local.ServiceManager.createInstanceWithContext(
        "com.sun.star.bridge.UnoUrlResolver", local
    )
    last = None
    for _ in range(attempts):
        try:
            ctx = resolver.resolve(
                f"uno:socket,host=localhost,port={port};urp;StarOffice.ComponentContext"
            )
            return ctx
        except Exception as exc:  # noqa: BLE001
            last = exc
            time.sleep(0.5)
    raise RuntimeError(f"LibreOffice-Verbindung fehlgeschlagen: {last}")


def prop(name: str, value):
    p = PropertyValue()
    p.Name = name
    p.Value = value
    return p


def main() -> int:
    if len(sys.argv) != 4:
        print("Usage: update_and_export.py <in.docx> <out.docx> <out.pdf>", file=sys.stderr)
        return 2

    in_docx, out_docx, out_pdf = (Path(a) for a in sys.argv[1:])

    ctx = connect()
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)

    url = uno.systemPathToFileUrl(str(in_docx.resolve()))
    doc = desktop.loadComponentFromURL(url, "_blank", 0, (prop("Hidden", True),))

    try:
        # Felder aktualisieren (Seitenzahlen etc.).
        try:
            doc.getTextFields().refresh()
        except Exception:  # noqa: BLE001
            pass
        # Alle Verzeichnisse (Inhaltsverzeichnis) aktualisieren.
        try:
            indexes = doc.getDocumentIndexes()
            for i in range(indexes.getCount()):
                indexes.getByIndex(i).update()
        except Exception:  # noqa: BLE001
            pass
        try:
            doc.refresh()
        except Exception:  # noqa: BLE001
            pass

        docx_url = uno.systemPathToFileUrl(str(out_docx.resolve()))
        doc.storeToURL(docx_url, (prop("FilterName", "MS Word 2007 XML"),))

        pdf_url = uno.systemPathToFileUrl(str(out_pdf.resolve()))
        doc.storeToURL(pdf_url, (prop("FilterName", "writer_pdf_Export"),))
    finally:
        doc.close(False)

    print("DOCX + PDF aktualisiert.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
