import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    // Separa las librerías grandes en chunks propios para mejorar el cacheo del
    // navegador y reducir el bundle inicial (PERF-009).
    rollupOptions: {
      output: {
        manualChunks: {
          'mui': ['@mui/material', '@mui/icons-material'],
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'webcam': ['react-webcam'],
        },
      },
    },
  },
})
