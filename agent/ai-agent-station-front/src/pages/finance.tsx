import React, { useMemo, useState } from 'react';
import { Layout, Nav, Typography } from '@douyinfe/semi-ui';
import { IconCreditCard, IconList } from '@douyinfe/semi-icons';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import styled from 'styled-components';

import { Sidebar, Header } from '../components/layout';
import { theme } from '../styles/theme';

const { Content } = Layout;
const { Title, Text } = Typography;

const PageLayout = styled(Layout)`
  min-height: 100vh;
  background: ${theme.colors.bg.secondary};
`;

const MainContent = styled.div<{ $collapsed: boolean }>`
  display: flex;
  flex: 1;
  margin-left: ${props => (props.$collapsed ? '80px' : '280px')};
  transition: margin-left ${theme.animation.duration.normal} ${theme.animation.easing.cubic};
`;

const ContentArea = styled(Content)`
  flex: 1;
  padding: ${theme.spacing.lg};
  background: ${theme.colors.bg.secondary};
  overflow-y: auto;
`;

const InnerShell = styled.div`
  display: flex;
  gap: ${theme.spacing.lg};
  min-height: calc(100vh - 72px - ${theme.spacing.lg} * 2);
`;

const InnerSider = styled.div`
  width: 240px;
  flex-shrink: 0;
  background: ${theme.colors.bg.primary};
  border: 1px solid ${theme.colors.border.secondary};
  border-radius: ${theme.borderRadius.lg};
  overflow: hidden;
`;

const InnerContent = styled.div`
  flex: 1;
  min-width: 0;
`;

const SiderHeader = styled.div`
  padding: ${theme.spacing.lg};
  border-bottom: 1px solid ${theme.colors.border.secondary};
  background: ${theme.colors.bg.primary};
`;

export const FinancePage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);

  const selectedKey = useMemo(() => {
    if (location.pathname.includes('/finance/orders')) return 'orders';
    return 'assets';
  }, [location.pathname]);

  const handleLogout = () => {
    document.cookie = 'loginToken=;expires=Thu, 01 Jan 1970 00:00:01 GMT;path=/';
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    localStorage.removeItem('isLoggedIn');
    navigate('/login');
  };

  const handleNavigation = (keyOrPath: string) => {
    switch (keyOrPath) {
      case 'dashboard':
        navigate('/dashboard');
        break;
      case 'agent-list':
        navigate('/agent-list');
        break;
      case 'agent-config':
        navigate('/agent-config');
        break;
      case 'client-management':
        navigate('/client-management');
        break;
      case 'ai-client-api-management':
        navigate('/ai-client-api-management');
        break;
      case 'advisor-management':
        navigate('/advisor-management');
        break;
      case 'rag-order-management':
        navigate('/rag-order-management');
        break;
      case 'client-model-management':
        navigate('/client-model-management');
        break;
      case 'client-system-prompt-management':
        navigate('/client-system-prompt-management');
        break;
      case 'client-tool-mcp-management':
        navigate('/client-tool-mcp-management');
        break;
      default:
        navigate(keyOrPath);
        break;
    }
  };

  return (
    <PageLayout>
      <Sidebar selectedKey="dashboard" onSelect={handleNavigation} collapsed={collapsed} />
      <MainContent $collapsed={collapsed}>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          <Header onToggleSidebar={() => setCollapsed(!collapsed)} onLogout={handleLogout} collapsed={collapsed} />
          <ContentArea>
            <InnerShell>
              <InnerSider>
                <SiderHeader>
                  <Title heading={5} style={{ margin: 0 }}>
                    计费 / 资产
                  </Title>
                  <Text type="tertiary" style={{ display: 'block', marginTop: 4 }}>
                    查看余额与下发流水
                  </Text>
                </SiderHeader>
                <Nav
                  style={{ border: 'none' }}
                  selectedKeys={[selectedKey]}
                  onSelect={({ selectedKeys }: { selectedKeys: string[] }) => {
                    const key = selectedKeys[0];
                    if (key === 'assets') navigate('/finance/assets');
                    if (key === 'orders') navigate('/finance/orders');
                  }}
                  items={[
                    { itemKey: 'assets', text: '资源包/额度', icon: <IconCreditCard /> },
                    { itemKey: 'orders', text: '订单管理', icon: <IconList /> },
                  ]}
                />
              </InnerSider>
              <InnerContent>
                <Outlet />
              </InnerContent>
            </InnerShell>
          </ContentArea>
        </div>
      </MainContent>
    </PageLayout>
  );
};

