export type CategoryId =
  | 'open'
  | 'ews'
  | 'obc'
  | 'vjdt'
  | 'ntb'
  | 'ntc'
  | 'ntd'
  | 'sbc'
  | 'sc'
  | 'st'

export type GenderId = 'female' | 'other'

export type BranchId =
  | 'cs'
  | 'it'
  | 'aids'
  | 'entc'
  | 'electrical'
  | 'mech'
  | 'civil'
  | 'chemical'
  | 'robotics'
  | 'instrumentation'

export type CollegeType = 'Government' | 'Aided' | 'Private' | 'State university'

export type FitLevel = 'strong' | 'good' | 'stretch' | 'longshot'

export interface Category {
  id: CategoryId
  label: string
  description: string
  cutoffAdvantage: number
}

export interface Branch {
  id: BranchId
  name: string
  shortName: string
  demand: number
}

export interface College {
  id: string
  name: string
  shortName: string
  city: string
  type: CollegeType
  baseCutoff: number
  branches: BranchId[]
}

export interface StudentProfile {
  name: string
  percentile: number | null
  category: CategoryId
  gender: GenderId
  homeUniversity: string
  branches: BranchId[]
  cities: string[]
  configured: boolean
}

export interface PreferenceItem {
  id: string
  collegeId: string
  branchId: BranchId
  addedAt: number
}

export interface CourseOption {
  key: string
  college: College
  branch: Branch
  cutoff: number
  gap: number | null
  fit: FitLevel | null
}

export const CATEGORIES: Category[] = [
  { id: 'open', label: 'Open / General', description: 'No category reservation claimed', cutoffAdvantage: 0 },
  { id: 'ews', label: 'EWS', description: 'Economically Weaker Section', cutoffAdvantage: 0.7 },
  { id: 'obc', label: 'OBC', description: 'Other Backward Classes', cutoffAdvantage: 1.8 },
  { id: 'vjdt', label: 'VJ / DT', description: 'Vimukta Jati / Denotified Tribes', cutoffAdvantage: 3.6 },
  { id: 'ntb', label: 'NT-B', description: 'Nomadic Tribe B', cutoffAdvantage: 4.0 },
  { id: 'ntc', label: 'NT-C', description: 'Nomadic Tribe C', cutoffAdvantage: 4.2 },
  { id: 'ntd', label: 'NT-D', description: 'Nomadic Tribe D', cutoffAdvantage: 4.5 },
  { id: 'sbc', label: 'SBC', description: 'Special Backward Classes', cutoffAdvantage: 3.2 },
  { id: 'sc', label: 'SC', description: 'Scheduled Caste', cutoffAdvantage: 6.0 },
  { id: 'st', label: 'ST', description: 'Scheduled Tribe', cutoffAdvantage: 9.0 },
]

export const BRANCHES: Branch[] = [
  { id: 'cs', name: 'Computer Engineering', shortName: 'CS', demand: 2.6 },
  { id: 'it', name: 'Information Technology', shortName: 'IT', demand: 2.1 },
  { id: 'aids', name: 'AI & Data Science', shortName: 'AI&DS', demand: 2.9 },
  { id: 'entc', name: 'Electronics & Telecommunication', shortName: 'E&TC', demand: 1.1 },
  { id: 'electrical', name: 'Electrical Engineering', shortName: 'EE', demand: 0.3 },
  { id: 'mech', name: 'Mechanical Engineering', shortName: 'ME', demand: -1.0 },
  { id: 'civil', name: 'Civil Engineering', shortName: 'CE', demand: -1.9 },
  { id: 'chemical', name: 'Chemical Engineering', shortName: 'CHE', demand: -0.4 },
  { id: 'robotics', name: 'Robotics & Automation', shortName: 'RA', demand: 1.0 },
  { id: 'instrumentation', name: 'Instrumentation Engineering', shortName: 'IE', demand: -0.7 },
]

export const CITIES = [
  'Pune',
  'Mumbai',
  'Navi Mumbai',
  'Nagpur',
  'Nashik',
  'Chhatrapati Sambhajinagar',
  'Amravati',
  'Kolhapur',
  'Sangli',
  'Solapur',
  'Karad',
  'Jalgaon',
  'Lonere',
  'Ahmednagar',
]

export const HOME_UNIVERSITIES = [
  'University of Mumbai',
  'Savitribai Phule Pune University',
  'Rashtrasant Tukadoji Maharaj Nagpur University',
  'Sant Gadge Baba Amravati University',
  'Dr. Babasaheb Ambedkar Marathwada University',
  'Shivaji University, Kolhapur',
  'Punyashlok Ahilyadevi Holkar Solapur University',
  'Swami Ramanand Teerth Marathwada University',
  'Kavayitri Bahinabai Chaudhari North Maharashtra University',
  'Other / not sure',
]

export const COLLEGES: College[] = [
  {
    id: 'coep',
    name: 'COEP Technological University',
    shortName: 'COEP',
    city: 'Pune',
    type: 'State university',
    baseCutoff: 97.25,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil', 'instrumentation'],
  },
  {
    id: 'vjti',
    name: 'Veermata Jijabai Technological Institute',
    shortName: 'VJTI',
    city: 'Mumbai',
    type: 'Government',
    baseCutoff: 97.0,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'spce',
    name: 'Sardar Patel College of Engineering',
    shortName: 'SPCE',
    city: 'Mumbai',
    type: 'Aided',
    baseCutoff: 95.7,
    branches: ['civil', 'mech', 'electrical'],
  },
  {
    id: 'djsce',
    name: 'Dwarkadas J. Sanghvi College of Engineering',
    shortName: 'DJSCE',
    city: 'Mumbai',
    type: 'Private',
    baseCutoff: 95.2,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil', 'chemical'],
  },
  {
    id: 'kjsc',
    name: 'K. J. Somaiya College of Engineering',
    shortName: 'KJSCE',
    city: 'Mumbai',
    type: 'Private',
    baseCutoff: 94.5,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil', 'chemical'],
  },
  {
    id: 'tsec',
    name: 'Thadomal Shahani Engineering College',
    shortName: 'TSEC',
    city: 'Mumbai',
    type: 'Private',
    baseCutoff: 93.5,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil', 'chemical'],
  },
  {
    id: 'vesit',
    name: "Vivekanand Education Society's Institute of Technology",
    shortName: 'VESIT',
    city: 'Mumbai',
    type: 'Private',
    baseCutoff: 94.0,
    branches: ['cs', 'it', 'aids', 'entc', 'instrumentation'],
  },
  {
    id: 'rait',
    name: 'Ramrao Adik Institute of Technology',
    shortName: 'RAIT',
    city: 'Navi Mumbai',
    type: 'Private',
    baseCutoff: 92.9,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'crce',
    name: 'Fr. Conceicao Rodrigues College of Engineering',
    shortName: 'CRCE',
    city: 'Mumbai',
    type: 'Private',
    baseCutoff: 92.2,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'pict',
    name: 'Pune Institute of Computer Technology',
    shortName: 'PICT',
    city: 'Pune',
    type: 'Private',
    baseCutoff: 96.1,
    branches: ['cs', 'it', 'aids', 'entc'],
  },
  {
    id: 'pccoe',
    name: 'Pimpri Chinchwad College of Engineering',
    shortName: 'PCCOE',
    city: 'Pune',
    type: 'Private',
    baseCutoff: 93.2,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'vitp',
    name: 'Vishwakarma Institute of Technology',
    shortName: 'VIT Pune',
    city: 'Pune',
    type: 'Private',
    baseCutoff: 94.2,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil', 'chemical'],
  },
  {
    id: 'mitwpu',
    name: 'MIT World Peace University',
    shortName: 'MIT-WPU',
    city: 'Pune',
    type: 'Private',
    baseCutoff: 92.1,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil', 'chemical'],
  },
  {
    id: 'aissms',
    name: 'AISSMS College of Engineering',
    shortName: 'AISSMS',
    city: 'Pune',
    type: 'Private',
    baseCutoff: 90.7,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'mitaoe',
    name: 'MIT Academy of Engineering',
    shortName: 'MIT AOE',
    city: 'Pune',
    type: 'Private',
    baseCutoff: 88.9,
    branches: ['cs', 'it', 'aids', 'entc', 'mech', 'civil'],
  },
  {
    id: 'sinhgad',
    name: 'Sinhgad College of Engineering',
    shortName: 'SCOE',
    city: 'Pune',
    type: 'Private',
    baseCutoff: 86.9,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil', 'chemical'],
  },
  {
    id: 'gcoen',
    name: 'Government College of Engineering, Nagpur',
    shortName: 'GCOE Nagpur',
    city: 'Nagpur',
    type: 'Government',
    baseCutoff: 91.2,
    branches: ['cs', 'it', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'rcoem',
    name: 'Shri Ramdeobaba College of Engineering and Management',
    shortName: 'RCOEM',
    city: 'Nagpur',
    type: 'Private',
    baseCutoff: 92.0,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'ycce',
    name: 'Yeshwantrao Chavan College of Engineering',
    shortName: 'YCCE',
    city: 'Nagpur',
    type: 'Private',
    baseCutoff: 90.8,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'raisoni',
    name: 'G. H. Raisoni College of Engineering',
    shortName: 'GH Raisoni',
    city: 'Nagpur',
    type: 'Private',
    baseCutoff: 85.8,
    branches: ['cs', 'it', 'aids', 'entc', 'mech', 'civil'],
  },
  {
    id: 'gcoea',
    name: 'Government College of Engineering, Amravati',
    shortName: 'GCOE Amravati',
    city: 'Amravati',
    type: 'Government',
    baseCutoff: 86.6,
    branches: ['cs', 'it', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'gcoes',
    name: 'Government College of Engineering, Chhatrapati Sambhajinagar',
    shortName: 'GCOE Sambhajinagar',
    city: 'Chhatrapati Sambhajinagar',
    type: 'Government',
    baseCutoff: 88.1,
    branches: ['cs', 'it', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'gcoek',
    name: 'Government College of Engineering, Karad',
    shortName: 'GCOE Karad',
    city: 'Karad',
    type: 'Government',
    baseCutoff: 85.2,
    branches: ['cs', 'it', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'wce',
    name: 'Walchand College of Engineering',
    shortName: 'WCE Sangli',
    city: 'Sangli',
    type: 'Aided',
    baseCutoff: 92.4,
    branches: ['cs', 'it', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'kit',
    name: 'Kolhapur Institute of Technology',
    shortName: 'KIT Kolhapur',
    city: 'Kolhapur',
    type: 'Private',
    baseCutoff: 88.5,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'wit',
    name: 'Walchand Institute of Technology',
    shortName: 'WIT Solapur',
    city: 'Solapur',
    type: 'Private',
    baseCutoff: 88.2,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'kkwagh',
    name: 'K. K. Wagh Institute of Engineering Education and Research',
    shortName: 'KK Wagh',
    city: 'Nashik',
    type: 'Private',
    baseCutoff: 89.3,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil', 'chemical'],
  },
  {
    id: 'gcoej',
    name: 'Government College of Engineering, Jalgaon',
    shortName: 'GCOE Jalgaon',
    city: 'Jalgaon',
    type: 'Government',
    baseCutoff: 81.8,
    branches: ['cs', 'it', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'dbatu',
    name: 'Dr. Babasaheb Ambedkar Technological University',
    shortName: 'DBATU',
    city: 'Lonere',
    type: 'State university',
    baseCutoff: 83.2,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil', 'chemical'],
  },
  {
    id: 'jnec',
    name: 'MGM Jawaharlal Nehru Engineering College',
    shortName: 'JNEC',
    city: 'Chhatrapati Sambhajinagar',
    type: 'Private',
    baseCutoff: 87.1,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'pravara',
    name: 'Pravara Rural Engineering College',
    shortName: 'PREC',
    city: 'Ahmednagar',
    type: 'Private',
    baseCutoff: 82.8,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
  {
    id: 'prmit',
    name: 'Prof. Ram Meghe Institute of Technology and Research',
    shortName: 'PRMIT',
    city: 'Amravati',
    type: 'Private',
    baseCutoff: 81.7,
    branches: ['cs', 'it', 'aids', 'entc', 'electrical', 'mech', 'civil'],
  },
]

export const BRANCH_BY_ID = Object.fromEntries(BRANCHES.map((branch) => [branch.id, branch])) as Record<
  BranchId,
  Branch
>

export const COLLEGE_BY_ID = Object.fromEntries(COLLEGES.map((college) => [college.id, college])) as Record<
  string,
  College
>

export const CATEGORY_BY_ID = Object.fromEntries(CATEGORIES.map((category) => [category.id, category])) as Record<
  CategoryId,
  Category
>
