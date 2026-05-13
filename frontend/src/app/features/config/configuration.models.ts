export type LlmPurpose =
  | 'audience_classification'
  | 'summary_generation'
  | 'task_extraction'
  | 'transcript_correction';

export interface LlmUsageView {
  readonly purpose: LlmPurpose;
  readonly active: boolean;
}

export interface LlmProviderView {
  readonly id: string;
  readonly name: string;
  readonly endpointUrl: string;
  readonly modelName: string;
  readonly apiKeySecretRef: string | null;
  readonly apiKeySet: boolean;
  readonly apiType: string;
  readonly lastTestResult: 'success' | 'failed' | null;
  readonly lastTestMessage: string | null;
  readonly lastTestedAt: string | null;
  readonly usages: readonly LlmUsageView[];
}

export interface LlmProviderRequest {
  name: string;
  endpointUrl: string;
  modelName: string;
  apiKeySecretRef: string | null;
  apiKey: string | null;
  clearApiKey?: boolean;
  apiType: string | null;
}

export interface SttProviderView {
  readonly id: string;
  readonly name: string;
  readonly endpointUrl: string;
  readonly modelName: string;
  readonly apiKeySecretRef: string | null;
  readonly apiKeySet: boolean;
  readonly active: boolean;
  readonly lastTestResult: 'success' | 'failed' | null;
  readonly lastTestMessage: string | null;
  readonly lastTestedAt: string | null;
}

export interface SttProviderRequest {
  name: string;
  endpointUrl: string;
  modelName: string;
  apiKeySecretRef: string | null;
  apiKey: string | null;
  clearApiKey?: boolean;
}

export interface PromptTemplateView {
  readonly id: string;
  readonly purpose: LlmPurpose;
  readonly content: string;
  readonly version: number;
  readonly active: boolean;
  readonly createdAt: string;
}

export interface PlaceholderInfo {
  readonly purpose: LlmPurpose;
  readonly required: readonly string[];
}
