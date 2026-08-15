#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer-core');

(async () => {
  const input = process.argv[2];
  const outputDirectory = process.argv[3];
  if (!input || !outputDirectory) {
    console.error('Usage: export_figures.js <rendered.html> <output-directory>');
    process.exit(2);
  }

  fs.mkdirSync(outputDirectory, { recursive: true });
  const browser = await puppeteer.launch({
    executablePath: process.env.CHROMIUM ||
      'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1600, height: 1200, deviceScaleFactor: 2 });
    await page.goto('file://' + path.resolve(input), {
      waitUntil: 'networkidle0',
      timeout: 180000
    });
    const figures = await page.$$('figure .mermaid-rendered');
    for (let index = 0; index < figures.length; index += 1) {
      await figures[index].screenshot({
        path: path.join(outputDirectory, `abbildung-${index + 1}.png`),
        type: 'png',
        omitBackground: false
      });
    }
    console.log(`${figures.length} Word-Diagramme exportiert`);
  } finally {
    await browser.close();
  }
})().catch(error => {
  console.error(error);
  process.exit(1);
});
