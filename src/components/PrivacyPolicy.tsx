import { useEffect, useRef } from 'react'
import { X } from 'lucide-react'

export default function PrivacyPolicy({ onClose }: { onClose: () => void }) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    dialog.showModal()
    closeButtonRef.current?.focus()
    return () => {
      if (dialog.open) dialog.close()
    }
  }, [])

  return (
    <dialog
      className="privacy-dialog"
      ref={dialogRef}
      aria-labelledby="privacy-dialog-title"
      onCancel={(event) => {
        event.preventDefault()
        onClose()
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <header className="privacy-dialog__head">
        <div>
          <span className="eyebrow">CAP Compass</span>
          <h2 id="privacy-dialog-title">Privacy policy</h2>
        </div>
        <button
          ref={closeButtonRef}
          className="privacy-dialog__close"
          type="button"
          aria-label="Close privacy policy"
          onClick={onClose}
        >
          <X size={18} aria-hidden="true" />
        </button>
      </header>
      <iframe
        className="privacy-dialog__frame"
        src="/privacy-policy.html"
        title="CAP Compass privacy policy"
        sandbox=""
      />
    </dialog>
  )
}
