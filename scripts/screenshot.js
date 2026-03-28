const { chromium } = require('playwright');

(async () => {
    const url = process.argv[2];
    const output = process.argv[3];

    if (!url || !output) {
        console.error('Usage: node screenshot.js <url> <output>');
        process.exit(1);
    }

    const browser = await chromium.launch();
    const page = await browser.newPage();

    await page.setViewportSize({
        width: 1280,
        height: 800
    });

    await page.goto(url, { waitUntil: 'networkidle' });

    // 等待页面稳定（关键）
    await page.waitForTimeout(1000);

    await page.screenshot({
        path: output,
        fullPage: false,
        clip: { x: 0, y: 0, width: 1280, height: 800 }
    });

    await browser.close();
})();