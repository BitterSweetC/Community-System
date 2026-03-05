import asyncio
from playwright.async_api import async_playwright

BASE = "http://localhost:5173"
OUT = "e:/tmp/test_screenshots"

async def run():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page(viewport={"width": 1280, "height": 800})
        errors = []
        page.on("pageerror", lambda e: errors.append(str(e)))

        # 1. Landing / login page
        print("=== 1. LANDING PAGE ===")
        await page.goto(BASE, wait_until="networkidle", timeout=15000)
        print(f"URL: {page.url}, Title: {await page.title()}")
        await page.screenshot(path=f"{OUT}/01_landing.png")

        # 2. Login with admin/123456
        print("\n=== 2. LOGIN (admin/123456) ===")
        await page.goto(f"{BASE}/login", wait_until="networkidle", timeout=15000)
        await page.screenshot(path=f"{OUT}/02_login_page.png")

        await page.fill("input[type='text']", "admin")
        await page.fill("input[type='password']", "123456")
        await page.screenshot(path=f"{OUT}/03_login_filled.png")
        await page.click(".el-button--primary")
        await page.wait_for_timeout(3000)
        print(f"After login URL: {page.url}")
        await page.screenshot(path=f"{OUT}/04_after_login.png")

        # Check for alert/error
        try:
            dialog_text = await page.inner_text(".el-message, .el-alert, [class*='error']", timeout=1000)
            print(f"Message: {dialog_text}")
        except: pass

        # 3. Admin pages
        if "login" not in page.url:
            print("\n=== 3. ADMIN PAGES ===")
            for path, label in [
                ("/admin", "admin_dashboard"),
                ("/admin/clubs", "admin_clubs"),
                ("/admin/activities", "admin_activities"),
                ("/admin/recruit", "admin_recruit"),
                ("/admin/resources", "admin_resources"),
                ("/admin/finance", "admin_finance"),
                ("/admin/notices", "admin_notices"),
            ]:
                try:
                    await page.goto(f"{BASE}{path}", wait_until="networkidle", timeout=8000)
                    print(f"  {path} -> {page.url}")
                    await page.screenshot(path=f"{OUT}/page_{label}.png")
                except Exception as e:
                    print(f"  {path} ERROR: {e}")
        else:
            print("Still on login page - login may have failed")

        # 4. Logout via API then test student login
        print("\n=== 4. STUDENT LOGIN (student/123456) ===")
        await page.evaluate("() => { localStorage.clear(); sessionStorage.clear(); }")
        await page.context.clear_cookies()
        await page.goto(f"{BASE}/login", wait_until="networkidle", timeout=10000)
        await page.fill("input[type='text']", "student")
        await page.fill("input[type='password']", "123456")
        await page.click(".el-button--primary")
        await page.wait_for_timeout(3000)
        print(f"Student login URL: {page.url}")
        await page.screenshot(path=f"{OUT}/05_student_after_login.png")

        if "login" not in page.url:
            for path, label in [
                ("/student", "student_home"),
                ("/student/clubs", "student_clubs"),
                ("/student/activities", "student_activities"),
            ]:
                try:
                    await page.goto(f"{BASE}{path}", wait_until="networkidle", timeout=8000)
                    print(f"  {path} -> {page.url}")
                    await page.screenshot(path=f"{OUT}/page_{label}.png")
                except Exception as e:
                    print(f"  {path} ERROR: {e}")

        print(f"\nJS Errors ({len(errors)}):")
        for e in errors: print(f"  {e}")
        print(f"\nScreenshots saved to {OUT}")
        await browser.close()

asyncio.run(run())
