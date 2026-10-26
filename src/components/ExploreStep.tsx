import { useEffect, useMemo, useState } from 'react'
import {
  ArrowRight,
  Check,
  CheckCircle2,
  ChevronDown,
  Info,
  ListOrdered,
  MapPin,
  Plus,
  RotateCcw,
  Search,
  SlidersHorizontal,
  Sparkles,
} from 'lucide-react'
import {
  BRANCHES,
  CATEGORY_BY_ID,
  CITIES,
  type BranchId,
  type CollegeType,
  type CourseOption,
  type FitLevel,
  type StudentProfile,
} from '../data/catalog'
import { FIT_META, getCourseOptions } from '../lib/planner'
import { FitBadge } from './Shared'

type SortKey = 'competitive' | 'closest' | 'name'

interface ExploreStepProps {
  profile: StudentProfile
  preferenceCount: number
  onAdd: (collegeId: string, branchId: BranchId) => void
  addedKeys: Set<string>
  onAutoBuild: () => void
  onOpenPreferences: () => void
  onEditProfile: () => void
}

const FIT_FILTERS: Array<{ id: 'all' | FitLevel; label: string }> = [
  { id: 'all', label: 'All fits' },
  { id: 'strong', label: 'Strong' },
  { id: 'good', label: 'Good' },
  { id: 'stretch', label: 'Stretch' },
  { id: 'longshot', label: 'Long shot' },
]

function initials(name: string) {
  return name.replace(/[^a-zA-Z]/g, '').slice(0, 2).toUpperCase()
}

function CollegeCard({
  college,
  options,
  addedKeys,
  onAdd,
}: {
  college: CourseOption['college']
  options: CourseOption[]
  addedKeys: Set<string>
  onAdd: (collegeId: string, branchId: BranchId) => void
}) {
  const [expanded, setExpanded] = useState(false)
  const visibleOptions = expanded ? options : options.slice(0, 3)

  return (
    <article className="college-card">
      <div className="college-card__head">
        <div className="college-mark" aria-hidden="true">
          {initials(college.shortName)}
        </div>
        <div className="college-card__identity">
          <div className="meta-line">
            <span className={`type-pill type-pill--${college.type.toLowerCase().replace(' ', '-')}`}>
              {college.type}
            </span>
            <span>
              <MapPin size={13} aria-hidden="true" />
              {college.city}
            </span>
          </div>
          <h3>{college.name}</h3>
          <p>
            {college.shortName} · {options.length} matching {options.length === 1 ? 'branch' : 'branches'}
          </p>
        </div>
      </div>

      <div className="branch-list">
        {visibleOptions.map((option) => {
          const added = addedKeys.has(option.key)
          return (
            <div className="branch-row" key={option.key}>
              <div className="branch-row__name">
                <span className="branch-code">{option.branch.shortName}</span>
                <span>{option.branch.name}</span>
              </div>
              <div className="branch-row__cutoff">
                <span>Sample cutoff</span>
                <strong>{option.cutoff.toFixed(2)}</strong>
              </div>
              <FitBadge fit={option.fit} compact />
              <button
                type="button"
                className={`add-button ${added ? 'is-added' : ''}`}
                onClick={() => onAdd(option.college.id, option.branch.id)}
                disabled={added}
                aria-label={
                  added
                    ? `Added: ${option.branch.name} at ${option.college.shortName} is already in your list`
                    : `Add ${option.branch.name} at ${option.college.shortName} to your list`
                }
              >
                {added ? <Check size={15} aria-hidden="true" /> : <Plus size={15} aria-hidden="true" />}
                {added ? 'Added' : 'Add'}
              </button>
            </div>
          )
        })}
      </div>

      {options.length > 3 ? (
        <button type="button" className="card-more" onClick={() => setExpanded((current) => !current)}>
          {expanded ? 'Show fewer branches' : `Show ${options.length - 3} more branch${options.length - 3 === 1 ? '' : 'es'}`}
          <ChevronDown size={15} className={expanded ? 'is-flipped' : ''} aria-hidden="true" />
        </button>
      ) : null}
    </article>
  )
}

export default function ExploreStep({
  profile,
  preferenceCount,
  onAdd,
  addedKeys,
  onAutoBuild,
  onOpenPreferences,
  onEditProfile,
}: ExploreStepProps) {
  const profileOptions = useMemo(() => getCourseOptions(profile), [profile])
  const [search, setSearch] = useState('')
  const [branchFilter, setBranchFilter] = useState<'all' | BranchId>('all')
  const [cityFilter, setCityFilter] = useState('all')
  const [typeFilter, setTypeFilter] = useState<'all' | CollegeType>('all')
  const [fitFilter, setFitFilter] = useState<'all' | FitLevel>('all')
  const [sort, setSort] = useState<SortKey>('competitive')
  const [visibleCount, setVisibleCount] = useState(12)
  const category = CATEGORY_BY_ID[profile.category]

  useEffect(() => {
    setVisibleCount(12)
  }, [search, branchFilter, cityFilter, typeFilter, fitFilter, sort])

  const baseFiltered = useMemo(() => {
    const term = search.trim().toLowerCase()
    return profileOptions.filter((option) => {
      if (branchFilter !== 'all' && option.branch.id !== branchFilter) return false
      if (cityFilter !== 'all' && option.college.city !== cityFilter) return false
      if (typeFilter !== 'all' && option.college.type !== typeFilter) return false
      if (term) {
        const haystack =
          `${option.college.name} ${option.college.shortName} ${option.college.city} ${option.branch.name} ${option.branch.shortName}`.toLowerCase()
        if (!haystack.includes(term)) return false
      }
      return true
    })
  }, [profileOptions, branchFilter, cityFilter, typeFilter, search])

  const fitCounts = useMemo(() => {
    const counts: Record<FitLevel, number> = { strong: 0, good: 0, stretch: 0, longshot: 0 }
    for (const option of baseFiltered) if (option.fit) counts[option.fit] += 1
    return counts
  }, [baseFiltered])

  const filtered = useMemo(() => {
    if (fitFilter === 'all') return baseFiltered
    return baseFiltered.filter((option) => option.fit === fitFilter)
  }, [baseFiltered, fitFilter])

  const grouped = useMemo(() => {
    const groups = new Map<string, CourseOption[]>()
    for (const option of filtered) {
      const existing = groups.get(option.college.id)
      if (existing) existing.push(option)
      else groups.set(option.college.id, [option])
    }
    return [...groups.values()]
      .map((options) => options.sort((a, b) => b.cutoff - a.cutoff))
      .sort((a, b) => {
        if (sort === 'name') return a[0].college.name.localeCompare(b[0].college.name)
        if (sort === 'closest') {
          const aGap = Math.min(...a.map((option) => Math.abs(option.gap ?? 999)))
          const bGap = Math.min(...b.map((option) => Math.abs(option.gap ?? 999)))
          return aGap - bGap
        }
        return b[0].college.baseCutoff - a[0].college.baseCutoff
      })
  }, [filtered, sort])

  const profileCounts = useMemo(() => {
    const counts: Record<FitLevel, number> = { strong: 0, good: 0, stretch: 0, longshot: 0 }
    for (const option of profileOptions) if (option.fit) counts[option.fit] += 1
    return counts
  }, [profileOptions])

  const hasFilters =
    search !== '' || branchFilter !== 'all' || cityFilter !== 'all' || typeFilter !== 'all' || fitFilter !== 'all'

  const clearFilters = () => {
    setSearch('')
    setBranchFilter('all')
    setCityFilter('all')
    setTypeFilter('all')
    setFitFilter('all')
    setSort('competitive')
  }

  return (
    <div className="screen-stack">
      <section className="page-heading page-heading--split">
        <div>
          <span className="eyebrow">Step 02 · Explore</span>
          <h1>
            Where does{' '}
            <span className="text-accent">
              {profile.percentile === null ? 'your score' : `${profile.percentile.toFixed(2)}%ile`}
            </span>{' '}
            take you?
          </h1>
          <p>
            {profile.percentile === null
              ? 'Set your percentile to compare each course with a sample closing percentile.'
              : `${category.label}${profile.gender === 'female' ? ' · ladies’ quota estimate' : ''} · sample closing percentiles, not official cut-offs.`}
          </p>
          <div className="heading-actions">
            <button type="button" className="text-link" onClick={onEditProfile}>
              Edit profile
            </button>
            {preferenceCount > 0 ? (
              <button type="button" className="text-link" onClick={onOpenPreferences}>
                My list ({preferenceCount})
                <ArrowRight size={14} aria-hidden="true" />
              </button>
            ) : null}
          </div>
        </div>
        {profile.percentile !== null ? (
          <div className="fit-overview" aria-label="Fit overview">
            {(['stretch', 'good', 'strong'] as FitLevel[]).map((fit) => (
              <div className={`fit-stat fit-stat--${fit}`} key={fit}>
                <span>{FIT_META[fit].shortLabel}</span>
                <strong>{profileCounts[fit]}</strong>
              </div>
            ))}
            <div className="fit-stat fit-stat--muted">
              <span>Long shot</span>
              <strong>{profileCounts.longshot}</strong>
            </div>
          </div>
        ) : null}
      </section>

      {preferenceCount === 0 ? (
        <section className="starter-banner">
          <div className="starter-banner__icon">
            <Sparkles size={20} aria-hidden="true" />
          </div>
          <div className="starter-banner__copy">
            <strong>Start with a balanced draft list</strong>
            <span>
              {profile.percentile === null
                ? 'Add your percentile first, then we can build a starter list across stretch, good and strong options.'
                : `We’ll pick across ${profile.branches.length} branch${profile.branches.length === 1 ? '' : 'es'}${
                    profile.cities.length > 0 ? ' and your preferred cities' : ''
                  }, then put the most ambitious choices first.`}
            </span>
          </div>
          <button
            type="button"
            className="button button--dark"
            onClick={profile.percentile === null ? onEditProfile : onAutoBuild}
          >
            {profile.percentile === null ? 'Add percentile' : 'Auto-build my list'}
            <ArrowRight size={16} aria-hidden="true" />
          </button>
        </section>
      ) : (
        <button type="button" className="list-status-bar" onClick={onOpenPreferences}>
          <span>
            <CheckCircle2 size={17} aria-hidden="true" />
            <strong>{preferenceCount}</strong> {preferenceCount === 1 ? 'choice' : 'choices'} in your CAP list
          </span>
          <span>
            Open preference list
            <ArrowRight size={15} aria-hidden="true" />
          </span>
        </button>
      )}

      <section className="card filter-card">
        <div className="filter-top">
          <label className="search-box">
            <Search size={18} aria-hidden="true" />
            <input
              type="search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search college, city or branch"
              aria-label="Search colleges"
            />
            {search ? (
              <button
                type="button"
                className="search-clear"
                onClick={() => setSearch('')}
                aria-label="Clear search"
              >
                ×
              </button>
            ) : null}
          </label>
          <button type="button" className="button button--quiet" onClick={clearFilters} disabled={!hasFilters}>
            <RotateCcw size={15} aria-hidden="true" />
            Reset
          </button>
        </div>

        {profile.percentile !== null ? (
          <div className="fit-tabs" role="group" aria-label="Filter by fit">
            <span className="fit-tabs__label">
              <SlidersHorizontal size={14} aria-hidden="true" />
              Fit
            </span>
            {FIT_FILTERS.map((filter) => {
              const count = filter.id === 'all' ? baseFiltered.length : fitCounts[filter.id]
              return (
                <button
                  key={filter.id}
                  type="button"
                  className={`fit-tab ${fitFilter === filter.id ? 'is-active' : ''}`}
                  aria-pressed={fitFilter === filter.id}
                  onClick={() => setFitFilter(filter.id)}
                >
                  {filter.label}
                  <span>{count}</span>
                </button>
              )
            })}
          </div>
        ) : (
          <div className="filter-score-prompt">
            <Info size={15} aria-hidden="true" />
            Add your percentile to see strong, good and stretch fit labels.
            <button type="button" className="text-link" onClick={onEditProfile}>
              Add score
            </button>
          </div>
        )}

        <div className="filter-grid">
          <label className="compact-field">
            <span>Branch</span>
            <span className="select-wrap select-wrap--small">
              <select value={branchFilter} onChange={(event) => setBranchFilter(event.target.value as 'all' | BranchId)}>
                <option value="all">All my branches</option>
                {BRANCHES.filter((branch) => profile.branches.includes(branch.id)).map((branch) => (
                  <option key={branch.id} value={branch.id}>
                    {branch.shortName} · {branch.name}
                  </option>
                ))}
              </select>
              <ChevronDown size={15} aria-hidden="true" />
            </span>
          </label>

          <label className="compact-field">
            <span>City</span>
            <span className="select-wrap select-wrap--small">
              <select value={cityFilter} onChange={(event) => setCityFilter(event.target.value)}>
                <option value="all">All cities</option>
                {CITIES.map((city) => (
                  <option key={city} value={city}>
                    {city}
                  </option>
                ))}
              </select>
              <ChevronDown size={15} aria-hidden="true" />
            </span>
          </label>

          <label className="compact-field">
            <span>College type</span>
            <span className="select-wrap select-wrap--small">
              <select
                value={typeFilter}
                onChange={(event) => setTypeFilter(event.target.value as 'all' | CollegeType)}
              >
                <option value="all">All types</option>
                <option value="Government">Government</option>
                <option value="Aided">Aided</option>
                <option value="Private">Private</option>
                <option value="State university">State university</option>
              </select>
              <ChevronDown size={15} aria-hidden="true" />
            </span>
          </label>

          <label className="compact-field">
            <span>Sort</span>
            <span className="select-wrap select-wrap--small">
              <select value={sort} onChange={(event) => setSort(event.target.value as SortKey)}>
                <option value="competitive">Most competitive</option>
                <option value="closest">Closest to my score</option>
                <option value="name">College name</option>
              </select>
              <ChevronDown size={15} aria-hidden="true" />
            </span>
          </label>
        </div>

        <div className="filter-foot">
          <span>
            <strong>{grouped.length}</strong> {grouped.length === 1 ? 'college' : 'colleges'} ·{' '}
            <strong>{filtered.length}</strong> course {filtered.length === 1 ? 'option' : 'options'}
          </span>
          <span className="sample-label">
            <Info size={13} aria-hidden="true" />
            Sample cut-offs · verify official data
          </span>
        </div>
      </section>

      {grouped.length === 0 ? (
        <section className="empty-panel">
          <div className="empty-panel__icon">
            <Search size={24} aria-hidden="true" />
          </div>
          <h2>No matching course options</h2>
          <p>
            {hasFilters
              ? 'Try widening your filters or clearing the search. Your selected branches still limit what appears here.'
              : 'Add more branches to your profile to see course options.'}
          </p>
          <div className="empty-panel__actions">
            {hasFilters ? (
              <button type="button" className="button button--secondary" onClick={clearFilters}>
                Clear filters
              </button>
            ) : null}
            <button type="button" className="button button--ghost" onClick={onEditProfile}>
              Edit profile
            </button>
          </div>
        </section>
      ) : (
        <>
          <div className="college-grid">
            {grouped.slice(0, visibleCount).map((options) => (
              <CollegeCard
                key={options[0].college.id}
                college={options[0].college}
                options={options}
                addedKeys={addedKeys}
                onAdd={onAdd}
              />
            ))}
          </div>
          {visibleCount < grouped.length ? (
            <div className="load-more">
              <button type="button" className="button button--secondary" onClick={() => setVisibleCount((count) => count + 12)}>
                Show 12 more colleges
              </button>
            </div>
          ) : null}
        </>
      )}

      <section className="footnote-card">
        <Info size={17} aria-hidden="true" />
        <p>
          <strong>About this data.</strong> Closing percentiles here are sample estimates generated for planning. They
          are not official CET Cell cut-offs and do not account for every seat type, minority quota or round-level
          change. Always use the official cut-off explorer, institute codes and course codes before submitting your CAP
          form.
        </p>
      </section>

      {preferenceCount > 0 ? (
        <div className="floating-list-bar">
          <span>
            <ListOrdered size={17} aria-hidden="true" />
            {preferenceCount} {preferenceCount === 1 ? 'course' : 'courses'} in your list
          </span>
          <button type="button" className="button button--primary button--small" onClick={onOpenPreferences}>
            Review list
            <ArrowRight size={15} aria-hidden="true" />
          </button>
        </div>
      ) : null}
    </div>
  )
}

