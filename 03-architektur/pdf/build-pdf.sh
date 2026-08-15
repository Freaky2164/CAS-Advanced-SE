#!/usr/bin/env bash
# build-pdf.sh — Baut das Image (einmalig) und erzeugt die PDF im Container.
#   ./pdf/build-pdf.sh [input.md] [output.pdf]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # .../03-architektur/pdf
ARCH_DIR="$(dirname "$SCRIPT_DIR")"                          # .../03-architektur
IMAGE="fhma-seminar-pdf:latest"
IN="${1:-seminararbeit-architektur.md}"
OUT="${2:-seminararbeit-architektur.pdf}"

if ! docker info >/dev/null 2>&1; then
  echo "Docker-Daemon nicht erreichbar. Bitte Docker starten." >&2
  exit 1
fi

if [[ -z "$(docker images -q "$IMAGE")" ]]; then
  echo ">> Baue Image $IMAGE ..."
  docker build -t "$IMAGE" "$SCRIPT_DIR"
fi

echo ">> Erzeuge PDF aus $IN -> $OUT"
docker run --rm -v "${ARCH_DIR}:/work" "$IMAGE" "$IN" "$OUT"
echo ">> Fertig: ${ARCH_DIR}/${OUT}"
