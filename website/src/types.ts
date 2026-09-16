export type ProviderType = 'ROU' | 'DS' | 'GLM' | 'VOL' | 'NEW' | 'CMD';

export interface ProviderItem {
  id: string;
  code: ProviderType;
  name: string;
  keyMask: string;
  tagColor: string;
  badgeBg: string;
  status: 'active' | 'warning' | 'error';
  statusLabel: string;
  primaryMetric: string;
  primaryValue: string;
  subMetric?: string;
  subValue?: string;
  progress1?: {
    label: string;
    value: number; // percentage
    display: string;
    warning?: boolean;
  };
  progress2?: {
    label: string;
    value: number; // percentage
    display: string;
  };
  detailPills?: string[];
  lastUpdated: string;
}

export interface MatrixRow {
  provider: string;
  code: ProviderType;
  color: string;
  credential: string;
  credentialFormat: string;
  metrics: string[];
  exclusiveFeatures: string[];
  officialDocUrl: string;
  sampleEndpoint: string;
}

export interface FaqItem {
  id: string;
  question: string;
  answer: string;
  isWide?: boolean;
}
