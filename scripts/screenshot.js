const { chromium } = require('playwright');

(async () => {
    const url = process.argv[2];
    const output = process.argv[3];

    if (!url || !output) {
        console.error('Usage: node screenshot.js <url> <output>');
        process.exit(1);
    }

    const browser = await chromium.launch({
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    const context = await browser.newContext({
        // 兜底：若误跳到 HTTPS 且证书与内网主机名不匹配，避免直接失败
        ignoreHTTPSErrors: true
    });
    const page = await context.newPage();

    await page.setViewportSize({
        width: 1280,
        height: 800
    });

    await page.goto(url, {
        waitUntil: 'domcontentloaded',
        timeout: 10000
    });

    // 等待页面稳定（关键）
    await page.waitForTimeout(1000);

    await page.screenshot({
        path: output,
        fullPage: false,
        clip: { x: 0, y: 0, width: 1280, height: 800 }
    });

    await context.close();
    await browser.close();
})();