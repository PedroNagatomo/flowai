import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage.tsx";
import { WorkflowEditorPage } from "./pages/WorkflowEditorPage.tsx";
import { SettingsPage } from "./pages/SettingsPage.tsx";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { WorkflowDetailPage } from "./pages/WorkflowDetailPage.tsx";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/workflows/new"
          element={
            <ProtectedRoute>
              <WorkflowEditorPage mode="create" />
            </ProtectedRoute>
          }
        />
        <Route
          path="/workflows/:id"
          element={
            <ProtectedRoute>
              <WorkflowEditorPage mode="edit" />
            </ProtectedRoute>
          }
        />
        <Route
          path="/settings"
          element={
            <ProtectedRoute>
              <SettingsPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/workflows/:id/detail"
          element={
            <ProtectedRoute>
              <WorkflowDetailPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/workflows/:id/detail"
          element={
            <ProtectedRoute>
              <WorkflowDetailPage />
            </ProtectedRoute>
          }
        />

        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
