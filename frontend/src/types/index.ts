export interface User {
  id: string;
  email: string;
  name: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  name: string;
}

export interface WorkflowDefinition {
  trigger: {
    type: 'WEBHOOK' | 'SCHEDULE' | 'EMAIL_RECEIVED' | 'MANUAL';
    config: Record<string, unknown>;
  };
  conditions?: Array<{
    field: string;
    operator: string;
    value: unknown;
  }>;
  actions: Array<{
    type: string;
    config: Record<string, unknown>;
  }>;
}

export interface Workflow {
  id: string;
  name: string;
  description?: string;
  definition: WorkflowDefinition;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWorkflowRequest {
  name: string;
  description?: string;
  definition: WorkflowDefinition;
}

export interface UpdateWorkflowRequest {
  name?: string;
  description?: string;
  definition?: WorkflowDefinition;
  isActive?: boolean;
}