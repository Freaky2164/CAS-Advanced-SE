# Containerisierte PDF-Toolchain für die Seminararbeit.
# Rendert Markdown (inkl. Mermaid-Diagrammen, Emojis, Umlauten) nach PDF:
#   Pandoc  ->  HTML (mit Mermaid.js)  ->  Chromium (print-to-PDF)
# Keine lokale Installation von Pandoc/LaTeX/Node nötig – alles im Image.
FROM node:20-bookworm-slim

# Pandoc + Chromium + vollständige Schriftabdeckung (Latein, CJK-Fallback, Farb-Emoji)
RUN apt-get update && apt-get install -y --no-install-recommends \
        pandoc \
        chromium \
        python3 \
        python3-pip \
        fonts-dejavu \
        fonts-noto \
        fonts-noto-color-emoji \
        fonts-noto-cjk \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN python3 -m pip install --break-system-packages --no-cache-dir pypdf reportlab

ENV CHROMIUM=/usr/bin/chromium \
    PUPPETEER_SKIP_DOWNLOAD=true

WORKDIR /opt/app

# puppeteer-core nutzt das System-Chromium; mermaid liefert das JS-Bundle
RUN npm init -y >/dev/null 2>&1 \
    && npm install --no-audit --no-fund puppeteer-core@22 mermaid@10 \
    && mkdir -p /opt/assets \
    && cp node_modules/mermaid/dist/mermaid.min.js /opt/assets/mermaid.min.js

COPY template.html /opt/assets/template.html
COPY style.css     /opt/assets/style.css
COPY render.js     /opt/app/render.js
COPY apply_author_headers.py /opt/app/apply_author_headers.py
COPY add_directory_page_numbers.py /opt/app/add_directory_page_numbers.py
COPY md2pdf.sh     /usr/local/bin/md2pdf
RUN sed -i 's/\r$//' /usr/local/bin/md2pdf && chmod +x /usr/local/bin/md2pdf

WORKDIR /work
ENTRYPOINT ["md2pdf"]
