# Word-Toolchain (Container): erweitert das PDF-Image um LibreOffice + python3-uno,
# um aus der Markdown ein sauber formatiertes DOCX (mit nativem Word-Inhaltsverzeichnis)
# zu erzeugen. Mermaid-Diagramme werden vorab als PNG gerendert und eingebettet.
FROM fhma-seminar-pdf:latest

RUN apt-get update && apt-get install -y --no-install-recommends \
        libreoffice-writer \
        libreoffice-core \
        python3-uno \
    && rm -rf /var/lib/apt/lists/*

COPY render_mermaid.js     /opt/app/render_mermaid.js
COPY prepare_docx_md.py    /opt/app/prepare_docx_md.py
COPY update_and_export.py  /opt/app/update_and_export.py
COPY build-docx.sh         /usr/local/bin/build-docx
RUN sed -i 's/\r$//' /usr/local/bin/build-docx && chmod +x /usr/local/bin/build-docx

WORKDIR /work
ENTRYPOINT ["build-docx"]
