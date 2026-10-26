import {
  BRANCH_BY_ID,
  CATEGORY_BY_ID,
  COLLEGE_BY_ID,
  COLLEGES,
  type BranchId,
  type College,
  type CourseOption,
  type FitLevel,
  type PreferenceItem,
  type StudentProfile,
} from '../data/catalog'

const FIT_ORDER: Record<FitLevel, number> = {
  stretch: 0,
  good: 1,
  strong: 2,
  longshot: 3,
}

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max)

const round = (value: number, digits = 2) => {
  const factor = 10 ** digits
  return Math.round(value * factor) / factor
}

export const FIT_META: Record<
  FitLevel,
  { label: string; shortLabel: string; description: string; tone: string }
> = {
  strong: {
    label: 'Strong fit',
    shortLabel: 'Strong',
    description: 'Your percentile is comfortably above the sample closing percentile.',
    tone: 'strong',
  },
  good: {
    label: 'Good fit',
    shortLabel: 'Good',
    description: 'Your percentile is close to the sample closing percentile.',
    tone: 'good',
  },
  stretch: {
    label: 'Stretch',
    shortLabel: 'Stretch',
    description: 'A useful aspirational choice, but do not rely on it alone.',
    tone: 'stretch',
  },
  longshot: {
    label: 'Long shot',
    shortLabel: 'Long shot',
    description: 'Your percentile is well below the sample closing percentile.',
    tone: 'longshot',
  },
}

export function calculateCutoff(college: College, branchId: BranchId, profile: StudentProfile): number {
  const branch = BRANCH_BY_ID[branchId]
  const category = CATEGORY_BY_ID[profile.category]
  const ladiesQuotaAdjustment = profile.gender === 'female' ? 0.6 : 0
  const raw = college.baseCutoff + branch.demand - category.cutoffAdvantage - ladiesQuotaAdjustment
  return round(clamp(raw, 45, 99.94))
}

export function getFitLevel(percentile: number | null, cutoff: number): FitLevel | null {
  if (percentile === null || Number.isNaN(percentile)) return null
  const gap = percentile - cutoff
  if (gap >= 2.5) return 'strong'
  if (gap >= -0.5) return 'good'
  if (gap >= -3.5) return 'stretch'
  return 'longshot'
}

export function getAllCourseOptions(profile: StudentProfile): CourseOption[] {
  return COLLEGES.flatMap((college) =>
    college.branches.map((branchId) => {
      const cutoff = calculateCutoff(college, branchId, profile)
      const gap = profile.percentile === null ? null : round(profile.percentile - cutoff)
      return {
        key: `${college.id}:${branchId}`,
        college,
        branch: BRANCH_BY_ID[branchId],
        cutoff,
        gap,
        fit: getFitLevel(profile.percentile, cutoff),
      }
    }),
  )
}

export function getCourseOptions(profile: StudentProfile): CourseOption[] {
  if (profile.branches.length === 0) return getAllCourseOptions(profile)
  return getAllCourseOptions(profile).filter((option) => profile.branches.includes(option.branch.id))
}

export function buildStarterList(profile: StudentProfile, options: CourseOption[]): string[] {
  if (profile.percentile === null || options.length === 0) return []

  const cityFiltered =
    profile.cities.length > 0 ? options.filter((option) => profile.cities.includes(option.college.city)) : options

  const bestPerCollege = new Map<string, CourseOption>()
  for (const option of cityFiltered) {
    const existing = bestPerCollege.get(option.college.id)
    if (!existing) {
      bestPerCollege.set(option.college.id, option)
      continue
    }
    const optionDistance = option.fit === 'longshot' ? 1000 : Math.abs(option.gap ?? 999)
    const existingDistance = existing.fit === 'longshot' ? 1000 : Math.abs(existing.gap ?? 999)
    if (optionDistance < existingDistance) bestPerCollege.set(option.college.id, option)
  }

  const distinctOptions = [...bestPerCollege.values()]
  const candidates = distinctOptions.filter((option) => option.fit !== 'longshot')
  const stretch = candidates
    .filter((option) => option.fit === 'stretch')
    .sort((a, b) => b.college.baseCutoff - a.college.baseCutoff)
    .slice(0, 3)
  const good = candidates
    .filter((option) => option.fit === 'good')
    .sort((a, b) => b.college.baseCutoff - a.college.baseCutoff)
    .slice(0, 5)
  const strong = candidates
    .filter((option) => option.fit === 'strong')
    .sort((a, b) => b.college.baseCutoff - a.college.baseCutoff)
    .slice(0, 4)

  const selected = [...stretch, ...good, ...strong]
  const selectedIds = new Set(selected.map((option) => option.key))
  const filler = [...candidates, ...distinctOptions]
    .filter((option) => !selectedIds.has(option.key))
    .sort((a, b) => b.cutoff - a.cutoff)

  for (const option of filler) {
    if (selected.length >= 12) break
    if (selectedIds.has(option.key)) continue
    selected.push(option)
    selectedIds.add(option.key)
  }

  return selected
    .sort((a, b) => {
      const fitDifference = FIT_ORDER[a.fit ?? 'longshot'] - FIT_ORDER[b.fit ?? 'longshot']
      return fitDifference !== 0 ? fitDifference : b.cutoff - a.cutoff
    })
    .slice(0, 12)
    .map((option) => option.key)
}

export interface PreferenceIssue {
  id: string
  tone: 'warning' | 'info' | 'good'
  title: string
  text: string
}

export interface PreferenceAnalysis {
  total: number
  uniqueColleges: number
  counts: Record<FitLevel, number>
  issues: PreferenceIssue[]
}

export function analysePreferences(items: PreferenceItem[], profile: StudentProfile): PreferenceAnalysis {
  const options = getAllCourseOptions(profile)
  const optionMap = new Map(options.map((option) => [option.key, option]))
  const resolved = items.flatMap((item) => {
    const option = optionMap.get(`${item.collegeId}:${item.branchId}`)
    return option ? [option] : []
  })

  const counts: Record<FitLevel, number> = { strong: 0, good: 0, stretch: 0, longshot: 0 }
  for (const option of resolved) {
    if (option.fit) counts[option.fit] += 1
  }
  const uniqueColleges = new Set(resolved.map((option) => option.college.id)).size
  const issues: PreferenceIssue[] = []

  if (resolved.length > 0 && resolved.length < 10) {
    issues.push({
      id: 'length',
      tone: 'warning',
      title: 'Add more course options',
      text: 'A practical CAP list usually has 12 or more course choices. More valid options do not hurt your first preference.',
    })
  }

  if (resolved.length > 0 && counts.strong === 0) {
    issues.push({
      id: 'safe',
      tone: 'warning',
      title: 'No safer option yet',
      text: 'Add at least two or three strong-fit courses below your preferred choices so the list still works if cut-offs move.',
    })
  } else if (resolved.length > 0 && counts.strong < 2) {
    issues.push({
      id: 'safe-more',
      tone: 'info',
      title: 'Consider one more safer choice',
      text: 'Two or more strong-fit options give your list a sturdier base.',
    })
  }

  if (resolved.length >= 8 && counts.stretch === 0) {
    issues.push({
      id: 'reach',
      tone: 'info',
      title: 'Room for a dream choice',
      text: 'You do not need to play it safe at the top. Add a stretch course first if you would genuinely take it.',
    })
  }

  if (resolved.length >= 8 && counts.longshot > Math.max(2, Math.floor(resolved.length / 3))) {
    issues.push({
      id: 'longshot',
      tone: 'warning',
      title: 'Too many long shots',
      text: 'Long-shot courses can sit at the top, but make sure the middle and bottom of the list have realistic choices.',
    })
  }

  if (resolved.length >= 6 && uniqueColleges < 4) {
    issues.push({
      id: 'variety',
      tone: 'info',
      title: 'Broaden the college mix',
      text: 'Several branches from the same college is fine, but adding another college reduces the risk of a single cutoff jump affecting the whole list.',
    })
  }

  if (issues.length === 0 && resolved.length > 0) {
    issues.push({
      id: 'balanced',
      tone: 'good',
      title: 'List looks balanced',
      text: 'Keep checking the official round-wise cut-offs and remember that preference order is more important than a fixed “safe” formula.',
    })
  }

  return { total: resolved.length, uniqueColleges, counts, issues }
}

export function preferenceLabel(item: PreferenceItem): string {
  const college = COLLEGE_BY_ID[item.collegeId]
  const branch = BRANCH_BY_ID[item.branchId]
  return `${college?.name ?? 'Unknown college'} — ${branch?.name ?? 'Unknown branch'}`
}

export function formatPreferenceText(items: PreferenceItem[], profile: StudentProfile): string {
  const category = CATEGORY_BY_ID[profile.category]
  const header = [
    'CAP COMPASS — MY CAP PREFERENCE LIST',
    profile.name ? `Student: ${profile.name}` : '',
    `MHT-CET percentile: ${profile.percentile === null ? 'Not set' : profile.percentile.toFixed(2)}`,
    `Category: ${category.label}${profile.gender === 'female' ? ' · Female' : ''}`,
    profile.homeUniversity ? `Home university: ${profile.homeUniversity}` : '',
    '',
  ].filter(Boolean)

  const rows = items.map((item, index) => {
    const college = COLLEGE_BY_ID[item.collegeId]
    const branch = BRANCH_BY_ID[item.branchId]
    const cutoff = college ? calculateCutoff(college, item.branchId, profile) : 0
    return `${index + 1}. ${college?.name ?? 'Unknown college'} — ${branch?.name ?? 'Unknown branch'} (${college?.city ?? ''}) · sample cutoff ${cutoff.toFixed(2)}`
  })

  return [
    ...header,
    ...rows,
    '',
    'Planning sheet only. Verify institute codes, course codes and official cut-offs on cetcell.mahacet.org before submitting.',
    `Generated ${new Date().toLocaleDateString('en-IN')}`,
  ].join('\n')
}

const escapeCsv = (value: string | number) => `"${String(value).replaceAll('"', '""')}"`

export function formatPreferenceCsv(items: PreferenceItem[], profile: StudentProfile): string {
  const options = new Map(getAllCourseOptions(profile).map((option) => [option.key, option]))
  const headers = [
    'Preference No.',
    'College',
    'Branch',
    'City',
    'College type',
    'Sample closing percentile',
    'Fit estimate',
    'Official institute code',
    'Official course code',
  ]
  const rows = items.map((item, index) => {
    const option = options.get(`${item.collegeId}:${item.branchId}`)
    return [
      index + 1,
      option?.college.name ?? '',
      option?.branch.name ?? '',
      option?.college.city ?? '',
      option?.college.type ?? '',
      option ? option.cutoff.toFixed(2) : '',
      option?.fit ? FIT_META[option.fit].label : 'Not estimated',
      '',
      '',
    ]
  })
  return [headers, ...rows].map((row) => row.map(escapeCsv).join(',')).join('\r\n')
}

export function getRoundReadiness(items: PreferenceItem[], profile: StudentProfile): number {
  if (profile.percentile === null || items.length === 0) return 0
  const analysis = analysePreferences(items, profile)
  let score = Math.min(items.length, 12) * 4
  if (analysis.counts.strong >= 2) score += 14
  if (analysis.counts.good >= 3) score += 10
  if (analysis.counts.stretch >= 2) score += 8
  if (analysis.uniqueColleges >= 4) score += 8
  if (items.length >= 12) score += 12
  return Math.min(score, 100)
}
