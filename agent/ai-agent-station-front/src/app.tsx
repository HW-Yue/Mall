import React from 'react';
import { createRoot } from 'react-dom/client';
import './styles/tailwind.css';
import { BrowserRouter as Router, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { LoginPage, DashboardPage, AgentConfigPage, AgentListPage, ClientManagement, AiClientApiManagement, AdvisorManagement, RagOrderManagement, ClientModelManagement, ClientSystemPromptManagement, ClientToolMcpManagement, FinancePage, FinanceAssetsPage, FinanceOrdersPage } from './pages';
import { ChatDrawerProvider } from './contexts/chat-drawer-context';
import { ChatDrawerChrome } from './components/chat-drawer/ChatDrawerChrome';

const getCookie = (name: string): string | null => {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
};

const isAuthenticated = (): boolean => {
  // 统一通过 cookie 中的 loginToken 判断是否已登录
  const loginToken = getCookie('loginToken');
  return !!loginToken;
};

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return isAuthenticated() ? <>{children}</> : <Navigate to="/login" replace />;
};

const LoginRedirect: React.FC = () => {
  return isAuthenticated() ? <Navigate to="/dashboard" replace /> : <LoginPage />;
};

const ProtectedLayout: React.FC = () => (
  <>
    <Outlet />
    <ChatDrawerChrome />
  </>
);

const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginRedirect />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <ChatDrawerProvider>
                <ProtectedLayout />
              </ChatDrawerProvider>
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="agent-config" element={<AgentConfigPage />} />
          <Route path="agent-list" element={<AgentListPage />} />
          <Route path="client-management" element={<ClientManagement />} />
          <Route path="ai-client-api-management" element={<AiClientApiManagement />} />
          <Route path="advisor-management" element={<AdvisorManagement />} />
          <Route path="rag-order-management" element={<RagOrderManagement />} />
          <Route path="client-model-management" element={<ClientModelManagement />} />
          <Route path="client-system-prompt-management" element={<ClientSystemPromptManagement />} />
          <Route path="client-tool-mcp-management" element={<ClientToolMcpManagement />} />
          <Route path="finance" element={<FinancePage />}>
            <Route index element={<Navigate to="assets" replace />} />
            <Route path="assets" element={<FinanceAssetsPage />} />
            <Route path="orders" element={<FinanceOrdersPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </Router>
  );
};

const app = createRoot(document.getElementById('root')!);

app.render(<App />);
