import { expect, test } from '@playwright/test';

const DEMO_USER = 'demo';
const DEMO_PASSWORD = 'demo-password-change-me';

test.describe('Briefing-Agent smoke', () => {
  test('login and reach the dashboard', async ({ page }) => {
    await page.goto('/');

    await page.locator('input#username').fill(DEMO_USER);
    await page.locator('input#password').fill(DEMO_PASSWORD);
    await page.getByRole('button', { name: 'Anmelden' }).click();

    await page.waitForURL((url) => url.pathname.startsWith('/dashboard'));
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  });

  test('create an LLM provider on the configuration page', async ({ page }) => {
    await page.goto('/');
    await page.locator('input#username').fill(DEMO_USER);
    await page.locator('input#password').fill(DEMO_PASSWORD);
    await page.getByRole('button', { name: 'Anmelden' }).click();
    await page.waitForURL((url) => url.pathname.startsWith('/dashboard'));

    await page.goto('/configuration');
    await expect(page.getByRole('heading', { name: 'Konfiguration' })).toBeVisible();

    const providerName = `Smoke LLM ${Date.now()}`;
    const llmCard = page.locator('article.surface-card').filter({ hasText: 'LLM-Provider' });

    await llmCard.locator('input[formControlName="name"]').fill(providerName);
    await llmCard.locator('input[formControlName="endpointUrl"]').fill('http://smoke.invalid/v1');
    await llmCard.locator('input[formControlName="modelName"]').fill('smoke-model');
    await llmCard.locator('input[formControlName="apiType"]').fill('openai_compatible');
    await llmCard.getByRole('button', { name: 'Anlegen' }).click();

    await expect(llmCard.getByText(providerName, { exact: false })).toBeVisible();
  });
});
