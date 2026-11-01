import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

export default defineConfig({
  plugins: [svelte()],
  server: {
    port: 5173,
    proxy: {
      '/extract': 'http://localhost:9090',
      '/terms': 'http://localhost:9090',
      '/privacy': 'http://localhost:9090'
    }
  }
});
