export interface PersonView {
  readonly id: string;
  readonly fullName: string;
  readonly email: string | null;
  readonly role: string | null;
  readonly company: string | null;
  readonly tombstoned: boolean;
  readonly pseudonym: string | null;
}

export interface PersonRequest {
  fullName: string;
  email: string;
  role: string;
  company: string;
}

export interface PersonPersonaView {
  readonly id: string;
  readonly personId: string;
  readonly personaText: string;
}

export interface GroupView {
  readonly id: string;
  readonly name: string;
  readonly personaText: string;
  readonly summaryRetentionMonths: number | null;
  readonly memberIds: readonly string[];
}

export interface GroupRequest {
  name: string;
  personaText: string;
  summaryRetentionMonths: number | null;
  memberIds: string[];
}

export interface TopicView {
  readonly id: string;
  readonly name: string;
  readonly personaText: string;
  readonly summaryRetentionMonths: number | null;
  readonly memberIds: readonly string[];
}

export interface TopicRequest {
  name: string;
  personaText: string;
  summaryRetentionMonths: number | null;
  memberIds: string[];
}
