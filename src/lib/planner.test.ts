import { describe, expect, it } from 'vitest'
import {
  BRANCH_BY_ID,
  COLLEGE_BY_ID,
  type PreferenceItem,
  type StudentProfile,
} from '../data/catalog'
import {
  analysePreferences,
  buildStarterList,
  calculateCutoff,
  formatPreferenceCsv,
  getAllCourseOptions,
  getCourseOptions,
  getFitLevel,
} from './planner'

const profile: StudentProfile = {
  name: 'Test Student',
  percentile: 92.5,
  category: 'obc',
  gender: 'female',
  homeUniversity: 'Savitribai Phule Pune University',
  branches: ['cs', 'it', 'aids'],
  cities: [],
  configured: true,
}

const preference = (collegeId: string, branchId: PreferenceItem['branchId'], index: number): PreferenceItem => ({
  id: `${collegeId}:${branchId}:${index}`,
  collegeId,
  branchId,
  addedAt: index,
})

describe('cutoff estimation', () => {
  it('applies category and ladies quota adjustments', () => {
    const openProfile = { ...profile, category: 'open' as const, gender: 'other' as const }
    const coep = COLLEGE_BY_ID.coep
    expect(calculateCutoff(coep, 'cs', profile)).toBeLessThan(calculateCutoff(coep, 'cs', openProfile))
  })

  it('never exceeds 100 or falls below the floor', () => {
    const top = calculateCutoff(COLLEGE_BY_ID.coep, 'aids', { ...profile, category: 'open', gender: 'other' })
    const low = calculateCutoff(COLLEGE_BY_ID.prmit, 'civil', { ...profile, category: 'st', gender: 'other' })
    expect(top).toBeLessThanOrEqual(100)
    expect(low).toBeGreaterThanOrEqual(45)
  })
})

describe('fit classification', () => {
  it('classifies gaps consistently', () => {
    expect(getFitLevel(95, 91)).toBe('strong')
    expect(getFitLevel(92, 92)).toBe('good')
    expect(getFitLevel(92, 94.5)).toBe('stretch')
    expect(getFitLevel(92, 98)).toBe('longshot')
    expect(getFitLevel(null, 92)).toBeNull()
  })
})

describe('course options', () => {
  it('limits explore options to preferred branches', () => {
    const options = getCourseOptions(profile)
    expect(options.length).toBeGreaterThan(0)
    expect(options.every((option) => profile.branches.includes(option.branch.id))).toBe(true)
  })

  it('keeps every branch available for resolving a saved list', () => {
    const all = getAllCourseOptions(profile)
    expect(all.length).toBeGreaterThan(getCourseOptions(profile).length)
  })
})

describe('starter list builder', () => {
  it('creates a 12-college list with no long-shot-only picks', () => {
    const items = buildStarterList(profile, getCourseOptions(profile))
    expect(items).toHaveLength(12)
    const colleges = items.map((key) => key.split(':')[0])
    expect(new Set(colleges).size).toBe(12)

    const preferenceItems = items.map((key, index) => {
      const [collegeId, branchId] = key.split(':')
      return preference(collegeId, branchId as PreferenceItem['branchId'], index)
    })
    const analysis = analysePreferences(preferenceItems, profile)
    expect(analysis.counts.longshot).toBe(0)
    expect(analysis.counts.stretch + analysis.counts.good + analysis.counts.strong).toBe(12)
  })

  it('respects selected cities when options exist', () => {
    const cityProfile = { ...profile, cities: ['Pune', 'Mumbai'] }
    const items = buildStarterList(cityProfile, getCourseOptions(cityProfile))
    expect(items).toHaveLength(12)
    expect(items.every((key) => ['Pune', 'Mumbai'].includes(COLLEGE_BY_ID[key.split(':')[0]].city))).toBe(true)
  })

  it('returns nothing without a percentile', () => {
    expect(buildStarterList({ ...profile, percentile: null }, getCourseOptions(profile))).toEqual([])
  })
})

describe('list analysis and export', () => {
  it('warns when the list is too short and has no safe option', () => {
    const items = [preference('coep', 'cs', 0)]
    const analysis = analysePreferences(items, profile)
    expect(analysis.total).toBe(1)
    expect(analysis.issues.some((issue) => issue.id === 'length')).toBe(true)
    expect(analysis.issues.some((issue) => issue.id === 'safe')).toBe(true)
  })

  it('resolves saved courses even after a branch preference is removed', () => {
    const item = preference('coep', 'civil', 0)
    const changedProfile: StudentProfile = { ...profile, branches: ['cs'] }
    expect(analysePreferences([item], changedProfile).total).toBe(1)
  })

  it('exports blank official code columns in the CSV', () => {
    const csv = formatPreferenceCsv([preference('coep', 'cs', 0)], profile)
    expect(csv).toContain('Official institute code')
    expect(csv).toContain(BRANCH_BY_ID.cs.name)
    expect(csv).toContain(COLLEGE_BY_ID.coep.name)
    expect(csv.split('\r\n')[1]?.endsWith('"",""')).toBe(true)
  })
})
