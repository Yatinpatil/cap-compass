import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  CheckCircle2,
  ClipboardCheck,
  Compass,
  ExternalLink,
  ListOrdered,
  RotateCcw,
  Search,
  ShieldCheck,
  UserRound,
} from 'lucide-react'
import type { BranchId, PreferenceItem, StudentProfile } from './data/catalog'
import { buildStarterList, getCourseOptions } from './lib/planner'
import {
  DEFAULT_PROFILE,
  STORAGE_KEYS,
  makePreference,
  useStoredState,
} from './lib/storage'
import { addBackButtonListener, exitNativeApp, interceptExternalLinks } from './lib/native'
import ProfileStep from './components/ProfileStep'
import ExploreStep from './components/ExploreStep'
import PreferenceStep from './components/PreferenceStep'
import GuideStep from './components/GuideStep'
import PrivacyPolicy from './components/PrivacyPolicy'

type Screen = 'profile' | 'explore' | 'preferences' | 'guide'

const STEPS: Array<{
  id: Screen
  label: string
  caption: string
  icon: typeof UserRound
}> = [
  { id: 'profile', label: 'Profile', caption: 'Score & branches', icon: UserRound },
  { id: 'explore', label: 'Explore', caption: 'Find your range', icon: Search },
  { id: 'preferences', label: 'My list', caption: 'Set the order', icon: ListOrdered },
  { id: 'guide', label: 'CAP guide', caption: 'Fill the form', icon: ClipboardCheck },
]

const PREVIOUS_SCREEN: Record<Screen, Screen | null> = {
  profile: null,
  explore: 'profile',
  preferences: 'explore',
  guide: 'preferences',
}

interface ToastState {
  id: number
  message: string
}

function App() {
  const [screen, setScreen] = useState<Screen>('profile')
  const [profile, setProfile] = useStoredState<StudentProfile>(STORAGE_KEYS.profile, DEFAULT_PROFILE)
  const [preferences, setPreferences] = useStoredState<PreferenceItem[]>(STORAGE_KEYS.preferences, [])
  const [checkedTasks, setCheckedTasks] = useStoredState<Record<string, boolean>>(STORAGE_KEYS.checklist, {})
  const [checkedDocuments, setCheckedDocuments] = useStoredState<Record<string, boolean>>(
    STORAGE_KEYS.documents,
    {},
  )
  const [toast, setToast] = useState<ToastState | null>(null)
  const [privacyOpen, setPrivacyOpen] = useState(false)

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(null), 3600)
    return () => window.clearTimeout(timer)
  }, [toast])

  const showToast = useCallback((message: string) => {
    setToast({ id: Date.now() + Math.random(), message })
  }, [])

  const goTo = useCallback((nextScreen: Screen) => {
    setScreen(nextScreen)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [])

  useEffect(() => interceptExternalLinks(), [])

  useEffect(
    () =>
      addBackButtonListener(() => {
        const previousScreen = PREVIOUS_SCREEN[screen]
        if (previousScreen) {
          goTo(previousScreen)
          return
        }
        void exitNativeApp()
      }),
    [goTo, screen],
  )

  const saveProfile = () => {
    setProfile((current) => ({ ...current, configured: true }))
    goTo('explore')
    showToast('Profile saved. Your matches are ready.')
  }

  const addedKeys = useMemo(
    () => new Set(preferences.map((item) => `${item.collegeId}:${item.branchId}`)),
    [preferences],
  )

  const addPreference = (collegeId: string, branchId: BranchId) => {
    const key = `${collegeId}:${branchId}`
    if (addedKeys.has(key)) return
    setPreferences((current) => [...current, makePreference(collegeId, branchId, current.length)])
    showToast('Course added to your CAP list.')
  }

  const autoBuild = () => {
    if (profile.percentile === null) {
      goTo('profile')
      showToast('Add your percentile before auto-building a list.')
      return
    }
    if (preferences.length > 0) {
      goTo('preferences')
      showToast('Auto-build only starts a new list. Use Explore to add more choices.')
      return
    }
    const options = getCourseOptions(profile)
    const keys = buildStarterList(profile, options)
    if (keys.length === 0) {
      showToast(
        profile.cities.length > 0
          ? 'No matching courses in your preferred cities. Try clearing city preferences.'
          : 'Add more branches or a percentile to build a starter list.',
      )
      return
    }
    const items = keys.map((key, index) => {
      const [collegeId, branchId] = key.split(':') as [string, BranchId]
      return makePreference(collegeId, branchId, index)
    })
    setPreferences(items)
    goTo('preferences')
    showToast('Starter list built. Reorder it around your true preference.')
  }

  const resetPlan = () => {
    const confirmed = window.confirm(
      'Reset your profile, preference list and checklist on this device? This cannot be undone.',
    )
    if (!confirmed) return
    setProfile(DEFAULT_PROFILE)
    setPreferences([])
    setCheckedTasks({})
    setCheckedDocuments({})
    goTo('profile')
    showToast('Plan reset. You can start fresh.')
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="container topbar__inner">
          <button type="button" className="brand" onClick={() => goTo('profile')}>
            <span className="brand__mark">
              <Compass size={22} strokeWidth={2.2} aria-hidden="true" />
            </span>
            <span className="brand__text">
              <strong>CAP Compass</strong>
              <small>MHT-CET preference planner</small>
            </span>
          </button>
          <div className="topbar__actions">
            <span className="save-chip">
              <ShieldCheck size={14} aria-hidden="true" />
              Saved on this device
            </span>
            <button type="button" className="reset-button" onClick={resetPlan} aria-label="Reset plan">
              <RotateCcw size={15} aria-hidden="true" />
              <span>Reset plan</span>
            </button>
          </div>
        </div>
      </header>

      <nav className="stepbar" aria-label="Planner steps">
        <div className="container stepbar__inner">
          {STEPS.map((step, index) => {
            const Icon = step.icon
            const active = screen === step.id
            const complete =
              step.id === 'profile'
                ? profile.configured
                : step.id === 'preferences'
                  ? preferences.length > 0
                  : step.id === 'guide'
                    ? Object.values(checkedTasks).some(Boolean)
                    : false
            return (
              <button
                key={step.id}
                type="button"
                className={`step ${active ? 'is-active' : ''} ${complete ? 'is-complete' : ''}`}
                onClick={() => goTo(step.id)}
                aria-current={active ? 'step' : undefined}
              >
                <span className="step__num">
                  {complete && !active ? <CheckCircle2 size={17} aria-hidden="true" /> : <Icon size={16} aria-hidden="true" />}
                </span>
                <span className="step__copy">
                  <strong>
                    <span className="step__index">0{index + 1}</span>
                    {step.label}
                    {step.id === 'preferences' && preferences.length > 0 ? (
                      <span className="step__count">{preferences.length}</span>
                    ) : null}
                  </strong>
                  <small>{step.caption}</small>
                </span>
              </button>
            )
          })}
        </div>
      </nav>

      <main className="container main">
        {screen === 'profile' ? (
          <ProfileStep
            profile={profile}
            setProfile={setProfile}
            onSubmit={saveProfile}
            preferenceCount={preferences.length}
            onOpenPreferences={() => goTo('preferences')}
          />
        ) : null}

        {screen === 'explore' ? (
          <ExploreStep
            profile={profile}
            preferenceCount={preferences.length}
            onAdd={addPreference}
            addedKeys={addedKeys}
            onAutoBuild={autoBuild}
            onOpenPreferences={() => goTo('preferences')}
            onEditProfile={() => goTo('profile')}
          />
        ) : null}

        {screen === 'preferences' ? (
          <PreferenceStep
            profile={profile}
            preferences={preferences}
            setPreferences={setPreferences}
            onExplore={() => goTo('explore')}
            onGuide={() => goTo('guide')}
            onAutoBuild={autoBuild}
            onProfile={() => goTo('profile')}
            onToast={showToast}
          />
        ) : null}

        {screen === 'guide' ? (
          <GuideStep
            profile={profile}
            preferences={preferences}
            checkedTasks={checkedTasks}
            setCheckedTasks={setCheckedTasks}
            checkedDocuments={checkedDocuments}
            setCheckedDocuments={setCheckedDocuments}
            onPreferences={() => goTo('preferences')}
          />
        ) : null}
      </main>

      <footer className="site-footer">
        <div className="container site-footer__inner">
          <div className="site-footer__brand">
            <span className="brand__mark brand__mark--small">
              <Compass size={18} aria-hidden="true" />
            </span>
            <div>
              <strong>CAP Compass</strong>
              <p>An independent planner for Maharashtra engineering admissions.</p>
            </div>
          </div>
          <p className="site-footer__note">
            Not affiliated with the State CET Cell. Sample cut-offs are for planning only. Always verify the current
            brochure, cut-offs, institute codes and course codes before submitting.
          </p>
          <div className="site-footer__links">
            <button type="button" onClick={() => setPrivacyOpen(true)}>
              Privacy policy
            </button>
            <a href="https://cetcell.mahacet.org/" target="_blank" rel="noreferrer">
              Official CET Cell portal
              <ExternalLink size={14} aria-hidden="true" />
            </a>
          </div>
        </div>
      </footer>

      {privacyOpen ? <PrivacyPolicy onClose={() => setPrivacyOpen(false)} /> : null}

      <div className={`toast ${toast ? 'is-visible' : ''}`} role="status" aria-live="polite">
        {toast ? (
          <>
            <CheckCircle2 size={17} aria-hidden="true" />
            <span>{toast.message}</span>
          </>
        ) : null}
      </div>
    </div>
  )
}

export default App
