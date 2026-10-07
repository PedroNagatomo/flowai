import { api } from './client';
import type {
  Workflow,
  CreateWorkflowRequest,
  UpdateWorkflowRequest,
} from '../types';

export const workflowsApi = {
  list: () => api.get<Workflow[]>('/workflows').then((r) => r.data),

  get: (id: string) => api.get<Workflow>(`/workflows/${id}`).then((r) => r.data),

  create: (data: CreateWorkflowRequest) =>
    api.post<Workflow>('/workflows', data).then((r) => r.data),

  update: (id: string, data: UpdateWorkflowRequest) =>
    api.put<Workflow>(`/workflows/${id}`, data).then((r) => r.data),

  delete: (id: string) => api.delete(`/workflows/${id}`).then((r) => r.data),
};