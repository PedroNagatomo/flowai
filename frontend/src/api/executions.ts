import { api } from './client';

export interface Execution {
  id: string;
  workflowId: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  triggerPayload?: Record<string, unknown>;
  executionLog?: Record<string, unknown>;
  errorMessage?: string;
  startedAt: string;
  finishedAt?: string;
  durationMs?: number;
}

export const executionsApi = {
  listByWorkflow: (workflowId: string) =>
    api.get<Execution[]>(`/workflows/${workflowId}/executions`).then((r) => r.data),

  runManually: (workflowId: string) =>
    api.post(`/workflows/${workflowId}/run`).then((r) => r.data),
};