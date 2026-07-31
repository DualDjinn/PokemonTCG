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
      },
      fontFamily: {
        'pixel': ['"Press Start 2P"', 'monospace'],
        'share-tech': ['"Share Tech Mono"', 'monospace'],
        'space-mono': ['"Space Mono"', 'monospace'],
        'orbitron': ['Orbitron', 'sans-serif'],
      },
      boxShadow: {
        'neon-cyan': '0 0 24px color-mix(in srgb, #00aaff 40%, transparent)',
        'neon-magenta': '0 0 24px color-mix(in srgb, #ff3333 40%, transparent)',
      },
    },
  },
  plugins: [],
}
