export const environment = {
  production: false,
  baseUrl: `${window.location.protocol}//${window.location.hostname}:8080`,
  apiUrl: `${window.location.protocol}//${window.location.hostname}:8080/api`,
  wsUrl: `${window.location.protocol}//${window.location.hostname}:8080/ws`,
  mapLightUrl: 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
  mapDarkUrl: 'https://tiles.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
};
