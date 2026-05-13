export interface DashboardSummary {
  readonly id: string;
  readonly audienceType: 'person' | 'persongroup' | 'topic';
  readonly audienceName: string;
  readonly confidence: 'low' | 'medium' | 'high' | null;
  readonly reasoning: string | null;
  readonly summaryExcerpt: string | null;
}

export interface DashboardEntry {
  readonly id: string;
  readonly createdAt: string;
  readonly sourceType: 'audio' | 'text';
  readonly transcriptExcerpt: string | null;
  readonly summaries: readonly DashboardSummary[];
}

export interface DashboardResponse {
  readonly windowDays: number;
  readonly ereignisse: readonly DashboardEntry[];
}
