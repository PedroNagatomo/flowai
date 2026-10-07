import { create } from "zustand";
import type { Workflow } from "../types";
import { workflowsApi } from "../api/workflows";
import { aiApi, type GeneratedWorkflow } from "../api/ai";

interface WorkflowState {
  workflows: Workflow[];
  loading: boolean;
  error: string | null;
  fetchAll: () => Promise<void>;
  create: (
    data: Parameters<typeof workflowsApi.create>[0],
  ) => Promise<Workflow>;
  update: (
    id: string,
    data: Parameters<typeof workflowsApi.update>[1],
  ) => Promise<Workflow>;
  remove: (id: string) => Promise<void>;
  reset: () => void;
  generate: (prompt: string) => Promise<GeneratedWorkflow>;
}

export const useWorkflowStore = create<WorkflowState>((set, get) => ({
  workflows: [],
  loading: false,
  error: null,

  fetchAll: async () => {
    set({ loading: true, error: null });
    try {
      const data = await workflowsApi.list();
      set({ workflows: data, loading: false });
    } catch (e) {
      set({ error: "Falha ao carregar workflows", loading: false });
    }
  },

  create: async (data) => {
    const created = await workflowsApi.create(data);
    set({ workflows: [created, ...get().workflows] });
    return created;
  },

  update: async (id, data) => {
    const updated = await workflowsApi.update(id, data);
    set({
      workflows: get().workflows.map((w) => (w.id === id ? updated : w)),
    });
    return updated;
  },

  remove: async (id) => {
    await workflowsApi.delete(id);
    set({ workflows: get().workflows.filter((w) => w.id !== id) });
  },

  generate: async (prompt: string) => {
    return aiApi.generate(prompt);
  },

  reset: () => set({ workflows: [], loading: false, error: null }),
}));
