import { useState, type FormEvent } from 'react'
import { ArrowRight, Check, ChevronDown, Info, ListOrdered, MapPin, ShieldCheck } from 'lucide-react'
import {
  BRANCHES,
  CATEGORIES,
  CATEGORY_BY_ID,
  CITIES,
  HOME_UNIVERSITIES,
  type BranchId,
  type GenderId,
  type StudentProfile,
} from '../data/catalog'
import { PercentileRing } from './Shared'

interface ProfileStepProps {
  profile: StudentProfile
  setProfile: (profile: StudentProfile) => void
  onSubmit: () => void
  preferenceCount: number
  onOpenPreferences: () => void
}

export default function ProfileStep({
  profile,
  setProfile,
  onSubmit,
  preferenceCount,
  onOpenPreferences,
}: ProfileStepProps) {
  const [attempted, setAttempted] = useState(false)
  const percentileValid = profile.percentile !== null && profile.percentile >= 0 && profile.percentile <= 100
  const branchesValid = profile.branches.length > 0
  const canContinue = percentileValid && branchesValid
  const selectedCategory = CATEGORY_BY_ID[profile.category]

  const patch = (next: Partial<StudentProfile>) => setProfile({ ...profile, ...next })

  const toggleBranch = (branchId: BranchId) => {
    const exists = profile.branches.includes(branchId)
    patch({
      branches: exists ? profile.branches.filter((id) => id !== branchId) : [...profile.branches, branchId],
    })
  }

  const toggleCity = (city: string) => {
    const exists = profile.cities.includes(city)
    patch({ cities: exists ? profile.cities.filter((item) => item !== city) : [...profile.cities, city] })
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setAttempted(true)
    if (!canContinue) return
    onSubmit()
  }

  return (
    <div className="screen-stack">
      <section className="hero">
        <div className="hero__copy">
          <span className="eyebrow eyebrow--light">MHT-CET · CAP preference planner</span>
          <h1>
            Your percentile opens doors.
            <br />
            <span>Your order decides which one.</span>
          </h1>
          <p>
            Compare sample closing cut-offs, build a balanced course list, and walk into the official CAP form
            knowing exactly what goes where.
          </p>
          <div className="hero__chips">
            <span>Category-aware</span>
            <span>List-first, not college-first</span>
            <span>Saved on your device</span>
          </div>
        </div>
        <div className="hero__preview" aria-label="Example CAP preference list">
          <div className="preview-head">
            <span>How a good list feels</span>
            <span className="preview-pill">Example</span>
          </div>
          <div className="preview-row preview-row--stretch">
            <span className="preview-rank">01</span>
            <div>
              <strong>COEP · Computer</strong>
              <span>Aim high, but keep going</span>
            </div>
            <span className="preview-fit">Stretch</span>
          </div>
          <div className="preview-row preview-row--good">
            <span className="preview-rank">02</span>
            <div>
              <strong>PICT · IT</strong>
              <span>A realistic top choice</span>
            </div>
            <span className="preview-fit">Good</span>
          </div>
          <div className="preview-row preview-row--strong">
            <span className="preview-rank">03</span>
            <div>
              <strong>PCCOE · Computer</strong>
              <span>A strong landing option</span>
            </div>
            <span className="preview-fit">Strong</span>
          </div>
          <div className="preview-note">Your real list can be 12+ options long.</div>
        </div>
      </section>

      <div className="profile-layout">
        <form className="card profile-form" onSubmit={handleSubmit} noValidate>
          <div className="section-heading">
            <div>
              <span className="eyebrow">Step 01 · Your profile</span>
              <h2>Tell us what matters</h2>
              <p>We use these details to estimate fit and personalise your CAP checklist.</p>
            </div>
            <span className="local-chip">
              <ShieldCheck size={14} aria-hidden="true" />
              Device only
            </span>
          </div>

          <div className="field-row field-row--two">
            <label className="field">
              <span className="field__label">Your name <em>optional</em></span>
              <input
                type="text"
                value={profile.name}
                onChange={(event) => patch({ name: event.target.value })}
                placeholder="e.g. Aarav"
                autoComplete="name"
              />
            </label>

            <label className={`field field--percentile ${attempted && !percentileValid ? 'field--error' : ''}`}>
              <span className="field__label">MHT-CET percentile</span>
              <span className="percentile-input">
                <input
                  type="number"
                  min="0"
                  max="100"
                  step="0.01"
                  inputMode="decimal"
                  value={profile.percentile ?? ''}
                  onChange={(event) => {
                    const next = event.target.value
                    patch({ percentile: next === '' ? null : Number(next) })
                  }}
                  placeholder="92.50"
                  aria-invalid={attempted && !percentileValid}
                  aria-describedby={attempted && !percentileValid ? 'percentile-error' : undefined}
                />
                <span>%ile</span>
              </span>
              {attempted && !percentileValid ? (
                <span className="field__error" id="percentile-error" role="alert">
                  Enter a percentile between 0 and 100.
                </span>
              ) : (
                <span className="field__hint">Use the percentile from your official scorecard.</span>
              )}
            </label>
          </div>

          <div className="field-row field-row--two">
            <label className="field">
              <span className="field__label">Category</span>
              <span className="select-wrap">
                <select
                  value={profile.category}
                  onChange={(event) => patch({ category: event.target.value as StudentProfile['category'] })}
                >
                  {CATEGORIES.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.label}
                    </option>
                  ))}
                </select>
                <ChevronDown size={17} aria-hidden="true" />
              </span>
              <span className="field__hint">{selectedCategory.description}. Claim only what you can document.</span>
            </label>

            <label className="field">
              <span className="field__label">Home university <em>optional</em></span>
              <span className="select-wrap">
                <select
                  value={profile.homeUniversity}
                  onChange={(event) => patch({ homeUniversity: event.target.value })}
                >
                  <option value="">Select home university</option>
                  {HOME_UNIVERSITIES.map((university) => (
                    <option key={university} value={university}>
                      {university}
                    </option>
                  ))}
                </select>
                <ChevronDown size={17} aria-hidden="true" />
              </span>
              <span className="field__hint">Used in your printable checklist, not to alter cut-off estimates.</span>
            </label>
          </div>

          <fieldset className="field field--fieldset">
            <legend className="field__label">Ladies’ quota eligibility</legend>
            <div className="segmented" role="group" aria-label="Gender for ladies quota estimate">
              {(
                [
                  { id: 'female', label: 'Female candidate', note: 'Estimate includes ladies’ quota' },
                  { id: 'other', label: 'Male / other', note: 'Gender-neutral seats only' },
                ] as { id: GenderId; label: string; note: string }[]
              ).map((option) => (
                <button
                  key={option.id}
                  type="button"
                  className={`segmented__option ${profile.gender === option.id ? 'is-active' : ''}`}
                  aria-pressed={profile.gender === option.id}
                  onClick={() => patch({ gender: option.id })}
                >
                  <span>{option.label}</span>
                  <small>{option.note}</small>
                </button>
              ))}
            </div>
          </fieldset>

          <fieldset
            className={`field field--fieldset ${attempted && !branchesValid ? 'field--error' : ''}`}
            aria-describedby={attempted && !branchesValid ? 'branch-error' : undefined}
          >
            <legend className="field__label">
              Branches you want
              <span className="field__inline-count">{profile.branches.length} selected</span>
            </legend>
            <div className="choice-grid choice-grid--branches">
              {BRANCHES.map((branch) => {
                const active = profile.branches.includes(branch.id)
                return (
                  <button
                    key={branch.id}
                    type="button"
                    className={`choice-chip ${active ? 'is-active' : ''}`}
                    aria-pressed={active}
                    onClick={() => toggleBranch(branch.id)}
                  >
                    <span className="choice-chip__check">
                      {active ? <Check size={12} aria-hidden="true" /> : null}
                    </span>
                    <span>
                      <strong>{branch.shortName}</strong>
                      <small>{branch.name}</small>
                    </span>
                  </button>
                )
              })}
            </div>
            {attempted && !branchesValid ? (
              <span className="field__error" id="branch-error" role="alert">
                Choose at least one branch.
              </span>
            ) : null}
          </fieldset>

          <fieldset className="field field--fieldset">
            <legend className="field__label">
              Preferred cities <em>optional</em>
            </legend>
            <div className="city-chips">
              {CITIES.map((city) => {
                const active = profile.cities.includes(city)
                return (
                  <button
                    key={city}
                    type="button"
                    className={`city-chip ${active ? 'is-active' : ''}`}
                    aria-pressed={active}
                    onClick={() => toggleCity(city)}
                  >
                    <MapPin size={13} aria-hidden="true" />
                    {city}
                  </button>
                )
              })}
            </div>
            <span className="field__hint">
              Leave blank to consider all of Maharashtra. Preferred cities are prioritised when auto-building.
            </span>
          </fieldset>

          <div className="form-footer">
            <div className="form-footer__note">
              <Info size={16} aria-hidden="true" />
              <span>Sample cut-offs are directional, not an admission guarantee.</span>
            </div>
            <div className="form-footer__actions">
              {preferenceCount > 0 ? (
                <button type="button" className="button button--ghost" onClick={onOpenPreferences}>
                  <ListOrdered size={16} aria-hidden="true" />
                  My list ({preferenceCount})
                </button>
              ) : null}
              <button type="submit" className="button button--primary">
                {profile.configured ? 'Update my matches' : 'Show my matches'}
                <ArrowRight size={17} aria-hidden="true" />
              </button>
            </div>
          </div>
        </form>

        <aside className="side-stack">
          <section className="card snapshot-card">
            <div className="snapshot-card__top">
              <PercentileRing value={profile.percentile} />
              <div>
                <span className="eyebrow">Your starting point</span>
                <h3>{profile.name ? `Hi, ${profile.name}.` : 'Your score, in context.'}</h3>
                <p>
                  {profile.percentile === null
                    ? 'Add your percentile to see how sample cut-offs compare.'
                    : `${selectedCategory.label}${profile.gender === 'female' ? ' · Ladies’ quota estimate on' : ''}`}
                </p>
              </div>
            </div>
            <div className="snapshot-facts">
              <div>
                <span>Branches</span>
                <strong>{profile.branches.length || '—'}</strong>
              </div>
              <div>
                <span>Cities</span>
                <strong>{profile.cities.length || 'All'}</strong>
              </div>
              <div>
                <span>Saved list</span>
                <strong>{preferenceCount}</strong>
              </div>
            </div>
          </section>

          <section className="card method-card">
            <span className="eyebrow">How fit is estimated</span>
            <ol className="method-list">
              <li>
                <span>01</span>
                <div>
                  <strong>Compare your percentile</strong>
                  <p>We measure the gap against a sample closing percentile for your category and quota.</p>
                </div>
              </li>
              <li>
                <span>02</span>
                <div>
                  <strong>Balance the range</strong>
                  <p>Stretch, good and strong-fit choices each play a job in the final list.</p>
                </div>
              </li>
              <li>
                <span>03</span>
                <div>
                  <strong>Order by true preference</strong>
                  <p>A CAP list is read top-down. Never place a less-wanted course above a favourite.</p>
                </div>
              </li>
            </ol>
          </section>

          <section className="card caveat-card">
            <Info size={18} aria-hidden="true" />
            <div>
              <strong>Built for planning, not prediction.</strong>
              <p>
                Cut-offs move by year, round, seat type and category. Verify every code and closing percentile on the
                official CET Cell portal before you submit.
              </p>
            </div>
          </section>
        </aside>
      </div>
    </div>
  )
}
