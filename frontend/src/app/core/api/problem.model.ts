/**
 * RFC 7807 Problem Details payload returned by the backend.
 */
export interface ProblemDetails {
  readonly type: string;
  readonly title: string;
  readonly status: number;
  readonly instance?: string;
  readonly errors?: readonly string[];
}
