import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import { setupNativeShell } from './lib/native'
import './styles.css'

setupNativeShell()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
