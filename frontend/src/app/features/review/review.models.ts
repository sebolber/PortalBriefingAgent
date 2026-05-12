export interface SummaryHistoryEntry {
  readonly changeType: 'manual_edit' | 'regenerated';
  readonly changedAt: string;
  readonly changedByAuthorId: string | null;
  readonly previousText: string;
  readonly newText: string;
  readonly feedback: string | null;
}

export interface SummaryDetail {
  readonly id: string;
  readonly audienceType: 'person' | 'persongroup' | 'topic';
  readonly summaryText: string;
  readonly editState: 'ai_generated' | 'manually_edited' | 'regenerated';
  readonly acceptedAt: string | null;
  readonly history: readonly SummaryHistoryEntry[];
}

export interface EreignisSummaryView {
  readonly id: string;
  readonly audienceType: 'person' | 'persongroup' | 'topic';
  readonly audienceName: string;
  readonly summaryText: string;
}

export interface EreignisDetail {
  readonly id: string;
  readonly sourceType: 'audio' | 'text';
  readonly reviewStatus: 'pending' | 'reviewed' | 'released';
  readonly transcript: string | null;
  readonly summaries: readonly EreignisSummaryView[];
}
