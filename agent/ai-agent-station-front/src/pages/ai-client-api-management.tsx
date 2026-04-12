import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Layout,
  Table, 
  Button, 
  Input, 
  Space, 
  Typography, 
  Toast,
  Card
} from '@douyinfe/semi-ui';
import { 
  IconSearch, 
  IconRefresh,
  IconPlus
} from '@douyinfe/semi-icons';
import {
  ResourceTableCard,
  NameIdCell,
  DescriptionCellWrap,
  StatusDotTag,
  UpdateTimeWithCreateTooltip,
  TableActionLinks,
} from '../components/resource-list-table';
import styled from 'styled-components';
import { theme } from '../styles/theme';
import { Sidebar, Header } from '../components/layout';
import { AiClientApiCreateModal } from '../components/ai-client-api-create-modal';
import { AiClientApiEditModal } from '../components/ai-client-api-edit-modal';
import { 
  aiClientApiAdminService, 
  AiClientApiQueryRequestDTO, 
  AiClientApiResponseDTO 
} from '../services/ai-client-api-admin-service';
import { AiAgentService } from '../services/ai-agent-service';

const { Content } = Layout;
const { Title } = Typography;

// 样式组件
const AiClientApiManagementLayout = styled(Layout)`
  min-height: 100vh;
  background: ${theme.colors.bg.secondary};
`;

const MainContent = styled.div<{ $collapsed: boolean }>`
  display: flex;
  flex: 1;
  margin-left: ${props => props.$collapsed ? '80px' : '280px'};
  transition: margin-left ${theme.animation.duration.normal} ${theme.animation.easing.cubic};
`;

const ContentArea = styled(Content)`
  flex: 1;
  padding: ${theme.spacing.lg};
  background: ${theme.colors.bg.secondary};
  overflow-y: auto;
`;

const AiClientApiManagementContainer = styled.div`
  height: 100%;
  display: flex;
  flex-direction: column;
`;

const PageHeader = styled.div`
  padding: ${theme.spacing.lg};
  border-bottom: 1px solid ${theme.colors.border.secondary};
`;

const SearchSection = styled(Card)`
  margin: ${theme.spacing.lg};
  
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

const TableContainer = styled.div`
  flex: 1;
  margin: 0 ${theme.spacing.lg} ${theme.spacing.lg};
  display: flex;
  flex-direction: column;
`;

const TableWrapper = styled.div`
  flex: 1;
  overflow: auto;
`;

export const AiClientApiManagement: React.FC = () => {
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<AiClientApiResponseDTO[]>([]);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [searchForm, setSearchForm] = useState<AiClientApiQueryRequestDTO>({
    apiId: '',
    status: undefined,
    pageNum: 1,
    pageSize: 10
  });
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<AiClientApiResponseDTO | null>(null);
  const [loadingApiIds, setLoadingApiIds] = useState<Set<string>>(new Set());

  // 表格列定义（通用资源列表模板）
  const columns = [
    {
      title: 'API',
      key: 'apiNameId',
      width: 220,
      render: (_: unknown, record: AiClientApiResponseDTO) => (
        <NameIdCell
          name={record.baseUrl}
          id={record.apiId}
          idLabel="API ID"
          copyLabel="API ID"
        />
      ),
    },
    {
      title: '对话路径',
      dataIndex: 'completionsPath',
      key: 'completionsPath',
      width: 100,
      render: (text: string) => (
        <DescriptionCellWrap style={{ maxWidth: 100 }}>{text || '-'}</DescriptionCellWrap>
      ),
    },
    {
      title: '嵌入路径',
      dataIndex: 'embeddingsPath',
      key: 'embeddingsPath',
      width: 100,
      render: (text: string) => (
        <DescriptionCellWrap style={{ maxWidth: 100 }}>{text || '-'}</DescriptionCellWrap>
      ),
    },
    {
      title: 'API密钥',
      dataIndex: 'apiKey',
      key: 'apiKey',
      width: 100,
      render: (text: string) => (
        <span
          title={text ? '点击复制' : '无'}
          style={{
            cursor: text ? 'pointer' as const : 'default',
            color: text ? 'var(--semi-color-primary)' : undefined,
            textDecoration: text ? 'underline' : undefined,
            fontSize: '12px',
          }}
          onClick={(e) => { e.stopPropagation(); text && handleCopyApiKey(text); }}
        >
          {text ? '***' + text.slice(-4) : '-'}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 88,
      render: (status: number) => <StatusDotTag status={status} />,
    },
    {
      title: '更新时间',
      key: 'updateTime',
      width: 160,
      render: (_: unknown, record: AiClientApiResponseDTO) => (
        <UpdateTimeWithCreateTooltip
          updateTime={record.updateTime}
          createTime={record.createTime}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right' as const,
      render: (_: unknown, record: AiClientApiResponseDTO) => (
        <TableActionLinks
          extraLeft={
            loadingApiIds.has(record.apiId) ? (
              <span className="action-link action-link-primary" style={{ cursor: 'default' }}>加载中</span>
            ) : (
              <a
                className="action-link action-link-primary"
                onClick={(e) => { e.stopPropagation(); handleLoadApi(record.apiId); }}
              >
                加载
              </a>
            )
          }
          onEdit={() => handleEdit(record)}
          onDelete={() => handleDelete(record.apiId)}
          deleteConfirmTitle="确定要删除这个API配置吗？"
          deleteConfirmContent="删除后无法恢复"
        />
      ),
    },
  ];

  // 加载数据
  const loadData = async () => {
    try {
      setLoading(true);
      const response = await aiClientApiAdminService.queryAiClientApiList(searchForm);
      if (response.code === '0000') {
        setDataSource(response.data || []);
      } else {
        Toast.error(response.info || '查询失败');
      }
    } catch (error) {
      console.error('查询AI客户端API配置失败:', error);
      Toast.error('查询失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  // 搜索
  const handleSearch = () => {
    setSearchForm(prev => ({ ...prev, pageNum: 1 }));
    loadData();
  };

  // 重置搜索
  const handleReset = () => {
    setSearchForm({
      apiId: '',
      status: undefined,
      pageNum: 1,
      pageSize: 10
    });
  };

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
        break;
    }
  };

  // 刷新
  const handleRefresh = () => {
    loadData();
  };

  // 创建
  const handleCreate = () => {
    setCreateModalVisible(true);
  };

  // 创建成功回调
  const handleCreateSuccess = () => {
    setCreateModalVisible(false);
    loadData();
  };

  // 取消创建
  const handleCreateCancel = () => {
    setCreateModalVisible(false);
  };

  // 编辑
  const handleEdit = (record: AiClientApiResponseDTO) => {
    setEditingRecord(record);
    setModalVisible(true);
  };

  // 模态框成功回调
  const handleModalSuccess = () => {
    setModalVisible(false);
    setEditingRecord(null);
    loadData();
  };

  // 模态框取消回调
  const handleModalCancel = () => {
    setModalVisible(false);
    setEditingRecord(null);
  };

  // 删除
  const handleDelete = async (apiId: string) => {
    try {
      const response = await aiClientApiAdminService.deleteAiClientApiByApiId(apiId);
      if (response.code === '0000') {
        Toast.success('删除成功');
        loadData();
      } else {
        Toast.error(response.info || '删除失败');
      }
    } catch (error) {
      console.error('删除AI客户端API配置失败:', error);
      Toast.error('删除失败，请稍后重试');
    }
  };

  // 复制API密钥
  const handleCopyApiKey = async (apiKey: string) => {
    try {
      await navigator.clipboard.writeText(apiKey);
      Toast.success('API密钥已复制到剪贴板');
    } catch (error) {
      console.error('复制失败:', error);
      // 降级方案：使用传统的复制方法
      try {
        const textArea = document.createElement('textarea');
        textArea.value = apiKey;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        Toast.success('API密钥已复制到剪贴板');
      } catch (fallbackError) {
        console.error('降级复制方案也失败:', fallbackError);
        Toast.error('复制失败，请手动复制');
      }
    }
  };

  // 加载API
  const handleLoadApi = async (apiId: string) => {
    try {
      setLoadingApiIds(prev => new Set(prev).add(apiId));
      const success = await AiAgentService.armoryApi(apiId);
      if (success) {
        Toast.success('API加载成功');
      } else {
        Toast.error('API加载失败');
      }
    } catch (error) {
      console.error('加载API失败:', error);
      Toast.error('加载API失败，请稍后重试');
    } finally {
      setLoadingApiIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(apiId);
        return newSet;
      });
    }
  };

  // 页面初始化
  useEffect(() => {
    loadData();
  }, []);

  return (
    <AiClientApiManagementLayout>
      <Sidebar 
        collapsed={collapsed} 
        selectedKey="ai-client-api-management"
        onSelect={handleNavigation}
      />
      <MainContent $collapsed={collapsed}>
        <ContentArea>
          <Header 
            collapsed={collapsed}
            onToggleSidebar={() => setCollapsed(!collapsed)}
            onLogout={() => {
              document.cookie = 'loginToken=;expires=Thu, 01 Jan 1970 00:00:01 GMT;path=/';
              localStorage.removeItem('token');
              localStorage.removeItem('userInfo');
              localStorage.removeItem('isLoggedIn');
              navigate('/login');
            }} 
          />
          <AiClientApiManagementContainer>
            <PageHeader>
              <Title heading={3}>模型API管理</Title>
            </PageHeader>

            <SearchSection>
              <SearchRow>
                <Input
                  placeholder="请输入API ID"
                  value={searchForm.apiId}
                  onChange={(value) => setSearchForm(prev => ({ ...prev, apiId: value }))}
                  style={{ width: 200 }}
                />
                <Button
                  type="primary"
                  icon={<IconSearch />}
                  onClick={handleSearch}
                  loading={loading}
                >
                  搜索
                </Button>
                <Button
                  icon={<IconRefresh />}
                  onClick={handleReset}
                >
                  重置
                </Button>
              </SearchRow>
            </SearchSection>

            <TableContainer>
              <ResourceTableCard>
                <div style={{ padding: '16px', borderBottom: '1px solid #e6e6e6' }}>
                  <Space>
                    <Button
                      type="primary"
                      icon={<IconPlus />}
                      onClick={handleCreate}
                    >
                      新增API
                    </Button>
                    <Button
                      icon={<IconRefresh />}
                      onClick={handleRefresh}
                      loading={loading}
                    >
                      刷新
                    </Button>
                  </Space>
                </div>
                <TableWrapper>
                  <Table
                    columns={columns}
                    dataSource={dataSource}
                    loading={loading}
                    size="small"
                    pagination={false}
                    scroll={{ x: 950, y: 'calc(100vh - 400px)' }}
                    rowKey="id"
                  />
                </TableWrapper>
              </ResourceTableCard>
            </TableContainer>
          </AiClientApiManagementContainer>
        </ContentArea>
      </MainContent>

      <AiClientApiCreateModal
        visible={createModalVisible}
        onCancel={handleCreateCancel}
        onSuccess={handleCreateSuccess}
      />

      <AiClientApiEditModal
        visible={modalVisible}
        editingRecord={editingRecord}
        onCancel={handleModalCancel}
        onSuccess={handleModalSuccess}
      />
    </AiClientApiManagementLayout>
  );
};