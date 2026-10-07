import { api } from './client';

export interface Integration {
  id: string;
  type: string;
  name: string;
  config: Record<string, unknown>;
  isActive: boolean;
  createdAt: string;
}

export interface CreateIntegrationRequest {
  type: string;
  name: string;
  config: Record<string, unknown>;
}

export const integrationsApi = {
  list: () => api.get<Integration[]>('/integrations').then((r) => r.data),
  create: (data: CreateIntegrationRequest) =>
    api.post<Integration>('/integrations', data).then((r) => r.data),
  update: (id: string, data: CreateIntegrationRequest) =>
    api.put<Integration>(`/integrations/${id}`, data).then((r) => r.data),
  delete: (id: string) => api.delete(`/integrations/${id}`).then((r) => r.data),
};