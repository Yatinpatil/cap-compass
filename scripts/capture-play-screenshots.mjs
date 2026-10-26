import { spawn } from 'node:child_process'
import { access, mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const chromeCandidates = [
  process.env.CHROME_PATH,
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
].filter(Boolean)

let chromePath
for (const candidate of chromeCandidates) {
  try {
    await access(candidate)
    chromePath = candidate
    break
  } catch {
    // Try the next standard installation path.
  }
}
if (!chromePath) throw new Error('Chrome was not found. Set CHROME_PATH to the installed Chrome executable.')

const tempRoot = await mkdtemp(path.join(os.tmpdir(), 'cap-compass-store-capture-'))
const profilePath = path.join(tempRoot, 'chrome-profile')
const vitePath = path.join(projectRoot, 'node_modules', 'vite', 'bin', 'vite.js')
const vitePort = 5181
const viteUrl = `http://127.0.0.1:${vitePort}`
const screenshotDir = path.join(projectRoot, 'play-store', 'screenshots', 'phone')
let vite
let chrome
let socket
const pending = new Map()
let nextMessageId = 0

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function waitFor(check, message, timeoutMs = 30000) {
  const endAt = Date.now() + timeoutMs
  while (Date.now() < endAt) {
    try {
      if (await check()) return
    } catch {
      // The local server or page may still be starting.
    }
    await delay(250)
  }
  throw new Error(message)
}

function send(method, params = {}) {
  const id = ++nextMessageId
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      pending.delete(id)
      reject(new Error(`Chrome did not answer ${method}.`))
    }, 10000)
    pending.set(id, {
      resolve: (value) => { clearTimeout(timeout); resolve(value) },
      reject: (error) => { clearTimeout(timeout); reject(error) },
    })
    socket.send(JSON.stringify({ id, method, params }))
  })
}

async function fetchJson(url) {
  const response = await fetch(url, { signal: AbortSignal.timeout(5000) })
  if (!response.ok) throw new Error(`Local capture endpoint returned ${response.status}: ${url}`)
  return response.json()
}

function handleMessage(event) {
  const message = JSON.parse(event.data)
  if (!message.id) return
  const request = pending.get(message.id)
  if (!request) return
  pending.delete(message.id)
  if (message.error) request.reject(new Error(message.error.message))
  else request.resolve(message.result)
}

async function evaluate(expression) {
  const result = await send('Runtime.evaluate', {
    expression,
    awaitPromise: true,
    returnByValue: true,
  })
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text)
  return result.result.value
}

async function saveScreenshot(filename) {
  const result = await send('Page.captureScreenshot', {
    format: 'png',
    fromSurface: true,
    captureBeyondViewport: false,
  })
  await writeFile(path.join(screenshotDir, filename), Buffer.from(result.data, 'base64'))
}

try {
  await mkdir(screenshotDir, { recursive: true })
  vite = spawn(process.execPath, [vitePath, '--host', '127.0.0.1', '--port', String(vitePort), '--strictPort'], {
    cwd: projectRoot,
    stdio: 'ignore',
    windowsHide: true,
  })
  await waitFor(async () => (await fetch(viteUrl, { signal: AbortSignal.timeout(1000) })).ok, 'Vite did not start for screenshot capture.')

  chrome = spawn(chromePath, [
    '--headless=new',
    '--disable-gpu',
    '--hide-scrollbars',
    '--disable-extensions',
    '--disable-background-networking',
    '--disable-crash-reporter',
    '--no-first-run',
    '--no-default-browser-check',
    `--user-data-dir=${profilePath}`,
    '--remote-debugging-port=0',
    '--remote-allow-origins=*',
    '--window-size=540,960',
    'about:blank',
  ], { stdio: 'ignore', windowsHide: true })

  const activePortFile = path.join(profilePath, 'DevToolsActivePort')
  await waitFor(async () => {
    try {
      await access(activePortFile)
      return true
    } catch {
      return false
    }
  }, 'Chrome did not expose its local screenshot session.')
  const [port] = (await readFile(activePortFile, 'utf8')).split(/\r?\n/)
  const debugOrigin = `http://127.0.0.1:${port}`
  const targets = await fetchJson(`${debugOrigin}/json/list`)
  const pageTarget = targets.find((target) => target.type === 'page')
  if (!pageTarget?.webSocketDebuggerUrl) throw new Error('Chrome did not return a page target for screenshots.')

  socket = new WebSocket(pageTarget.webSocketDebuggerUrl)
  socket.addEventListener('message', handleMessage)
  await new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error('Timed out connecting to Chrome for screenshot capture.')), 10000)
    socket.addEventListener('open', resolve, { once: true })
    socket.addEventListener('error', reject, { once: true })
    socket.addEventListener('open', () => clearTimeout(timeout), { once: true })
  })

  await send('Page.enable')
  await send('Runtime.enable')
  await send('Emulation.setDeviceMetricsOverride', {
    width: 540,
    height: 960,
    deviceScaleFactor: 2,
    mobile: true,
    screenWidth: 540,
    screenHeight: 960,
  })
  await send('Page.navigate', { url: viteUrl })
  await waitFor(
    async () => evaluate("document.readyState === 'complete' && document.querySelector('.hero h1') !== null"),
    'The CAP Compass phone view did not finish loading.',
  )
  await evaluate('document.fonts.ready.then(() => true)')
  await delay(350)
  await saveScreenshot('01-profile.png')

  await evaluate("document.querySelector('.stepbar button:nth-child(2)').click(); true")
  await waitFor(async () => evaluate("document.querySelector('.filter-card') !== null"), 'Explore did not open.')
  await delay(350)
  await saveScreenshot('02-explore.png')

  await evaluate("document.querySelector('.stepbar button:nth-child(3)').click(); true")
  await waitFor(async () => evaluate("document.body.innerText.includes('Your preference order.')"), 'The preference screen did not open.')
  await delay(350)
  await saveScreenshot('03-preferences.png')

  await evaluate("document.querySelector('.stepbar button:nth-child(4)').click(); true")
  await waitFor(async () => evaluate("document.body.innerText.includes('CAP form, without the guesswork.')"), 'The CAP guide did not open.')
  await delay(350)
  await saveScreenshot('04-guide.png')

  console.log(`Captured four phone screenshots in ${screenshotDir}`)
} finally {
  for (const request of pending.values()) request.reject(new Error('Screenshot session closed.'))
  if (socket?.readyState === WebSocket.OPEN) {
    try { await send('Browser.close') } catch { /* The browser may close the socket before replying. */ }
  }
  socket?.close()
  if (chrome && chrome.exitCode === null) {
    await Promise.race([
      new Promise((resolve) => chrome.once('exit', resolve)),
      delay(3000),
    ])
  }
  if (chrome && chrome.exitCode === null) chrome.kill()
  if (vite && vite.exitCode === null) vite.kill()
  try {
    await rm(tempRoot, { recursive: true, force: true })
  } catch {
    console.warn('Chrome kept a temporary profile file open; it can be removed after Chrome exits:', tempRoot)
  }
}
