#!/bin/sh
# @author Nils
#
# Läuft beim Container-Start (nginx: /docker-entrypoint.d/*.sh), BEVOR nginx
# startet: Liegt unter /etc/nginx/tls kein Zertifikat (Produktion: echtes
# Zertifikat dorthin mounten), wird ein selbstsigniertes für localhost erzeugt –
# nur für Entwicklung/Test, der Browser zeigt dafür eine Warnung an.
# Der private Schlüssel entsteht erst zur Laufzeit und liegt damit weder im
# Git-Repository noch im Docker-Image.
set -eu

CRT=/etc/nginx/tls/tls.crt
KEY=/etc/nginx/tls/tls.key

if [ ! -f "$CRT" ] || [ ! -f "$KEY" ]; then
    echo "TLS: kein Zertifikat unter /etc/nginx/tls gefunden – erzeuge selbstsigniertes für localhost (nur für Entwicklung!)"
    mkdir -p /etc/nginx/tls
    openssl req -x509 -newkey rsa:2048 -nodes -days 825 \
        -keyout "$KEY" -out "$CRT" \
        -subj "/CN=localhost" \
        -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
    chmod 600 "$KEY"
fi
