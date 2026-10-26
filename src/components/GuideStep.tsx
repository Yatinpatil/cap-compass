import { useMemo, useState, type Dispatch, type SetStateAction } from 'react'
import {
  ArrowRight,
  Check,
  ChevronDown,
  ClipboardCheck,
  ExternalLink,
  FileText,
  Info,
  ListOrdered,
  ShieldCheck,
  Sparkles,
} from 'lucide-react'
import { CATEGORY_BY_ID, type PreferenceItem, type StudentProfile } from '../data/catalog'
import { GUIDE_STEPS, getDocuments } from '../data/guide'

interface GuideStepProps {
  profile: StudentProfile
  preferences: PreferenceItem[]
  checkedTasks: Record<string, boolean>
  setCheckedTasks: Dispatch<SetStateAction<Record<string, boolean>>>
  checkedDocuments: Record<string, boolean>
  setCheckedDocuments: Dispatch<SetStateAction<Record<string, boolean>>>
  onPreferences: () => void
}

export default function GuideStep({
  profile,
  preferences,
  checkedTasks,
  setCheckedTasks,
  checkedDocuments,
  setCheckedDocuments,
  onPreferences,
}: GuideStepProps) {
  const documents = useMemo(() => getDocuments(profile), [profile])
  const [openStep, setOpenStep] = useState<string>(() => {
    const firstIncomplete = GUIDE_STEPS.find((step) => step.tasks.some((task) => !checkedTasks[task.id]))
    return firstIncomplete?.id ?? GUIDE_STEPS[0].id
  })
  const [showAllDocuments, setShowAllDocuments] = useState(false)
  const category = CATEGORY_BY_ID[profile.category]

  const totalTasks = GUIDE_STEPS.reduce((total, step) => total + step.tasks.length, 0)
  const completedTasks = GUIDE_STEPS.reduce(
    (total, step) => total + step.tasks.filter((task) => checkedTasks[task.id]).length,
    0,
  )
  const progress = Math.round((completedTasks / totalTasks) * 100)
  const completedDocuments = documents.filter((document) => checkedDocuments[document.id]).length
  const visibleDocuments = showAllDocuments ? documents : documents.slice(0, 6)

  const toggleTask = (id: string) => {
    setCheckedTasks((current) => ({ ...current, [id]: !current[id] }))
  }

  const toggleDocument = (id: string) => {
    setCheckedDocuments((current) => ({ ...current, [id]: !current[id] }))
  }

  return (
    <div className="screen-stack">
      <section className="page-heading page-heading--split">
        <div>
          <span className="eyebrow">Step 04 · CAP guide</span>
          <h1>CAP form, without the guesswork.</h1>
          <p>
            A personalised run-through for {category.label}
            {profile.homeUniversity ? ` · ${profile.homeUniversity}` : ''}. Tick items off as you complete them on the
            official portal.
          </p>
          <div className="heading-actions">
            <button type="button" className="text-link" onClick={onPreferences}>
              Review my list ({preferences.length})
              <ArrowRight size={14} aria-hidden="true" />
            </button>
          </div>
        </div>
        <div className="guide-progress-card">
          <div>
            <strong>{progress}%</strong>
            <span>checklist complete</span>
          </div>
          <div className="guide-progress-track">
            <span style={{ width: `${progress}%` }} />
          </div>
          <small>{completedTasks} of {totalTasks} steps checked</small>
        </div>
      </section>

      <div className="guide-layout">
        <section className="card checklist-card">
          <div className="checklist-card__head">
            <div>
              <span className="eyebrow">Your form checklist</span>
              <h2>Five stages, one clean submission</h2>
            </div>
            <span className="progress-pill">
              <ClipboardCheck size={15} aria-hidden="true" />
              {completedTasks}/{totalTasks}
            </span>
          </div>

          <div className="guide-step-list">
            {GUIDE_STEPS.map((step, index) => {
              const stepCompleted = step.tasks.filter((task) => checkedTasks[task.id]).length
              const isComplete = stepCompleted === step.tasks.length
              const isOpen = openStep === step.id
              return (
                <article className={`guide-step ${isOpen ? 'is-open' : ''} ${isComplete ? 'is-complete' : ''}`} key={step.id}>
                  <button
                    type="button"
                    className="guide-step__head"
                    aria-expanded={isOpen}
                    onClick={() => setOpenStep(isOpen ? '' : step.id)}
                  >
                    <span className="guide-step__num">{isComplete ? <Check size={17} aria-hidden="true" /> : index + 1}</span>
                    <span className="guide-step__title">
                      <strong>{step.title}</strong>
                      <small>
                        {stepCompleted} of {step.tasks.length} checked
                      </small>
                    </span>
                    <span className="guide-step__status">{isComplete ? 'Done' : isOpen ? 'In progress' : ''}</span>
                    <ChevronDown size={18} className={isOpen ? 'is-flipped' : ''} aria-hidden="true" />
                  </button>
                  {isOpen ? (
                    <div className="guide-step__body">
                      <p>{step.summary}</p>
                      <div className="task-list">
                        {step.tasks.map((task) => (
                          <label className={`task-item ${checkedTasks[task.id] ? 'is-checked' : ''}`} key={task.id}>
                            <input
                              type="checkbox"
                              checked={Boolean(checkedTasks[task.id])}
                              onChange={() => toggleTask(task.id)}
                            />
                            <span className="custom-check" aria-hidden="true">
                              <Check size={12} />
                            </span>
                            <span>{task.label}</span>
                          </label>
                        ))}
                      </div>
                      <div className="guide-tip">
                        <Sparkles size={16} aria-hidden="true" />
                        <p>{step.tip}</p>
                      </div>
                    </div>
                  ) : null}
                </article>
              )
            })}
          </div>
          <div className="checklist-card__foot">
            <ShieldCheck size={16} aria-hidden="true" />
            Progress is saved automatically in this browser. Nothing is uploaded.
          </div>
        </section>

        <aside className="side-stack">
          <section className="card docs-card">
            <div className="section-heading section-heading--compact">
              <div>
                <span className="eyebrow">Document tray</span>
                <h3>Keep these ready</h3>
              </div>
              <span className="progress-pill">
                {completedDocuments}/{documents.length}
              </span>
            </div>
            <p className="small-note">Tailored to your category. Check the current-year brochure for exact formats.</p>
            <div className="doc-list">
              {visibleDocuments.map((document) => (
                <label
                  className={`doc-item ${checkedDocuments[document.id] ? 'is-checked' : ''}`}
                  key={document.id}
                >
                  <input
                    type="checkbox"
                    checked={Boolean(checkedDocuments[document.id])}
                    onChange={() => toggleDocument(document.id)}
                  />
                  <span className="custom-check" aria-hidden="true">
                    <Check size={12} />
                  </span>
                  <span className="doc-item__copy">
                    <strong>
                      {document.label}
                      {document.conditional ? <em>If applicable</em> : null}
                    </strong>
                    <small>{document.note}</small>
                  </span>
                </label>
              ))}
            </div>
            {documents.length > 6 ? (
              <button type="button" className="card-more" onClick={() => setShowAllDocuments((current) => !current)}>
                {showAllDocuments ? 'Show fewer documents' : `Show ${documents.length - 6} more documents`}
                <ChevronDown size={15} className={showAllDocuments ? 'is-flipped' : ''} aria-hidden="true" />
              </button>
            ) : null}
          </section>

          <section className="card guide-list-card">
            <div className="guide-list-card__top">
              <span className="next-card__icon">
                <ListOrdered size={19} aria-hidden="true" />
              </span>
              <div>
                <span className="eyebrow">Your preference list</span>
                <h3>
                  {preferences.length === 0
                    ? 'No course choices yet'
                    : `${preferences.length} course ${preferences.length === 1 ? 'choice' : 'choices'}`}
                </h3>
              </div>
            </div>
            <p>
              {preferences.length === 0
                ? 'Build your CAP list before starting the preference-entry step.'
                : 'Use the printable sheet beside the official portal when entering institute and course codes.'}
            </p>
            <button type="button" className="text-link" onClick={onPreferences}>
              {preferences.length === 0 ? 'Build my list' : 'Open my list'}
              <ArrowRight size={14} aria-hidden="true" />
            </button>
          </section>

          <section className="card official-card">
            <span className="eyebrow">Official source</span>
            <h3>Only submit through CET Cell</h3>
            <p>
              Confirm the current CAP schedule, information brochure, cut-off explorer and portal links on the State CET
              Cell website.
            </p>
            <a
              className="button button--dark button--small"
              href="https://cetcell.mahacet.org/"
              target="_blank"
              rel="noreferrer"
            >
              <ExternalLink size={14} aria-hidden="true" />
              Open official portal
            </a>
          </section>

          <section className="card tiny-warning">
            <Info size={17} aria-hidden="true" />
            <p>
              <strong>Do not share OTPs or passwords.</strong> No third-party planner can submit or guarantee a CAP
              allotment for you.
            </p>
          </section>
        </aside>
      </div>

      <section className="card after-card">
        <div className="after-card__head">
          <div>
            <span className="eyebrow">After submission</span>
            <h2>When the allotment is published</h2>
          </div>
          <FileText size={24} aria-hidden="true" />
        </div>
        <div className="after-grid">
          <article>
            <span>01</span>
            <h3>Read the allotment carefully</h3>
            <p>Check the institute, course, category and seat type shown in the official result.</p>
          </article>
          <article>
            <span>02</span>
            <h3>Watch the reporting deadline</h3>
            <p>Note the fee, document verification and reporting window for that specific round.</p>
          </article>
          <article>
            <span>03</span>
            <h3>Complete the next action</h3>
            <p>Accept, freeze, float or move to a later round exactly as the current rules require.</p>
          </article>
        </div>
      </section>

      <section className="footnote-card">
        <Info size={17} aria-hidden="true" />
        <p>
          <strong>No affiliation.</strong> CAP Compass is an independent planning tool. Admission rules, reservation
          requirements, schedules and fees can change every year. The official CET Cell brochure and portal always take
          precedence.
        </p>
      </section>
    </div>
  )
}
