import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useWorkflowStore } from "../store/workflowStore";
import { AppLayout } from "../components/layout/AppLayout";
import { PageHeader } from "../components/layout/PageHeader";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { PageLoader } from "../components/ui/Spinner";
import { Modal } from "../components/ui/Modal";
import type { Workflow } from "../types";

export function DashboardPage() {
  const navigate = useNavigate();
  const { workflows, loading, fetchAll, remove, update } = useWorkflowStore();
  const [deleteTarget, setDeleteTarget] = useState<Workflow | null>(null);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  async function handleToggle(w: Workflow) {
    await update(w.id, { isActive: !w.isActive });
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    await remove(deleteTarget.id);
    setDeleteTarget(null);
  }

  return (
    <AppLayout>
      <PageHeader
        title="Workflows"
        subtitle={`${workflows.length} automaç${workflows.length === 1 ? "ão" : "ões"} criadas`}
        actions={
          <Button onClick={() => navigate("/workflows/new")}>
            + Nova automação
          </Button>
        }
      />

      <div className="p-8">
        {loading && workflows.length === 0 ? (
          <div className="flex justify-center py-16">
            <PageLoader />
          </div>
        ) : workflows.length === 0 ? (
          <EmptyState
            icon="⚡"
            title="Nenhum workflow ainda"
            description="Crie sua primeira automação descrevendo o que você quer em linguagem natural."
            action={
              <Link to="/workflows/new" className="btn-primary">
                Criar primeira automação
              </Link>
            }
          />
        ) : (
          <div className="grid gap-4">
            {workflows.map((w) => (
              <WorkflowCard
                key={w.id}
                workflow={w}
                onToggle={() => handleToggle(w)}
                onDelete={() => setDeleteTarget(w)}
              />
            ))}
          </div>
        )}
      </div>

      <Modal
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="Excluir workflow?"
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleteTarget(null)}>
              Cancelar
            </Button>
            <Button variant="danger" onClick={handleDelete}>
              Excluir
            </Button>
          </>
        }
      >
        <p className="text-gray-400">
          Tem certeza que quer excluir{" "}
          <strong className="text-gray-200">{deleteTarget?.name}</strong>? Essa
          ação não pode ser desfeita.
        </p>
      </Modal>
    </AppLayout>
  );
}

function WorkflowCard({
  workflow,
  onToggle,
  onDelete,
}: {
  workflow: Workflow;
  onToggle: () => void;
  onDelete: () => void;
}) {
  const triggerType = workflow.definition?.trigger?.type ?? "—";
  const actionCount = workflow.definition?.actions?.length ?? 0;
  const created = new Date(workflow.createdAt).toLocaleDateString("pt-BR");

  return (
    <div className="card flex items-start justify-between gap-4 hover:border-accent/30 transition-colors">
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-3 mb-1">
          <Link
            to={`/workflows/${workflow.id}/detail`}
            className="text-lg font-semibold hover:text-accent transition-colors truncate"
          >
            {workflow.name}
          </Link>
          <span
            className={workflow.isActive ? "badge-active" : "badge-inactive"}
          >
            {workflow.isActive ? "● Ativo" : "○ Inativo"}
          </span>
        </div>
        {workflow.description && (
          <p className="text-sm text-gray-400 mb-2">{workflow.description}</p>
        )}
        <div className="flex items-center gap-4 text-xs text-gray-500">
          <span>
            Trigger: <span className="text-gray-300">{triggerType}</span>
          </span>
          <span>·</span>
          <span>
            {actionCount} aç{actionCount === 1 ? "ão" : "ões"}
          </span>
          <span>·</span>
          <span>Criado em {created}</span>
        </div>
      </div>

      <div className="flex items-center gap-2 shrink-0">
        <button
          onClick={onToggle}
          className={`relative w-10 h-6 rounded-full transition-colors ${
            workflow.isActive ? "bg-accent" : "bg-gray-700"
          }`}
          aria-label={workflow.isActive ? "Desativar" : "Ativar"}
        >
          <span
            className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full transition-transform ${
              workflow.isActive ? "translate-x-4" : ""
            }`}
          />
        </button>
        <Link
          to={`/workflows/${workflow.id}/detail`}
          className="btn-ghost text-sm px-2"
        >
          Detalhes
        </Link>
        <Link
          to={`/workflows/${workflow.id}`}
          className="btn-ghost text-sm px-2"
        >
          Editar
        </Link>
        <button
          onClick={onDelete}
          className="btn-ghost text-sm px-2 text-danger hover:bg-danger/10"
        >
          Excluir
        </button>
      </div>
    </div>
  );
}
