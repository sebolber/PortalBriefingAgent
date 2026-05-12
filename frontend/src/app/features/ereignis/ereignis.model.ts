export interface SummaryView {
  readonly id: string;
  readonly audienceType: 'person' | 'persongroup' | 'topic';
  readonly audienceName: string;
  readonly summaryText: string;
}

export interface EreignisResponse {
  readonly id: string;
  readonly sourceType: 'audio' | 'text';
  readonly reviewStatus: 'pending' | 'reviewed' | 'released';
  readonly transcript: string | null;
  readonly summaries: readonly SummaryView[];
}

export const TEXT_HARD_CAP_CHARS = 10_000;
