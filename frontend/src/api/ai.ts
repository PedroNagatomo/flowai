import { api } from './client';
import type { WorkflowDefinition } from '../types';

export interface GeneratedWorkflow {
  name: string;
  description: string;
  definition: WorkflowDefinition;
}

export const aiApi = {
  generate: (prompt: string) =>
    api
      .post<GeneratedWorkflow>('/workflows/generate', { prompt })
      .then((r) => r.data),
};