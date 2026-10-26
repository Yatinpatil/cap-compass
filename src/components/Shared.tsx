import type { ReactNode } from 'react'
import type { FitLevel } from '../data/catalog'
import { FIT_META } from '../lib/planner'

export function FitBadge({ fit, compact = false }: { fit: FitLevel | null; compact?: boolean }) {
  if (!fit) {
    return <span className="fit-badge fit-badge--none">Score needed</span>
  }
  const meta = FIT_META[fit]
  return (
    <span
      className={`fit-badge fit-badge--${meta.tone}${compact ? ' fit-badge--compact' : ''}`}
      title={meta.description}
    >
      <span className="fit-badge__dot" />
      {compact ? meta.shortLabel : meta.label}
    </span>
  )
}

export function PercentileRing({
  value,
  size = 124,
  label = 'percentile',
}: {
  value: number | null
  size?: number
  label?: string
}) {
  const normalized = value === null ? 0 : Math.min(Math.max(value, 0), 100)
  return (
    <div
      className="percentile-ring"
      style={{
        width: size,
        height: size,
        background: `conic-gradient(var(--accent) ${normalized * 3.6}deg, var(--ring-track) 0deg)`,
      }}
      role="img"
      aria-label={value === null ? `No ${label} set` : `${value.toFixed(2)} ${label}`}
    >
      <div className="percentile-ring__inner">
        <strong>{value === null ? '—' : value.toFixed(2)}</strong>
        <span>{label}</span>
      </div>
    </div>
  )
}

export function InlineNotice({
  tone = 'info',
  title,
  children,
}: {
  tone?: 'info' | 'warning' | 'good'
  title?: string
  children: ReactNode
}) {
  return (
    <div className={`inline-notice inline-notice--${tone}`}>
      <span className="inline-notice__mark" aria-hidden="true">
        {tone === 'warning' ? '!' : tone === 'good' ? '✓' : 'i'}
      </span>
      <div>
        {title ? <strong>{title}</strong> : null}
        <div>{children}</div>
      </div>
    </div>
  )
}
