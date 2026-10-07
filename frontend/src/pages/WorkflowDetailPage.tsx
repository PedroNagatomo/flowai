import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { workflowsApi } from '../api/workflows';
import { executionsApi, type Execution } from '../api/executions';
import { getErrorMessage } from '../api/client';
import { AppLayout } from '../components/layout/AppLayout';
import { PageHeader } from '../components/layout/PageHeader';
import { Button } from '../components/ui/Button';
import { PageLoader } from '../components/ui/Spinner';
import { EmptyState } from '../components/ui/EmptyState';
import type { Workflow } from '../types';

export function WorkflowDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [workflow, setWorkflow] = useState<Workflow | null>(null);
  const [executions, setExecutions] = useState<Execution[]>([]);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  async function reload() {
    if (!id) return;
    try {
      const [w, e] = await Promise.all([
        workflowsApi.get(id),
        executionsApi.listByWorkflow(id),
      ]);
      setWorkflow(w);
      setExecutions(e);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    reload();
    // Auto-refresh a cada 5s pra ver execuções novas
    const t = setInterval(reload, 5000);
    return () => clearInterval(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function handleRun() {
    if (!id) return;
    setRunning(true);
    try {
      await executionsApi.runManually(id);
      // Espera um pouco e recarrega
      setTimeout(reload, 1000);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setRunning(false);
    }
  }

  function copyWebhook() {
    if (!workflow) return;
    const url = `${window.location.origin.replace(':5173', ':8080')}/api/webhooks/${workflow.id}`;
    navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  if (loading) return <PageLoader />;
  if (!workflow) {
    return (
      <AppLayout>
        <div className="p-8">
          <EmptyState
            title="Workflow não encontrado"
            action={
              <Link to="/dashboard" className="btn-primary">
                Voltar ao dashboard
              </Link>
            }
          />
        </div>
      </AppLayout>
    );
  }

  const webhookUrl = `${window.location.origin.replace(':5173', ':8080')}/api/webhooks/${workflow.id}`;
  const triggerType = workflow.definition?.trigger?.type ?? '—';
  const supportsWebhook = triggerType === 'WEBHOOK';
  const supportsManual = triggerType === 'MANUAL';

  return (
    <AppLayout>
      <PageHeader
        title={workflow.name}
        subtitle={workflow.description}
        actions={
          <>
            <Link to={`/workflows/${workflow.id}`} className="btn-secondary">
              Editar
            </Link>
            {(supportsManual || supportsWebhook) && (
              <Button onClick={handleRun} loading={running}>
                ▶ Executar agora
              </Button>
            )}
          </>
        }
      />

      <div className="p-8 max-w-4xl space-y-6">
        {error && (
          <div className="bg-danger/10 border border-danger/30 text-danger text-sm rounded-md p-3">
            {error}
          </div>
        )}

        {/* Webhook URL */}
        {supportsWebhook && (
          <div className="card">
            <h3 className="font-semibold mb-2 flex items-center gap-2">
              <span>🔗</span> URL do Webhook
            </h3>
            <p className="text-sm text-gray-400 mb-3">
              Faça um <code className="bg-bg px-1.5 py-0.5 rounded text-xs">POST</code> pra essa URL
              que o workflow dispara automaticamente. Ative o workflow primeiro (toggle no dashboard).
            </p>
            <div className="flex gap-2">
              <input
                readOnly
                value={webhookUrl}
                className="input font-mono text-xs flex-1"
                onClick={(e) => e.currentTarget.select()}
              />
              <Button variant="secondary" onClick={copyWebhook}>
                {copied ? '✓ Copiado' : 'Copiar'}
              </Button>
            </div>

            <details className="mt-3 text-xs text-gray-500">
              <summary className="cursor-pointer hover:text-gray-300">Exemplo com curl</summary>
              <pre className="bg-bg p-3 rounded mt-2 overflow-auto text-gray-300">
{`curl -X POST ${webhookUrl} \\
  -H "Content-Type: application/json" \\
  -d '{"id": "123", "nome": "Pedro"}'`}
              </pre>
            </details>
          </div>
        )}

        {/* Status */}
        <div className="card">
          <h3 className="font-semibold mb-4">Status</h3>
          <div className="grid grid-cols-3 gap-4 text-sm">
            <div>
              <p className="text-gray-500 text-xs mb-1">Trigger</p>
              <p className="font-medium">{triggerType}</p>
            </div>
            <div>
              <p className="text-gray-500 text-xs mb-1">Ações</p>
              <p className="font-medium">{workflow.definition?.actions?.length ?? 0}</p>
            </div>
            <div>
              <p className="text-gray-500 text-xs mb-1">Ativo</p>
              <span className={workflow.isActive ? 'badge-active' : 'badge-inactive'}>
                {workflow.isActive ? '● Sim' : '○ Não'}
              </span>
            </div>
          </div>
        </div>

        {/* Definition */}
        <details className="card">
          <summary className="cursor-pointer font-semibold">📄 Definição (JSON)</summary>
          <pre className="bg-bg p-3 rounded mt-3 overflow-auto text-xs text-gray-300">
            {JSON.stringify(workflow.definition, null, 2)}
          </pre>
        </details>

        {/* Histórico de execuções */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold">📊 Execuções recentes</h3>
            <span className="text-xs text-gray-500">{executions.length} no total</span>
          </div>

          {executions.length === 0 ? (
            <p className="text-sm text-gray-500 py-6 text-center">
              Nenhuma execução ainda. {supportsWebhook && 'Dispare o webhook ou '}
              clique em "Executar agora" pra testar.
            </p>
          ) : (
            <div className="space-y-2">
              {executions.map((exec) => (
                <ExecutionRow key={exec.id} execution={exec} />
              ))}
            </div>
          )}
        </div>
      </div>
    </AppLayout>
  );
}

function ExecutionRow({ execution }: { execution: Execution }) {
  const [expanded, setExpanded] = useState(false);

  const statusColor = {
    SUCCESS: 'text-success bg-success/10 border-success/30',
    FAILED: 'text-danger bg-danger/10 border-danger/30',
    PENDING: 'text-warning bg-warning/10 border-warning/30',
  }[execution.status];

  const date = new Date(execution.startedAt).toLocaleString('pt-BR');

  return (
    <div className="border border-border rounded-md overflow-hidden">
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between gap-3 p-3 hover:bg-surfaceLight/50 transition-colors text-left"
      >
        <div className="flex items-center gap-3 flex-1 min-w-0">
          <span className={`badge border ${statusColor}`}>{execution.status}</span>
          <span className="text-xs text-gray-500">{date}</span>
          {execution.durationMs != null && (
            <span className="text-xs text-gray-500">{execution.durationMs}ms</span>
          )}
        </div>
        <span className="text-gray-500 text-sm">{expanded ? '▲' : '▼'}</span>
      </button>

      {expanded && (
        <div className="border-t border-border p-3 space-y-3 text-xs bg-bg/50">
          {execution.errorMessage && (
            <div>
              <p className="text-gray-500 mb-1">Erro:</p>
              <p className="text-danger font-mono">{execution.errorMessage}</p>
            </div>
          )}
          {execution.triggerPayload && (
            <div>
              <p className="text-gray-500 mb-1">Payload recebido:</p>
              <pre className="bg-bg p-2 rounded overflow-auto text-gray-300">
                {JSON.stringify(execution.triggerPayload, null, 2)}
              </pre>
            </div>
          )}
          {execution.executionLog && (
            <div>
              <p className="text-gray-500 mb-1">Log de execução:</p>
              <pre className="bg-bg p-2 rounded overflow-auto text-gray-300">
                {JSON.stringify(execution.executionLog, null, 2)}
              </pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
}