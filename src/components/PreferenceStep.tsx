import { useMemo, useState, type Dispatch, type SetStateAction } from 'react'
import {
  AlertTriangle,
  ArrowDown,
  ArrowRight,
  ArrowUp,
  CheckCircle2,
  Copy,
  Download,
  ExternalLink,
  FileText,
  GripVertical,
  Info,
  ListOrdered,
  Plus,
  Printer,
  Share2,
  Sparkles,
  Target,
  Trash2,
} from 'lucide-react'
import { CATEGORY_BY_ID, type PreferenceItem, type StudentProfile } from '../data/catalog'
import {
  FIT_META,
  analysePreferences,
  formatPreferenceCsv,
  formatPreferenceText,
  getAllCourseOptions,
} from '../lib/planner'
import {
  copyText,
  isNativeApp,
  sharePreferenceCsv,
  sharePreferenceText,
} from '../lib/native'
import { FitBadge } from './Shared'

interface PreferenceStepProps {
  profile: StudentProfile
  preferences: PreferenceItem[]
  setPreferences: Dispatch<SetStateAction<PreferenceItem[]>>
  onExplore: () => void
  onGuide: () => void
  onAutoBuild: () => void
  onProfile: () => void
  onToast: (message: string) => void
}

export default function PreferenceStep({
  profile,
  preferences,
  setPreferences,
  onExplore,
  onGuide,
  onAutoBuild,
  onProfile,
  onToast,
}: PreferenceStepProps) {
  const [draggingId, setDraggingId] = useState<string | null>(null)
  const [overId, setOverId] = useState<string | null>(null)
  const category = CATEGORY_BY_ID[profile.category]

  const allOptions = useMemo(() => getAllCourseOptions(profile), [profile])
  const optionMap = useMemo(() => new Map(allOptions.map((option) => [option.key, option])), [allOptions])
  const analysis = useMemo(() => analysePreferences(preferences, profile), [preferences, profile])
  const rows = preferences.flatMap((item) => {
    const option = optionMap.get(`${item.collegeId}:${item.branchId}`)
    return option ? [{ item, option }] : []
  })

  const move = (index: number, direction: -1 | 1) => {
    const nextIndex = index + direction
    if (nextIndex < 0 || nextIndex >= preferences.length) return
    setPreferences((current) => {
      const next = [...current]
      const [moved] = next.splice(index, 1)
      if (!moved) return current
      next.splice(nextIndex, 0, moved)
      return next
    })
  }

  const moveBefore = (draggedId: string, targetId: string) => {
    if (draggedId === targetId) return
    setPreferences((current) => {
      const fromIndex = current.findIndex((item) => item.id === draggedId)
      const targetIndex = current.findIndex((item) => item.id === targetId)
      if (fromIndex === -1 || targetIndex === -1) return current
      const next = [...current]
      const [moved] = next.splice(fromIndex, 1)
      if (!moved) return current
      const insertAt = next.findIndex((item) => item.id === targetId)
      next.splice(insertAt === -1 ? next.length : insertAt, 0, moved)
      return next
    })
  }

  const remove = (id: string) => setPreferences((current) => current.filter((item) => item.id !== id))

  const copyList = async () => {
    try {
      await copyText(formatPreferenceText(preferences, profile))
      onToast('Preference list copied to clipboard.')
    } catch {
      onToast(isNativeApp ? 'Could not copy. Use Share instead.' : 'Could not access the clipboard. Download the CSV instead.')
    }
  }

  const shareList = async () => {
    try {
      const text = formatPreferenceText(preferences, profile)
      const shared = await sharePreferenceText(text)
      if (!shared) {
        await copyText(text)
        onToast('Sharing is not available. Preference list copied to clipboard instead.')
      }
    } catch {
      onToast('Could not open the share sheet. Try exporting the CSV instead.')
    }
  }

  const downloadCsv = async () => {
    const csv = formatPreferenceCsv(preferences, profile)
    const safeName = profile.name.trim().replace(/[^a-z0-9]+/gi, '-').replace(/^-|-$/g, '').toLowerCase()
    const filename = `cap-preference-list-${safeName || 'student'}.csv`

    if (isNativeApp) {
      try {
        const shared = await sharePreferenceCsv(filename, csv)
        if (!shared) {
          await copyText(csv)
          onToast('Sharing is not available. CSV contents copied to clipboard instead.')
        }
      } catch {
        onToast('Could not export the CSV on this device.')
      }
      return
    }

    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    onToast('CSV downloaded with blank official-code columns.')
  }

  const balanceTotal = analysis.counts.strong + analysis.counts.good + analysis.counts.stretch + analysis.counts.longshot
  const dateLabel = new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })

  if (preferences.length === 0) {
    return (
      <div className="screen-stack">
        <section className="page-heading">
          <span className="eyebrow">Step 03 · Arrange</span>
          <h1>Your preference order.</h1>
          <p>CAP reads your list from the top. Put the course you want most at No. 1 and work down from there.</p>
        </section>

        <section className="empty-board">
          <div className="empty-board__art" aria-hidden="true">
            <div className="paper-stack paper-stack--back" />
            <div className="paper-stack paper-stack--mid" />
            <div className="paper-stack paper-stack--front">
              <span>01</span>
              <strong>Dream choice</strong>
              <span>02</span>
              <strong>Realistic choice</strong>
              <span>03</span>
              <strong>Strong landing</strong>
            </div>
          </div>
          <div className="empty-board__copy">
            <span className="eyebrow">Nothing saved yet</span>
            <h2>Build a list you would actually accept.</h2>
            <p>
              Add specific college + branch combinations, then move them into your honest order. We’ll check the balance
              and prepare a clean sheet for the official CAP form.
            </p>
            <div className="empty-board__actions">
              <button type="button" className="button button--primary" onClick={onExplore}>
                Explore colleges
                <ArrowRight size={16} aria-hidden="true" />
              </button>
              <button
                type="button"
                className="button button--ghost"
                onClick={profile.percentile === null ? onProfile : onAutoBuild}
              >
                <Sparkles size={16} aria-hidden="true" />
                {profile.percentile === null ? 'Add your percentile' : 'Auto-build a starter list'}
              </button>
            </div>
            <div className="empty-board__steps">
              <span><strong>01</strong> Pick courses</span>
              <span><strong>02</strong> Order honestly</span>
              <span><strong>03</strong> Copy to CAP form</span>
            </div>
          </div>
        </section>

        <section className="order-explainer">
          <article>
            <span className="order-explainer__num">A</span>
            <h3>Aim high without fear</h3>
            <p>A stretch course at No. 1 does not reduce your chance at No. 2. If the top choice is unreachable, CAP moves down.</p>
          </article>
          <article>
            <span className="order-explainer__num">B</span>
            <h3>Keep enough doors open</h3>
            <p>Add a mix of good and strong-fit options. More valid choices do not hurt your higher preferences.</p>
          </article>
          <article>
            <span className="order-explainer__num">C</span>
            <h3>Never “save” a favourite</h3>
            <p>Do not put a college lower because you think it is “too good” for your score. Order by what you would accept.</p>
          </article>
        </section>
      </div>
    )
  }

  return (
    <div className="screen-stack">
      <section className="page-heading page-heading--split">
        <div>
          <span className="eyebrow">Step 03 · Arrange</span>
          <h1>Your preference order.</h1>
          <p>
            {preferences.length} course {preferences.length === 1 ? 'choice' : 'choices'} across {analysis.uniqueColleges}{' '}
            {analysis.uniqueColleges === 1 ? 'college' : 'colleges'} · {category.label}
            {profile.gender === 'female' ? ' · female' : ''}
          </p>
        </div>
        <div className="page-actions">
          <button type="button" className="button button--quiet" onClick={copyList}>
            <Copy size={15} aria-hidden="true" />
            Copy
          </button>
          <button type="button" className="button button--quiet" onClick={() => void downloadCsv()}>
            <Download size={15} aria-hidden="true" />
            CSV
          </button>
          {isNativeApp ? (
            <button type="button" className="button button--secondary" onClick={() => void shareList()}>
              <Share2 size={15} aria-hidden="true" />
              Share
            </button>
          ) : (
            <button type="button" className="button button--secondary" onClick={() => window.print()}>
              <Printer size={15} aria-hidden="true" />
              Print sheet
            </button>
          )}
        </div>
      </section>

      <section className="order-rule">
        <Target size={20} aria-hidden="true" />
        <div>
          <strong>Top of the list is assignment order, not a wishlist.</strong>
          <p>
            CAP checks your No. 1 preference first. If it is reachable, you are allotted there; if not, it moves down. A
            more-preferred course always belongs above a safer one.
          </p>
        </div>
      </section>

      <div className="preference-layout">
        <section className="card preference-card">
          <div className="preference-card__head">
            <div>
              <span className="eyebrow">Your order</span>
              <h2>{preferences.length} course choices</h2>
            </div>
            <button type="button" className="button button--ghost button--small" onClick={onExplore}>
              <Plus size={15} aria-hidden="true" />
              Add choices
            </button>
          </div>
          <p className="drag-hint">
            {isNativeApp
              ? 'Use the arrow buttons to rearrange. This sequence is what you enter into the official form.'
              : 'Drag a row or use the arrow buttons to rearrange. This sequence is what you enter into the official form.'}
          </p>

          <ol className="preference-list">
            {rows.map(({ item, option }, index) => (
              <li
                key={item.id}
                className={`preference-row preference-row--${option.fit ?? 'unknown'}${
                  draggingId === item.id ? ' is-dragging' : ''
                }${overId === item.id ? ' is-over' : ''}`}
                draggable={!isNativeApp}
                onDragStart={(event) => {
                  setDraggingId(item.id)
                  event.dataTransfer.effectAllowed = 'move'
                  event.dataTransfer.setData('text/plain', item.id)
                }}
                onDragOver={(event) => {
                  event.preventDefault()
                  event.dataTransfer.dropEffect = 'move'
                  setOverId(item.id)
                }}
                onDrop={(event) => {
                  event.preventDefault()
                  const dragged = draggingId ?? event.dataTransfer.getData('text/plain')
                  moveBefore(dragged, item.id)
                  setDraggingId(null)
                  setOverId(null)
                }}
                onDragEnd={() => {
                  setDraggingId(null)
                  setOverId(null)
                }}
              >
                <span className="drag-handle" aria-hidden="true">
                  <GripVertical size={17} />
                </span>
                <span className="preference-rank">{String(index + 1).padStart(2, '0')}</span>
                <div className="preference-main">
                  <div className="meta-line">
                    <span className={`type-pill type-pill--${option.college.type.toLowerCase().replace(' ', '-')}`}>
                      {option.college.type}
                    </span>
                    <span>{option.college.city}</span>
                  </div>
                  <h3>
                    {option.college.shortName} <span>· {option.branch.shortName}</span>
                  </h3>
                  <p>
                    {option.college.name} · {option.branch.name}
                  </p>
                </div>
                <div className="preference-cutoff">
                  <span className="preference-cutoff__label">Sample cutoff</span>
                  <strong>{option.cutoff.toFixed(2)}</strong>
                  <FitBadge fit={option.fit} compact />
                </div>
                <div className="preference-actions">
                  <div className="arrow-group">
                    <button
                      type="button"
                      onClick={() => move(index, -1)}
                      disabled={index === 0}
                      aria-label={`Move ${option.college.shortName} ${option.branch.shortName} up`}
                    >
                      <ArrowUp size={15} aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      onClick={() => move(index, 1)}
                      disabled={index === preferences.length - 1}
                      aria-label={`Move ${option.college.shortName} ${option.branch.shortName} down`}
                    >
                      <ArrowDown size={15} aria-hidden="true" />
                    </button>
                  </div>
                  <button
                    type="button"
                    className="remove-button"
                    onClick={() => remove(item.id)}
                    aria-label={`Remove ${option.college.shortName} ${option.branch.shortName} from list`}
                  >
                    <Trash2 size={15} aria-hidden="true" />
                  </button>
                </div>
              </li>
            ))}
          </ol>

          <div className="preference-card__foot">
            <span>
              <ListOrdered size={16} aria-hidden="true" />
              Saved automatically on this device
            </span>
            <button type="button" className="text-link" onClick={onExplore}>
              Add more choices
              <ArrowRight size={14} aria-hidden="true" />
            </button>
          </div>
        </section>

        <aside className="side-stack">
          <section className="card balance-card">
            <div className="section-heading section-heading--compact">
              <div>
                <span className="eyebrow">List health</span>
                <h3>Balance check</h3>
              </div>
              <span className="choice-pill">{preferences.length} choices</span>
            </div>
            {profile.percentile === null ? (
              <div className="balance-empty">
                <Info size={17} aria-hidden="true" />
                <p>Add your percentile to see how the list is balanced.</p>
                <button type="button" className="text-link" onClick={onProfile}>
                  Add score
                </button>
              </div>
            ) : (
              <>
                <div className="balance-bar" aria-hidden="true">
                  {(['stretch', 'good', 'strong', 'longshot'] as const).map((fit) =>
                    analysis.counts[fit] > 0 ? (
                      <span
                        key={fit}
                        className={`balance-bar__segment balance-bar__segment--${fit}`}
                        style={{ width: `${(analysis.counts[fit] / Math.max(balanceTotal, 1)) * 100}%` }}
                      />
                    ) : null,
                  )}
                </div>
                <div className="balance-legend">
                  {(['stretch', 'good', 'strong', 'longshot'] as const).map((fit) => (
                    <div key={fit}>
                      <span className={`legend-dot legend-dot--${fit}`} />
                      <span>{FIT_META[fit].shortLabel}</span>
                      <strong>{analysis.counts[fit]}</strong>
                    </div>
                  ))}
                </div>
              </>
            )}

            <div className="issue-list">
              {analysis.issues.map((issue) => (
                <div className={`issue issue--${issue.tone}`} key={issue.id}>
                  {issue.tone === 'good' ? (
                    <CheckCircle2 size={17} aria-hidden="true" />
                  ) : issue.tone === 'warning' ? (
                    <AlertTriangle size={17} aria-hidden="true" />
                  ) : (
                    <Info size={17} aria-hidden="true" />
                  )}
                  <div>
                    <strong>{issue.title}</strong>
                    <p>{issue.text}</p>
                  </div>
                </div>
              ))}
            </div>
          </section>

          <section className="card order-card">
            <span className="eyebrow">The order rule</span>
            <h3>Prefer it? Put it higher.</h3>
            <ul>
              <li>Put your genuine first choice at No. 1.</li>
              <li>Use stretch options at the top without fear.</li>
              <li>Finish with options you would be happy to join.</li>
            </ul>
            <p className="small-note">There is no benefit to ranking a course lower than your true preference.</p>
          </section>

          <section className="card next-card">
            <span className="next-card__icon">
              <FileText size={20} aria-hidden="true" />
            </span>
            <div>
              <span className="eyebrow">Next step</span>
              <h3>Take this list to the CAP form</h3>
              <p>Run through the personalised document and submission checklist before you lock anything.</p>
              <button type="button" className="button button--dark button--small" onClick={onGuide}>
                Open CAP guide
                <ArrowRight size={14} aria-hidden="true" />
              </button>
            </div>
          </section>

          <a
            className="official-link"
            href="https://cetcell.mahacet.org/"
            target="_blank"
            rel="noreferrer"
          >
            <ExternalLink size={15} aria-hidden="true" />
            Official CET Cell portal
          </a>
        </aside>
      </div>

      <div className="print-sheet" aria-hidden="true">
        <div className="print-sheet__head">
          <div>
            <span className="print-brand">CAP Compass</span>
            <h1>CAP Preference Planning Sheet</h1>
          </div>
          <div className="print-date">{dateLabel}</div>
        </div>
        <div className="print-meta">
          <span><strong>Student</strong> {profile.name || '—'}</span>
          <span><strong>Percentile</strong> {profile.percentile === null ? '—' : profile.percentile.toFixed(2)}</span>
          <span><strong>Category</strong> {category.label}</span>
          <span><strong>Home university</strong> {profile.homeUniversity || '—'}</span>
        </div>
        <table>
          <thead>
            <tr>
              <th>No.</th>
              <th>College</th>
              <th>Branch</th>
              <th>City</th>
              <th>Sample cutoff</th>
              <th>Official institute code</th>
              <th>Official course code</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(({ item, option }, index) => (
              <tr key={item.id}>
                <td>{index + 1}</td>
                <td>
                  <strong>{option.college.shortName}</strong>
                  <br />
                  {option.college.name}
                </td>
                <td>{option.branch.name}</td>
                <td>{option.college.city}</td>
                <td>{option.cutoff.toFixed(2)}</td>
                <td className="blank-cell" />
                <td className="blank-cell" />
              </tr>
            ))}
          </tbody>
        </table>
        <p className="print-disclaimer">
          Planning sheet only. Sample cut-offs are not official CET Cell data. Fill in the official institute and course
          codes from the current CAP portal before submitting.
        </p>
      </div>
    </div>
  )
}
