const TARGET = 'http://localhost:8080';

const proxyConfig = {
  '/api/**': {
    target: TARGET,
    secure: false,
    changeOrigin: true,
    logLevel: 'info',
  },
  '/cards/**': {
    target: TARGET,
    secure: false,
    changeOrigin: true,
    logLevel: 'info',
  },
  '/full-art-pokemon/**': {
    target: TARGET,
    secure: false,
    changeOrigin: true,
    logLevel: 'info',
  },
  '/media/**': {
    target: TARGET,
    secure: false,
    changeOrigin: true,
    logLevel: 'info',
    bypass: function (req) {
      // Always proxy media requests, never serve locally
      return null;
    },
  },
  '/ws/**': {
    target: TARGET,
    secure: false,
    ws: true,
    logLevel: 'info',
  },
};

module.exports = proxyConfig;
