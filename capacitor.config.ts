import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.capcompass.app',
  appName: 'CAP Compass',
  webDir: 'dist',
  backgroundColor: '#f4f0e6',
  android: {
    backgroundColor: '#f4f0e6',
  },
  plugins: {
    StatusBar: {
      style: 'LIGHT',
      overlaysWebView: true,
    },
    SystemBars: {
      initialViewportFitValueHint: 'cover',
    },
  },
}

export default config
