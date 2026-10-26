import { Capacitor } from '@capacitor/core'
import { App as CapacitorApp } from '@capacitor/app'
import { Browser } from '@capacitor/browser'
import { Clipboard } from '@capacitor/clipboard'
import { Directory, Filesystem } from '@capacitor/filesystem'
import { Share } from '@capacitor/share'
import { StatusBar, Style } from '@capacitor/status-bar'

export const isNativeApp = Capacitor.isNativePlatform()

export function setupNativeShell() {
  if (!isNativeApp) return
  document.documentElement.classList.add('is-native')
  void StatusBar.setStyle({ style: Style.Light }).catch(() => undefined)
}

export async function copyText(text: string) {
  await Clipboard.write({ string: text })
}

export async function sharePreferenceText(text: string) {
  const availability = await Share.canShare()
  if (!availability.value) return false
  await Share.share({
    title: 'CAP Compass preference list',
    text,
    dialogTitle: 'Share preference list',
  })
  return true
}

const encodeUtf8Base64 = (value: string) => {
  const bytes = new TextEncoder().encode(value)
  let binary = ''
  const chunkSize = 0x8000
  for (let index = 0; index < bytes.length; index += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(index, index + chunkSize))
  }
  return btoa(binary)
}

export async function sharePreferenceCsv(filename: string, csv: string) {
  const availability = await Share.canShare()
  if (!availability.value) return false
  const file = await Filesystem.writeFile({
    path: filename,
    data: encodeUtf8Base64(`\uFEFF${csv}`),
    directory: Directory.Cache,
    recursive: true,
  })
  await Share.share({
    title: 'CAP Compass preference list',
    text: 'CAP Compass preference list. Official institute and course codes are blank for manual entry.',
    files: [file.uri],
    dialogTitle: 'Export preference list',
  })
  return true
}

export const interceptExternalLinks = () => {
  if (!isNativeApp) return () => undefined

  const handleClick = (event: MouseEvent) => {
    if (!(event.target instanceof Element)) return
    const anchor = event.target.closest<HTMLAnchorElement>('a[target="_blank"]')
    if (!anchor) return
    event.preventDefault()
    void Browser.open({ url: anchor.href })
  }

  document.addEventListener('click', handleClick)
  return () => document.removeEventListener('click', handleClick)
}

export const addBackButtonListener = (onBack: () => void) => {
  if (!isNativeApp) return () => undefined

  let active = true
  let removeListener: (() => void) | undefined

  void CapacitorApp.addListener('backButton', onBack).then((listener) => {
    if (active) {
      removeListener = () => {
        void listener.remove()
      }
    } else {
      void listener.remove()
    }
  })

  return () => {
    active = false
    removeListener?.()
  }
}

export const exitNativeApp = async () => {
  if (isNativeApp) await CapacitorApp.exitApp()
}
