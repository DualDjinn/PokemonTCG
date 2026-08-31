/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        'bg-dark': '#0a0a0f',
        'bg-secondary': '#0f0f1a',
        'neon-cyan': '#00aaff',
        'neon-magenta': '#ff3333',
        'neon-green': '#00cc66',
        'neon-blue': '#0066cc',
        'text-primary': '#ffffff',
        'text-secondary': '#8888aa',
        // PokeShop theme colors
        'bg-panel': '#14132b',
        'bg-panel-2': '#1c1b3d',
        'line': 'rgba(237, 235, 255, 0.08)',
        'dim': '#9c98c7',
        'accent': '#c9ff3d',
        'accent-ink': '#0b0a1f',
        'violet': '#7c4dff',
      },
      fontFamily: {
        'pixel': ['"Press Start 2P"', 'monospace'],
        'share-tech': ['"Share Tech Mono"', 'monospace'],
        'space-mono': ['"Space Mono"', 'monospace'],
        'orbitron': ['Orbitron', 'sans-serif'],
        'rajdhani': ['Rajdhani', 'sans-serif'],
        'inter': ['Inter', 'sans-serif'],
        'jetbrains': ['"JetBrains Mono"', 'monospace'],
      },
      boxShadow: {
        'neon-cyan': '0 0 24px color-mix(in srgb, #00aaff 40%, transparent)',
        'neon-magenta': '0 0 24px color-mix(in srgb, #ff3333 40%, transparent)',
        'shop-glow': '0 0 30px rgba(201, 255, 61, 0.15)',
        'shop-violet': '0 0 30px rgba(124, 77, 255, 0.25)',
      },
      animation: {
        'fade-up': 'fade-up 0.6s cubic-bezier(0.22, 1, 0.36, 1) both',
        'marquee': 'marquee 25s linear infinite',
      },
      keyframes: {
        'fade-up': {
          from: { opacity: '0', transform: 'translateY(20px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        'marquee': {
          from: { transform: 'translateX(0)' },
          to: { transform: 'translateX(-50%)' },
        },
      },
    },
  },
  plugins: [],
}
