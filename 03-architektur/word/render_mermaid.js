#!/usr/bin/env node
/* Rendert jeden ```mermaid-Block einer Markdown-Datei als hochaufgeloestes PNG,
   damit die Diagramme in ein Word-Dokument eingebettet werden koennen. */
const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer-core');

const mdFile = process.argv[2];
const outDir = process.argv[3];
const mermaidJs = process.argv[4] || '/opt/assets/mermaid.min.js';

if (!mdFile || !outDir) {
  console.error('Usage: render_mermaid.js <input.md> <outDir> [mermaid.min.js]');
  process.exit(2);
}

function extractMermaid(md) {
  const blocks = [];
  const re = /```mermaid[ \t]*\r?\n([\s\S]*?)\r?\n```/g;
  let m;
  while ((m = re.exec(md)) !== null) blocks.push(m[1]);
  return blocks;
}

(async () => {
  const md = fs.readFileSync(mdFile, 'utf8');
  const blocks = extractMermaid(md);
  fs.mkdirSync(outDir, { recursive: true });
  const mermaidSrc = fs.readFileSync(mermaidJs, 'utf8');

  const manifest = [];
  const browser = await puppeteer.launch({
    executablePath: process.env.CHROMIUM || '/usr/bin/chromium',
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--font-render-hinting=none', '--disable-dev-shm-usage']
  });

  try {
    for (let i = 0; i < blocks.length; i++) {
      const page = await browser.newPage();
      await page.setViewport({ width: 2800, height: 2000, deviceScaleFactor: 2 });
      const html = '<!DOCTYPE html><html><head><meta charset="utf-8">' +
        '<style>html,body{margin:0;padding:0;background:#ffffff;}' +
        '#box{display:inline-block;background:#ffffff;padding:14px;}' +
        '#box svg{font-family:"Noto Sans","DejaVu Sans",Arial,sans-serif;}</style>' +
        '<script>' + mermaidSrc + '</script></head><body><div id="box"></div></body></html>';
      await page.setContent(html, { waitUntil: 'networkidle0', timeout: 120000 });

      const result = await page.evaluate(async (src) => {
        // eslint-disable-next-line no-undef
        mermaid.initialize({
          startOnLoad: false,
          theme: 'neutral',
          securityLevel: 'loose',
          maxTextSize: 900000,
          flowchart: { htmlLabels: true, useMaxWidth: false, nodeSpacing: 40, rankSpacing: 55 },
          sequence: { useMaxWidth: false },
          er: { useMaxWidth: false }
        });
        try {
          // eslint-disable-next-line no-undef
          const { svg } = await mermaid.render('g' + Math.random().toString(36).slice(2), src);
          document.getElementById('box').innerHTML = svg;
          return true;
        } catch (e) {
          return String((e && e.message) ? e.message : e);
        }
      }, blocks[i]);

      if (result !== true) {
        console.error('FAIL diagram ' + (i + 1) + ': ' + result);
        process.exit(1);
      }

      await new Promise(r => setTimeout(r, 200));
      const clip = await page.evaluate(() => {
        const b = document.getElementById('box').getBoundingClientRect();
        return { x: b.x, y: b.y, width: Math.ceil(b.width), height: Math.ceil(b.height) };
      });
      const out = path.join(outDir, 'diagram-' + (i + 1) + '.png');
      await page.screenshot({ path: out, clip });
      manifest.push({ n: i + 1, width: clip.width, height: clip.height });
      console.log('OK diagram ' + (i + 1) + ' -> ' + out + ' (' + clip.width + 'x' + clip.height + ')');
      await page.close();
    }
    fs.writeFileSync(path.join(outDir, 'diagrams.json'), JSON.stringify(manifest));
    console.log('Diagramme gerendert: ' + blocks.length);
  } finally {
    await browser.close();
  }
})().catch(err => { console.error(err); process.exit(1); });
