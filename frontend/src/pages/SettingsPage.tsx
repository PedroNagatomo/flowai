import { useEffect, useState } from 'react';
import { integrationsApi, type Integration } from '../api/integration';
import { getErrorMessage } from '../api/client';
import { useAuthStore } from '../store/authStore';
import { AppLayout } from '../components/layout/AppLayout';
import { PageHeader } from '../components/layout/PageHeader';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { EmptyState } from '../components/ui/EmptyState';

const INTEGRATION_TYPES = [
  {
    value: 'SLACK',
    label: 'Slack',
    icon: '💬',
    description: 'Recebe notificações via webhook do Slack',
    fields: [
      { key: 'webhookUrl', label: 'Webhook URL', placeholder: 'https://hooks.slack.com/services/...', type: 'url' },
      { key: 'defaultChannel', label: 'Canal padrão (opcional)', placeholder: '#geral', type: 'text' },
    ],
  },
  {
    value: 'SMTP',
    label: 'Email (SMTP)',
    icon: '📧',
    description: 'Envia emails através do seu servidor SMTP',
    fields: [
      { key: 'host', label: 'Host', placeholder: 'smtp.gmail.com', type: 'text' },
      { key: 'port', label: 'Porta', placeholder: '587', type: 'number' },
      { key: 'username', label: 'Usuário', placeholder: 'seu@email.com', type: 'text' },
      { key: 'password', label: 'Senha', placeholder: '••••••', type: 'password' },
    ],
  },
] as const;

export function SettingsPage() {
  const user = useAuthStore((s) => s.user);
  const [integrations, setIntegrations] = useState<Integration[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Integration | null>(null);

  async function load() {
    try {
      setIntegrations(await integrationsApi.list());
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  function openCreate() {
    setEditing(null);
    setModalOpen(true);
  }

  function openEdit(i: Integration) {
    setEditing(i);
    setModalOpen(true);
  }

  async function handleDelete(id: string) {
    if (!confirm('Excluir essa integração?')) return;
    await integrationsApi.delete(id);
    load();
  }

  return (
    <AppLayout>
      <PageHeader title="Configurações" subtitle="Conta e integrações" />

      <div className="p-8 max-w-3xl space-y-6">
        {/* Conta */}
        <div className="card">
          <h3 className="font-semibold mb-4">👤 Conta</h3>
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between">
              <dt className="text-gray-400">Nome</dt>
              <dd>{user?.name}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-400">Email</dt>
              <dd>{user?.email}</dd>
            </div>
          </dl>
        </div>

        {/* Integrações */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="font-semibold">🔌 Integrações</h3>
              <p className="text-sm text-gray-400 mt-1">
                Configure serviços que seus workflows podem usar.
              </p>
            </div>
            <Button onClick={openCreate}>+ Adicionar</Button>
          </div>

          {loading ? (
            <p className="text-gray-500 text-sm py-4">Carregando...</p>
          ) : integrations.length === 0 ? (
            <EmptyState
              icon="🔌"
              title="Nenhuma integração"
              description="Configure Slack ou SMTP pra usar nas ações dos workflows."
            />
          ) : (
            <div className="space-y-2">
              {integrations.map((i) => {
                const typeInfo = INTEGRATION_TYPES.find((t) => t.value === i.type);
                return (
                  <div
                    key={i.id}
                    className="flex items-center justify-between p-3 border border-border rounded-md"
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-2xl">{typeInfo?.icon ?? '🔌'}</span>
                      <div>
                        <p className="font-medium">{i.name}</p>
                        <p className="text-xs text-gray-500">{typeInfo?.label ?? i.type}</p>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <button onClick={() => openEdit(i)} className="btn-ghost text-sm px-2">
                        Editar
                      </button>
                      <button
                        onClick={() => handleDelete(i.id)}
                        className="btn-ghost text-sm px-2 text-danger hover:bg-danger/10"
                      >
                        Excluir
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      <IntegrationModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSaved={() => { setModalOpen(false); load(); }}
        integration={editing}
      />
    </AppLayout>
  );
}

function IntegrationModal({
  open,
  onClose,
  onSaved,
  integration,
}: {
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
  integration: Integration | null;
}) {
  const [type, setType] = useState('SLACK');
  const [name, setName] = useState('');
  const [config, setConfig] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const typeInfo = INTEGRATION_TYPES.find((t) => t.value === type)!;

  useEffect(() => {
    if (open) {
      setType(integration?.type ?? 'SLACK');
      setName(integration?.name ?? '');
      setConfig(
        integration
          ? Object.fromEntries(
              Object.entries(integration.config).map(([k, v]) => [k, String(v)])
            )
          : {}
      );
      setError('');
    }
  }, [open, integration]);

  async function handleSave() {
    setLoading(true);
    setError('');
    try {
      const data = { type, name, config };
      if (integration) {
        await integrationsApi.update(integration.id, data);
      } else {
        await integrationsApi.create(data);
      }
      onSaved();
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={integration ? 'Editar integração' : 'Nova integração'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button onClick={handleSave} loading={loading} disabled={!name.trim()}>
            Salvar
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        {error && (
          <div className="bg-danger/10 border border-danger/30 text-danger text-sm rounded-md p-3">
            {error}
          </div>
        )}

        <div>
          <label className="label">Tipo</label>
          <select
            value={type}
            onChange={(e) => setType(e.target.value)}
            className="input"
            disabled={!!integration}
          >
            {INTEGRATION_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.icon} {t.label}
              </option>
            ))}
          </select>
          <p className="text-xs text-gray-500 mt-1">{typeInfo.description}</p>
        </div>

        <Input
          label="Nome"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Ex: Slack da empresa"
          required
        />

        {typeInfo.fields.map((f) => (
          <Input
            key={f.key}
            label={f.label}
            type={f.type}
            value={config[f.key] ?? ''}
            onChange={(e) => setConfig({ ...config, [f.key]: e.target.value })}
            placeholder={f.placeholder}
          />
        ))}
      </div>
    </Modal>
  );
}