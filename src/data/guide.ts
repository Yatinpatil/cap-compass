import { CATEGORY_BY_ID, type StudentProfile } from './catalog'

export interface GuideTask {
  id: string
  label: string
}

export interface GuideStep {
  id: string
  title: string
  shortTitle: string
  summary: string
  tasks: GuideTask[]
  tip: string
}

export const GUIDE_STEPS: GuideStep[] = [
  {
    id: 'register',
    title: 'Register and open your CAP application',
    shortTitle: 'Register',
    summary:
      'Use the official CET Cell portal for the current admission year. Your MHT-CET application details are the starting point.',
    tasks: [
      { id: 'login', label: 'Log in to the official CET Cell CAP portal' },
      { id: 'app-id', label: 'Save your application ID and registration number safely' },
      { id: 'contact', label: 'Verify your mobile number and email address' },
      { id: 'details', label: 'Check that personal details match your Class 12 records' },
    ],
    tip: 'The portal and process can change each year. Follow the current year’s information brochure, not a screenshot from a previous year.',
  },
  {
    id: 'profile',
    title: 'Complete personal, academic and category details',
    shortTitle: 'Profile',
    summary:
      'Enter your MHT-CET percentile, category, home university and other eligibility details exactly as they appear in your documents.',
    tasks: [
      { id: 'percentile', label: 'Enter your MHT-CET percentile and roll number' },
      { id: 'category', label: 'Select the category you can validly claim' },
      { id: 'home-uni', label: 'Select your home university / eligibility university' },
      { id: 'reservation', label: 'Review any special reservation claims (EWS, ladies, PWD, defence, TFWS)' },
      { id: 'academics', label: 'Verify your qualifying examination and board details' },
    ],
    tip: 'A reservation claim without the required valid certificate can create problems later. Check the current rules before claiming it.',
  },
  {
    id: 'documents',
    title: 'Upload documents and pay the fee',
    shortTitle: 'Documents',
    summary:
      'Keep clean scans ready before you begin. File type, size and document validity rules are set by the current year’s brochure.',
    tasks: [
      { id: 'scan', label: 'Scan or photograph all required documents clearly' },
      { id: 'caste', label: 'Upload category, validity and income documents that apply to you' },
      { id: 'domicile', label: 'Upload domicile or home-university proof where required' },
      { id: 'fee', label: 'Pay the application / CAP fee and save the receipt' },
      { id: 'ack', label: 'Download the acknowledgement or application PDF' },
    ],
    tip: 'Keep both the original document and a clear digital copy. Do not crop out certificate numbers or seals.',
  },
  {
    id: 'preferences',
    title: 'Enter your course preferences in the right order',
    shortTitle: 'Preferences',
    summary:
      'A CAP preference is a specific college + branch combination. Fill the choices you genuinely want most first. Your list here is the order to copy.',
    tasks: [
      { id: 'open-cap', label: 'Open the CAP Round I preference-entry screen' },
      { id: 'order', label: 'Enter your top choice first and work downwards' },
      { id: 'enough', label: 'Add enough realistic options, not just dream colleges' },
      { id: 'codes', label: 'Cross-check every institute and course code on the official portal' },
      { id: 'save', label: 'Save the preferences and check the on-screen list' },
    ],
    tip: 'Do not put a safer college above one you prefer more. CAP allots the highest choice that is reachable, not the “best” choice by some other ranking.',
  },
  {
    id: 'submit',
    title: 'Lock, submit and track the rounds',
    shortTitle: 'Submit',
    summary:
      'After entering preferences, follow the exact lock/submit steps shown on the portal. Then track each round’s result and post-allotment action.',
    tasks: [
      { id: 'lock', label: 'Lock or submit before the deadline, as required' },
      { id: 'screenshot', label: 'Save the submitted preference list and confirmation' },
      { id: 'allotment', label: 'Check the official allotment result on the published date' },
      { id: 'action', label: 'Complete the round’s accept / freeze / float action as per current rules' },
      { id: 'next', label: 'If needed, participate in the next round within its deadline' },
    ],
    tip: 'Never share your password or OTP. Use only the official portal and keep every acknowledgement.',
  },
]

export interface DocumentItem {
  id: string
  label: string
  note: string
  conditional: boolean
}

export function getDocuments(profile: StudentProfile): DocumentItem[] {
  const category = CATEGORY_BY_ID[profile.category]
  const documents: DocumentItem[] = [
    {
      id: 'marks-10',
      label: 'Class 10 marksheet & certificate',
      note: 'For date of birth and academic record',
      conditional: false,
    },
    {
      id: 'marks-12',
      label: 'Class 12 / diploma marksheet',
      note: 'Qualifying examination proof',
      conditional: false,
    },
    {
      id: 'cet',
      label: 'MHT-CET scorecard',
      note: 'Keep the official scorecard PDF or printout',
      conditional: false,
    },
    {
      id: 'domicile',
      label: 'Maharashtra domicile certificate',
      note: 'Needed for state quota and home-university claims; check current rules',
      conditional: false,
    },
    {
      id: 'id',
      label: 'Aadhaar / accepted photo ID',
      note: 'Carry the original for verification',
      conditional: false,
    },
    {
      id: 'photo',
      label: 'Recent photograph & signature',
      note: 'Use the specifications in the current brochure',
      conditional: false,
    },
    {
      id: 'leaving',
      label: 'School leaving certificate',
      note: 'Often requested for admission reporting',
      conditional: false,
    },
  ]

  if (profile.category === 'open') {
    documents.push({
      id: 'income-tfws',
      label: 'Income certificate (only if claiming TFWS)',
      note: 'TFWS is a separate eligibility claim; verify current family-income rules',
      conditional: true,
    })
  } else if (profile.category === 'ews') {
    documents.push({
      id: 'ews',
      label: 'EWS eligibility certificate',
      note: 'Must be valid as per the current admission rules',
      conditional: false,
    })
    documents.push({
      id: 'income-ews',
      label: 'Income / supporting certificate for EWS',
      note: 'Issued by the competent authority',
      conditional: false,
    })
  } else if (profile.category === 'obc' || profile.category === 'sbc') {
    documents.push({
      id: 'caste',
      label: `${category.label} caste certificate`,
      note: 'From the competent authority',
      conditional: false,
    })
    documents.push({
      id: 'validity',
      label: 'Caste validity certificate',
      note: 'Check whether it is required at your stage',
      conditional: false,
    })
    documents.push({
      id: 'ncl',
      label: 'Non-creamy layer certificate',
      note: 'Required for OBC/SBC reservation benefits; check validity',
      conditional: false,
    })
  } else {
    documents.push({
      id: 'caste',
      label: `${category.label} caste certificate`,
      note: 'From the competent authority',
      conditional: false,
    })
    documents.push({
      id: 'validity',
      label: 'Caste validity certificate',
      note: 'Check the current-year validity requirement',
      conditional: false,
    })
  }

  documents.push(
    {
      id: 'pwd',
      label: 'PwD certificate (if applicable)',
      note: 'For a PwD reservation claim',
      conditional: true,
    },
    {
      id: 'defence',
      label: 'Defence / ex-servicemen certificate (if applicable)',
      note: 'For a defence quota claim',
      conditional: true,
    },
    {
      id: 'migration',
      label: 'Migration / gap certificate (if applicable)',
      note: 'Keep ready if your academic history needs it',
      conditional: true,
    },
  )

  return documents
}
