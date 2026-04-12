import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { 
  Layout, 
  Table, 
  Button, 
  Input, 
  Space, 
  Typography, 
  Toast,
  Tag,
  Card,
  Modal,
  Dropdown,
  Tooltip,
  ButtonGroup
} from '@douyinfe/semi-ui';
const { confirm } = Modal;
import { 
  IconSearch, 
  IconPlus, 
  IconEyeOpened, 
  IconEdit, 
  IconDelete,
  IconPlay,
  IconMore
} from '@douyinfe/semi-icons';
import styled from 'styled-components';
import { theme } from '../styles/theme';
import { Sidebar, Header } from '../components/layout';
import { useChatDrawer } from '../contexts/chat-drawer-context';
import { AiAgentDrawService, AiAgentDrawConfigResponseDTO, AiAgentDrawConfigQueryRequestDTO } from '../services/ai-agent-draw-service';
import { AiAgentService } from '../services/ai-agent-service';
const { Content } = Layout;
const { Title } = Typography;

// 样式组件
const AgentListLayout = styled(Layout)`
  min-height: 100vh;
  background: ${theme.colors.bg.secondary};
`;

const MainContent = styled.div<{ $collapsed: boolean }>`
  display: flex;
  flex: 1;
  margin-left: ${props => props.$collapsed ? '80px' : '280px'}; /* 根据 Sidebar 状态调整左边距 */
  transition: margin-left ${theme.animation.duration.normal} ${theme.animation.easing.cubic};
`;

const ContentArea = styled(Content)`
  flex: 1;
  padding: ${theme.spacing.lg};
  background: ${theme.colors.bg.secondary};
  overflow-y: auto;
`;

const PageHeader = styled.div`
  margin-bottom: ${theme.spacing.lg};
`;

const SearchSection = styled(Card)`
  margin-bottom: ${theme.spacing.lg};
  
  .semi-card-body {
    padding: ${theme.spacing.lg};
  }
`;

const SearchRow = styled.div`
  display: flex;
  align-items: center;
  gap: ${theme.spacing.base};
  flex-wrap: wrap;
`;

const TableCard = styled(Card)`
  .semi-card-body {
    padding: 0;
  }
  .semi-table-tbody .semi-table-row {
    transition: background-color ${theme.animation.duration.fast} ${theme.animation.easing.ease};
  }
  .semi-table-tbody .semi-table-row:hover {
    background-color: ${theme.colors.bg.tertiary} !important;
  }
  .semi-table-row {
    height: 48px;
  }
  .semi-table-cell {
    padding: ${theme.spacing.sm} ${theme.spacing.base};
  }
`;

const ConfigNameCell = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
  .config-name {
    font-weight: ${theme.typography.fontWeight.medium};
    color: ${theme.colors.text.primary};
  }
  .config-id {
    font-size: ${theme.typography.fontSize.xs};
    color: #999;
    cursor: pointer;
    user-select: none;
    &:hover {
      color: ${theme.colors.primary};
    }
  }
`;

const DescriptionCell = styled.div`
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StatusTagDot = styled.span<{ $enabled: boolean }>`
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: ${props => props.$enabled ? theme.colors.success : theme.colors.error};
  margin-right: 6px;
  vertical-align: middle;
`;

interface AgentListPageProps {
  selectedKey?: string;
  onMenuSelect?: (key: string) => void;
}

export const AgentListPage: React.FC<AgentListPageProps> = ({
  selectedKey = 'agent-list',
  onMenuSelect
}) => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [collapsed, setCollapsed] = useState(false);

  // 处理侧边栏导航
  const handleNavigation = (path: string) => {
    switch (path) {
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
        navigate(path);
        break;
    }
  };

  // 处理退出登录
  const handleLogout = () => {
    document.cookie = 'loginToken=;expires=Thu, 01 Jan 1970 00:00:01 GMT;path=/';
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    localStorage.removeItem('isLoggedIn');
    navigate('/login');
  };
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<AiAgentDrawConfigResponseDTO[]>([]);
  const [listQuery, setListQuery] = useState<AiAgentDrawConfigQueryRequestDTO>({
    pageNum: 1,
    pageSize: 10
  });
  const [total, setTotal] = useState(0);

  const { openDrawer } = useChatDrawer();
  const testRunConfigId = searchParams.get('testRunConfigId');
  useEffect(() => {
    if (!testRunConfigId) return;
    AiAgentDrawService.getDrawConfig(testRunConfigId)
      .then((config) => {
        if (config) {
          openDrawer({
            initialAgentId: config.agentId,
            initialConfig: {
              configId: config.configId,
              configName: config.configName || config.configId,
              agentId: config.agentId,
            },
          });
        }
        const next = new URLSearchParams(searchParams);
        next.delete('testRunConfigId');
        setSearchParams(next, { replace: true });
      })
      .catch(() => {
        const next = new URLSearchParams(searchParams);
        next.delete('testRunConfigId');
        setSearchParams(next, { replace: true });
      });
  }, [testRunConfigId, openDrawer, searchParams]);

  // 复制配置ID到剪贴板
  const copyConfigId = (configId: string) => {
    navigator.clipboard.writeText(configId).then(
      () => Toast.success('配置ID已复制'),
      () => Toast.error('复制失败')
    );
  };

  // 表格列定义
  const columns = [
    {
      title: '配置名称',
      key: 'configNameId',
      width: 200,
      render: (_: unknown, record: AiAgentDrawConfigResponseDTO) => (
        <ConfigNameCell>
          <span className="config-name">{record.configName || '-'}</span>
          <Tooltip content="点击复制">
            <span
              className="config-id"
              onClick={(e) => { e.stopPropagation(); copyConfigId(record.configId); }}
            >
              {record.configId}
            </span>
          </Tooltip>
        </ConfigNameCell>
      )
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 220,
      render: (text: string) => {
        const content = text || '-';
        return (
          <Tooltip content={content}>
            <DescriptionCell>{content}</DescriptionCell>
          </Tooltip>
        );
      }
    },
    {
      title: '智能体ID',
      dataIndex: 'agentId',
      key: 'agentId',
      width: 100,
      render: (text: string) => (
        <Typography.Text ellipsis={{ showTooltip: true }} style={{ fontFamily: 'monospace', fontSize: '12px' }}>
          {text || '-'}
        </Typography.Text>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 88,
      render: (status: number) => {
        const enabled = status === 1;
        return (
          <Tag color={enabled ? 'green' : 'red'}>
            <StatusTagDot $enabled={enabled} />
            {enabled ? '启用' : '禁用'}
          </Tag>
        );
      }
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right' as const,
      render: (_: unknown, record: AiAgentDrawConfigResponseDTO) => (
        <Space>
          <Button
            theme="borderless"
            type="tertiary"
            size="small"
            icon={<IconPlay />}
            onClick={() => {
              openDrawer({
                initialAgentId: record.agentId,
                initialConfig: {
                  configId: record.configId,
                  configName: record.configName || record.configId,
                  agentId: record.agentId,
                },
              });
            }}
          >
            测试运行
          </Button>
          <Dropdown
            trigger="click"
            position="bottomRight"
            render={
              <Dropdown.Menu>
                <Dropdown.Item icon={<IconEyeOpened />} onClick={() => handleView(record)}>
                  查看
                </Dropdown.Item>
                <Dropdown.Item icon={<IconEdit />} onClick={() => handleEdit(record)}>
                  修改
                </Dropdown.Item>
                <Dropdown.Item onClick={() => handleLoad(record)}>
                  加载
                </Dropdown.Item>
                <Dropdown.Item
                  type="danger"
                  icon={<IconDelete />}
                  onClick={() => {
                    confirm({
                      title: '确定要删除这个配置吗？',
                      content: '删除后无法恢复，请谨慎操作',
                      onOk: () => handleDelete(record),
                    });
                  }}
                >
                  删除
                </Dropdown.Item>
              </Dropdown.Menu>
            }
          >
            <Button type="tertiary" size="small" icon={<IconMore />} />
          </Dropdown>
        </Space>
      )
    }
  ];

  // 加载数据
  const loadData = async (params?: AiAgentDrawConfigQueryRequestDTO) => {
    setLoading(true);
    try {
      const queryParams = { ...listQuery, ...params };
      const data = await AiAgentDrawService.queryDrawConfigList(queryParams);
      setDataSource(data);
      // 注意：后端返回的是简单分页，这里设置一个估算的总数
      setTotal(data.length >= (queryParams.pageSize || 10) ? (queryParams.pageNum || 1) * (queryParams.pageSize || 10) + 1 : data.length);
    } catch (error) {
      Toast.error('加载数据失败');
      console.error('加载数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 搜索
  const handleSearch = () => {
    const params = { ...listQuery, pageNum: 1 };
    setListQuery(params);
    loadData(params);
  };

  // 重置搜索
  const handleReset = () => {
    const params = { pageNum: 1, pageSize: 10 };
    setListQuery(params);
    loadData(params);
  };

  // 分页变化
  const handlePageChange = (pageNum: number, pageSize: number) => {
    const params = { ...listQuery, pageNum, pageSize };
    setListQuery(params);
    loadData(params);
  };

  // 新建
  const handleCreate = () => {
    // 跳转到 agent-config 页面，新建模式
    navigate('/agent-config');
  };

  // 查看
  const handleView = (record: AiAgentDrawConfigResponseDTO) => {
    // 跳转到 agent-config 页面，传递 configId 参数，并添加 mode=view 表示只读模式
    navigate(`/agent-config?configId=${record.configId}&mode=view`);
  };

  // 编辑
  const handleEdit = (record: AiAgentDrawConfigResponseDTO) => {
    // 跳转到 agent-config 页面，传递 configId 参数，默认为编辑模式
    navigate(`/agent-config?configId=${record.configId}`);
  };

  // 加载 - 装配智能体
  const handleLoad = async (record: AiAgentDrawConfigResponseDTO) => {
    try {
      setLoading(true);
      Toast.info(`正在装配智能体: ${record.configName}`);
      
      const success = await AiAgentService.armoryAgent(record.agentId);
      
      if (success) {
        Toast.success(`智能体 ${record.configName} 装配成功！`);
      } else {
        Toast.error(`智能体 ${record.configName} 装配失败`);
      }
    } catch (error) {
      console.error('装配智能体失败:', error);
      Toast.error(`装配失败: ${error instanceof Error ? error.message : '未知错误'}`);
    } finally {
      setLoading(false);
    }
  };

  // 删除
  const handleDelete = async (record: AiAgentDrawConfigResponseDTO) => {
    try {
      setLoading(true);
      const success = await AiAgentDrawService.deleteDrawConfig(record.configId);
      if (success) {
        Toast.success(`成功删除配置: ${record.configName}`);
        // 重新加载数据
        await loadData();
      } else {
        Toast.error('删除配置失败');
      }
    } catch (error) {
      Toast.error('删除配置失败');
      console.error('删除配置失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 初始化加载数据
  useEffect(() => {
    loadData();
  }, []);

  return (
    <AgentListLayout>
      <Sidebar 
        selectedKey={selectedKey} 
        onSelect={handleNavigation}
        collapsed={collapsed}
      />
      <MainContent $collapsed={collapsed}>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          <Header
            onToggleSidebar={() => setCollapsed(!collapsed)}
            onLogout={handleLogout}
            collapsed={collapsed}
          />
          <ContentArea>
            <PageHeader>
              <Title heading={3}>代理列表</Title>
            </PageHeader>

            <SearchSection>
              <SearchRow>
                <Input
                  placeholder="配置名称"
                  value={listQuery.configName || ''}
                  onChange={(value) => setListQuery(prev => ({ ...prev, configName: value }))}
                  style={{ width: 200 }}
                  prefix={<IconSearch />}
                  size="default"
                  clearable
                />
                <Input
                  placeholder="智能体ID"
                  value={listQuery.agentId || ''}
                  onChange={(value) => setListQuery(prev => ({ ...prev, agentId: value }))}
                  style={{ width: 200 }}
                  size="default"
                  clearable
                />
                <ButtonGroup>
                  <Button
                    type="primary"
                    icon={<IconSearch />}
                    onClick={handleSearch}
                    loading={loading}
                  >
                    搜索
                  </Button>
                  <Button onClick={handleReset}>重置</Button>
                </ButtonGroup>
                <div style={{ marginLeft: 'auto' }}>
                  <Button
                    type="primary"
                    icon={<IconPlus />}
                    onClick={handleCreate}
                  >
                    新建
                  </Button>
                </div>
              </SearchRow>
            </SearchSection>

            <TableCard>
              <Table
                columns={columns}
                dataSource={dataSource}
                loading={loading}
                pagination={{
                  currentPage: listQuery.pageNum || 1,
                  pageSize: listQuery.pageSize || 10,
                  total: total,
                  showSizeChanger: true,
                  showQuickJumper: true,
                  onChange: handlePageChange
                }}
                rowKey="configId"
                scroll={{ x: 900 }}
                size="small"
              />
            </TableCard>

          </ContentArea>
        </div>
      </MainContent>
    </AgentListLayout>
  );
};

export default AgentListPage;