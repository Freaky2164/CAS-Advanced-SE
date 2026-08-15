#!/usr/bin/env node
/* Rendert eine (bereits mit Mermaid.js versehene) HTML-Datei via Chromium nach PDF.
   Wartet, bis Mermaid alle Diagramme gerendert hat (window.__mermaidDone). */
const path = require('path');
const puppeteer = require('puppeteer-core');

(async () => {
  const input = process.argv[2];
  const output = process.argv[3];
  const snapshot = process.argv[4];
  if (!input || !output) {
    console.error('Usage: render.js <input.html> <output.pdf>');
    process.exit(2);
  }

  const browser = await puppeteer.launch({
    executablePath: process.env.CHROMIUM || '/usr/bin/chromium',
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--font-render-hinting=none', '--disable-dev-shm-usage']
  });

  try {
    const page = await browser.newPage();
    page.on('console', m => { if (m.type() === 'error') console.error('[page]', m.text()); });

    const fileUrl = 'file://' + path.resolve(input);
    await page.goto(fileUrl, { waitUntil: 'networkidle0', timeout: 180000 });

    // Warten bis Mermaid fertig ist (auch bei Diagramm-Fehlern setzt die Seite das Flag).
    await page.waitForFunction(
      () => window.__mermaidDone === true ||
        document.querySelectorAll('pre.mermaid, .mermaid').length === 0,
      { timeout: 180000 }
    );
    // Kurzer Puffer, damit Fonts/SVG-Layout final sind.
    await new Promise(r => setTimeout(r, 400));

    if (snapshot) {
      const fs = require('fs');
      fs.writeFileSync(snapshot, await page.content(), 'utf8');
    }

    await page.pdf({
      path: output,
      format: 'A4',
      printBackground: true,
      preferCSSPageSize: true,
      margin: { top: '20mm', bottom: '18mm', left: '20mm', right: '18mm' },
      displayHeaderFooter: true,
      headerTemplate:
        '<div style="width:100%;font-size:8px;color:#666;padding:0 18mm;' +
        'font-family:sans-serif;">' +
        '<span>Arbeitsgruppe 3 · Architektur</span></div>',
      footerTemplate:
        '<div></div>'
    });

    console.log('OK: PDF geschrieben ->', output);
  } finally {
    await browser.close();
  }
})().catch(err => { console.error(err); process.exit(1); });
