import { type FormEvent, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { workflowsApi } from "../api/workflows";
import { getErrorMessage } from "../api/client";
import { useWorkflowStore } from "../store/workflowStore";
import { AppLayout } from "../components/layout/AppLayout";
import { PageHeader } from "../components/layout/PageHeader";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { Textarea } from "../components/ui/Textarea";
import { PageLoader } from "../components/ui/Spinner";
import type { WorkflowDefinition } from "../types";
import { aiApi, type GeneratedWorkflow } from "../api/ai";

const TRIGGER_OPTIONS = [
  { value: "MANUAL", label: "Manual (botão)" },
  { value: "WEBHOOK", label: "Webhook (POST)" },
  { value: "SCHEDULE", label: "Agendado (cron)" },
  { value: "EMAIL_RECEIVED", label: "Email recebido" },
] as const;

const ACTION_OPTIONS = [
  { value: "SEND_EMAIL", label: "Enviar email" },
  { value: "SEND_SLACK", label: "Enviar Slack" },
  { value: "HTTP_REQUEST", label: "Requisição HTTP" },
] as const;

const DEFAULT_DEFINITION: WorkflowDefinition = {
  trigger: { type: "MANUAL", config: {} },
  actions: [{ type: "SEND_EMAIL", config: {} }],
};

interface Props {
  mode: "create" | "edit";
}

export function WorkflowEditorPage({ mode }: Props) {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const createWorkflow = useWorkflowStore((s) => s.create);
  const updateWorkflow = useWorkflowStore((s) => s.update);

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [definitionText, setDefinitionText] = useState(
    JSON.stringify(DEFAULT_DEFINITION, null, 2),
  );
  const [loading, setLoading] = useState(mode === "edit");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [jsonError, setJsonError] = useState("");
  const [aiPrompt, setAiPrompt] = useState("");
  const [generating, setGenerating] = useState(false);
  const [aiError, setAiError] = useState("");

  useEffect(() => {
    if (mode === "edit" && id) {
      workflowsApi
        .get(id)
        .then((w) => {
          setName(w.name);
          setDescription(w.description ?? "");
          setDefinitionText(JSON.stringify(w.definition, null, 2));
        })
        .catch((e) => setError(getErrorMessage(e)))
        .finally(() => setLoading(false));
    }
  }, [mode, id]);

  function validateJson(): WorkflowDefinition | null {
    try {
      const parsed = JSON.parse(definitionText) as WorkflowDefinition;
      if (!parsed.trigger || !parsed.actions) {
        setJsonError('Precisa ter "trigger" e "actions"');
        return null;
      }
      if (!Array.isArray(parsed.actions) || parsed.actions.length === 0) {
        setJsonError('"actions" precisa ter pelo menos 1 item');
        return null;
      }
      setJsonError("");
      return parsed;
    } catch (e) {
      setJsonError("JSON inválido: " + (e as Error).message);
      return null;
    }
  }

  async function handleGenerate() {
    setAiError("");
    setGenerating(true);
    try {
      const result: GeneratedWorkflow = await aiApi.generate(aiPrompt);
      setName(result.name);
      setDescription(result.description);
      setDefinitionText(JSON.stringify(result.definition, null, 2));
      setJsonError("");
    } catch (err) {
      setAiError(getErrorMessage(err));
    } finally {
      setGenerating(false);
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");

    const parsed = validateJson();
    if (!parsed) return;

    setSaving(true);
    try {
      if (mode === "create") {
        await createWorkflow({ name, description, definition: parsed });
      } else if (id) {
        await updateWorkflow(id, { name, description, definition: parsed });
      }
      navigate("/dashboard");
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  function formatJson() {
    const parsed = validateJson();
    if (parsed) setDefinitionText(JSON.stringify(parsed, null, 2));
  }

  if (loading) return <PageLoader />;

  return (
    <AppLayout>
      <PageHeader
        title={mode === "create" ? "Nova automação" : "Editar automação"}
        subtitle="Defina o trigger e as ações do workflow"
      />

      <form onSubmit={handleSubmit} className="p-8 max-w-3xl space-y-6">
        {error && (
          <div className="bg-danger/10 border border-danger/30 text-danger text-sm rounded-md p-3">
            {error}
          </div>
        )}

        {mode === "create" && (
          <div className="card border-accent/30 bg-accent/5">
            <div className="flex items-start gap-3">
              <span className="text-2xl">✨</span>
              <div className="flex-1">
                <h3 className="font-semibold mb-1">Gerar com IA</h3>
                <p className="text-sm text-gray-400 mb-3">
                  Descreva em linguagem natural o que você quer automatizar.
                </p>
                <Textarea
                  value={aiPrompt}
                  onChange={(e) => setAiPrompt(e.target.value)}
                  placeholder='Ex: "Todo dia às 9h me manda um email com o resumo do dia"'
                  rows={3}
                  className="!font-sans"
                />
                {aiError && (
                  <p className="text-danger text-sm mt-2">{aiError}</p>
                )}
                <div className="flex justify-end mt-3">
                  <Button
                    type="button"
                    onClick={handleGenerate}
                    loading={generating}
                    disabled={!aiPrompt.trim()}
                  >
                    ✨ Gerar workflow
                  </Button>
                </div>
              </div>
            </div>
          </div>
        )}

        <div className="card space-y-4">
          <Input
            label="Nome"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Ex: Notificar Slack quando chegar pedido"
            required
            maxLength={255}
          />

          <Input
            label="Descrição (opcional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Breve descrição do que essa automação faz"
          />
        </div>

        <div className="card space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold">Definição (JSON)</h3>
              <p className="text-xs text-gray-500 mt-0.5">
                Na Fase 2, isso vai ser gerado automaticamente por IA.
              </p>
            </div>
            <Button
              type="button"
              variant="secondary"
              onClick={formatJson}
              className="text-xs"
            >
              Formatar
            </Button>
          </div>

          <Textarea
            value={definitionText}
            onChange={(e) => setDefinitionText(e.target.value)}
            rows={20}
            spellCheck={false}
            error={jsonError}
          />

          <details className="text-sm">
            <summary className="cursor-pointer text-gray-400 hover:text-gray-200">
              Ver exemplo de JSON
            </summary>
            <pre className="bg-bg p-3 rounded mt-2 overflow-auto text-xs text-gray-300">
              {`{
  "trigger": {
    "type": "SCHEDULE",
    "config": { "cron": "0 9 * * *", "timezone": "America/Sao_Paulo" }
  },
  "actions": [
    {
      "type": "SEND_EMAIL",
      "config": {
        "to": "eu@empresa.com",
        "subject": "Bom dia",
        "body": "Resumo do dia: {{now}}"
      }
    }
  ]
}`}
            </pre>
          </details>
        </div>

        <div className="flex justify-end gap-2">
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate("/dashboard")}
          >
            Cancelar
          </Button>
          <Button type="submit" loading={saving}>
            {mode === "create" ? "Criar automação" : "Salvar alterações"}
          </Button>
        </div>
      </form>
    </AppLayout>
  );
}
