#!/bin/bash
# Entrypoint des PDF-Containers: Markdown -> HTML (Pandoc) -> PDF (Chromium).
set -euo pipefail

IN="${1:-seminararbeit-architektur.md}"
OUT="${2:-seminararbeit-architektur.pdf}"

if [[ ! -f "$IN" ]]; then
  echo "Eingabedatei nicht gefunden: $IN (Arbeitsverzeichnis: $(pwd))" >&2
  exit 1
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# Assets, die das HTML relativ referenziert, neben das HTML legen:
cp /opt/assets/mermaid.min.js "$TMP/mermaid.min.js"
cp /opt/assets/style.css      "$TMP/style.css"

echo ">> Pandoc: Markdown -> HTML"
pandoc "$IN" \
  --standalone \
  --toc --toc-depth=3 \
  --template /opt/assets/template.html \
  --metadata=lang:de \
  -o "$TMP/render.html"

echo ">> Chromium: Vorab-PDF fuer Seitenzuordnung"
node /opt/app/render.js "$TMP/render.html" "$TMP/preliminary.pdf" "$TMP/rendered.html"

echo ">> Seitenzahlen in Verzeichnisse eintragen"
python3 /opt/app/add_directory_page_numbers.py \
  "$TMP/rendered.html" "$TMP/preliminary.pdf" "$TMP/numbered.html"

echo ">> Chromium: finales Layout"
node /opt/app/render.js "$TMP/numbered.html" "$TMP/base.pdf"

echo ">> Autorenkopfzeilen zuordnen"
python3 /opt/app/apply_author_headers.py "$TMP/base.pdf" "$OUT"

echo ">> Fertig: $OUT"
