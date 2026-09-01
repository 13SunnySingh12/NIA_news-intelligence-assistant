import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// envDir points to the repo root so all services share ONE .env file.
// Vite still only exposes VITE_*-prefixed variables to the browser bundle;
// backend secrets in the same file are never sent to the client.
export default defineConfig({
  plugins: [react()],
  envDir: '..',
  server: {
    port: 5173,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
    include: ['src/**/*.test.{js,jsx}'],
  },
});
