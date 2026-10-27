import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: { DEFAULT: '#E50821', dark: '#C80A20' },
        navy: { DEFAULT: '#1D2C3A', light: '#2B3D4D' },
        ink: '#1A2430',
        muted: '#8A96A2',
        canvas: '#F3F6FA',
      },
      fontFamily: { sans: ['Roboto', 'ui-sans-serif', 'system-ui', 'sans-serif'] },
      boxShadow: { panel: '0 8px 28px rgba(29, 44, 58, 0.07)' },
    },
  },
  plugins: [],
} satisfies Config
