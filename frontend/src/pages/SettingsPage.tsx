import { AppLayout } from '../components/layout/AppLayout';
import { PageHeader } from '../components/layout/PageHeader';
import { useAuthStore } from '../store/authStore';

export function SettingsPage() {
  const user = useAuthStore((s) => s.user);

  return (
    <AppLayout>
      <PageHeader title="Configurações" subtitle="Preferências da conta" />

      <div className="p-8 max-w-2xl space-y-6">
        <div className="card">
          <h3 className="font-semibold mb-4">Conta</h3>
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between">
              <dt className="text-gray-400">Nome</dt>
              <dd>{user?.name}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-400">Email</dt>
              <dd>{user?.email}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-400">ID</dt>
              <dd className="font-mono text-xs">{user?.id}</dd>
            </div>
          </dl>
        </div>

        <div className="card">
          <h3 className="font-semibold mb-2">Integrações</h3>
          <p className="text-sm text-gray-500">
            Slack, Email e Webhooks serão configurados aqui na Fase 3.
          </p>
        </div>
      </div>
    </AppLayout>
  );
}