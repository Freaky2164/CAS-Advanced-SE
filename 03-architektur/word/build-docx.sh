#!/bin/bash
# Entrypoint des Word-Containers:
#   Markdown -> (Mermaid-PNGs) -> Pandoc -> DOCX -> LibreOffice (TOC-Felder + PDF-Vorschau)
set -euo pipefail

IN="${1:-03-architekturdiagramme.md}"
OUT="${2:-03-architekturdiagramme.docx}"
export HOME=/tmp

if [[ ! -f "$IN" ]]; then
  echo "Eingabedatei nicht gefunden: $IN (Arbeitsverzeichnis: $(pwd))" >&2
  exit 1
fi

BUILD="$(mktemp -d)"
trap 'rm -rf "$BUILD"' EXIT
mkdir -p "$BUILD/img"

echo ">> Mermaid -> PNG"
node /opt/app/render_mermaid.js "$IN" "$BUILD/img" /opt/assets/mermaid.min.js

echo ">> Arbeits-Markdown erzeugen"
python3 /opt/app/prepare_docx_md.py "$IN" "$BUILD/work.md" img "$BUILD/img/diagrams.json"

echo ">> Pandoc: Markdown -> DOCX"
pandoc "$BUILD/work.md" \
  --from=markdown \
  --toc --toc-depth=3 \
  --resource-path="$BUILD" \
  -o "$BUILD/out.docx"

echo ">> LibreOffice-Server starten"
soffice --headless --invisible --nodefault --norestore --nologo \
  --accept="socket,host=localhost,port=2002;urp;" &
SOFFICE_PID=$!
sleep 2

echo ">> TOC-Felder aktualisieren + PDF-Vorschau exportieren"
python3 /opt/app/update_and_export.py "$BUILD/out.docx" "$BUILD/final.docx" "$BUILD/final.pdf"

kill "$SOFFICE_PID" 2>/dev/null || true

cp "$BUILD/final.docx" "$OUT"
PDFOUT="${OUT%.docx}-word-vorschau.pdf"
cp "$BUILD/final.pdf" "$PDFOUT" 2>/dev/null || true

echo ">> Fertig: $OUT"
echo ">> Vorschau: $PDFOUT"
