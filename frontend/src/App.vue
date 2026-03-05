<template>
  <router-view />
</template>

<style>
:root {
  --color-primary: #4f46e5;
  --color-primary-light: #6366f1;
  --color-primary-dark: #4338ca;
  --color-secondary: #3b82f6;
  --color-success: #10b981;
  --color-warning: #f59e0b;
  --color-danger: #ef4444;
  --color-text: #1e293b;
  --color-text-secondary: #64748b;
  --color-text-light: #94a3b8;
  --color-bg: #ffffff;
  --color-bg-secondary: #f8fafc;
  --color-border: #e2e8f0;
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.1);
  --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.1);
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --font-heading: 'DM Serif Display', -apple-system, BlinkMacSystemFont, serif;
  --font-body: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: var(--font-body);
  color: var(--color-text);
  background: var(--color-bg);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.premium-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
}

@media (max-width: 768px) {
  .premium-container {
    padding: 0 16px;
  }
}
</style>
