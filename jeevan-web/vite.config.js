import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Read .env from the monorepo root so the whole stack shares one .env.
  // Only VITE_*-prefixed vars are exposed to the client bundle.
  envDir: '..',
  server: {
    host: true,
    port: 5173,
  },
})
