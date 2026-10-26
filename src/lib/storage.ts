import { useEffect, useState, type Dispatch, type SetStateAction } from 'react'
import type { BranchId, PreferenceItem, StudentProfile } from '../data/catalog'

export const DEFAULT_PROFILE: StudentProfile = {
  name: '',
  percentile: null,
  category: 'open',
  gender: 'other',
  homeUniversity: '',
  branches: ['cs', 'it', 'aids'],
  cities: [],
  configured: false,
}

export function useStoredState<T>(
  key: string,
  initialValue: T,
): [T, Dispatch<SetStateAction<T>>] {
  const [value, setValue] = useState<T>(() => {
    try {
      const stored = window.localStorage.getItem(key)
      return stored ? (JSON.parse(stored) as T) : initialValue
    } catch {
      return initialValue
    }
  })

  useEffect(() => {
    try {
      window.localStorage.setItem(key, JSON.stringify(value))
    } catch {
      // Storage may be disabled or full; the planner still works for this session.
    }
  }, [key, value])

  return [value, setValue]
}

export const STORAGE_KEYS = {
  profile: 'cap-compass.profile.v1',
  preferences: 'cap-compass.preferences.v1',
  checklist: 'cap-compass.checklist.v1',
  documents: 'cap-compass.documents.v1',
} as const

export function makePreference(collegeId: string, branchId: BranchId, index = 0): PreferenceItem {
  return {
    id: `${collegeId}:${branchId}:${Date.now()}:${index}`,
    collegeId,
    branchId,
    addedAt: Date.now() + index,
  }
}
