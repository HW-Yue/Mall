import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Layout,
  Table, 
  Button, 
  Input, 
  Typography, 
  Toast,
  Card
} from '@douyinfe/semi-ui';
import {
  ResourceTableCard,
  NameIdCell,
  DescriptionCell,
  StatusDotTag,
  UpdateTimeWithCreateTooltip,
  TableActionLinks,
} from '../components/resource-list-table';
import { 
  IconSearch, 
  IconPlus, 
  IconRefresh
} from '@douyinfe/semi-icons';
import styled from 'styled-components';
import { theme } from '../styles/theme';
import { Sidebar, Header } from '../components/layout';
import { ClientCreateModal } from '../components/client-create-modal';
import { ClientEditModal } from '../components/client-edit-modal';
import { 
  aiClientAdminService, 
  AiClientQueryRequestDTO, 
  AiClientResponseDTO 
} from '../services/ai-client-admin-service';

const { Content } = Layout;
const { Title } = Typography;

// 样式组件
const ClientManagementLayout = styled(Layout)`
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

const ClientManagementContainer = styled.div`
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

export const ClientManagement: React.FC = () => {
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<AiClientResponseDTO[]>([]);
  const [searchText, setSearchText] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [currentEditClient, setCurrentEditClient] = useState<AiClientResponseDTO | null>(null);

  // 获取用户信息
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');

  // 处理退出登录
  const handleLogout = () => {
    document.cookie = 'loginToken=;expires=Thu, 01 Jan 1970 00:00:01 GMT;path=/';
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    localStorage.removeItem('isLoggedIn');
    Toast.success('已退出登录');
    navigate('/login');
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
        navigate(path);
        break;
    }
  };

  // 表格列定义（通用资源列表模板：隐藏 ID、名称+ID 合并、描述省略、仅更新时间+创建时间 Tooltip、状态圆点、操作链接）
  const columns = [
    {
      title: '客户端名称',
      key: 'clientNameId',
      width: 200,
      render: (_: unknown, record: AiClientResponseDTO) => (
        <NameIdCell
          name={record.clientName}
          id={record.clientId}
          idLabel="客户端ID"
          copyLabel="客户端ID"
        />
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 220,
      render: (text: string) => <DescriptionCell text={text} />,
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
      render: (_: unknown, record: AiClientResponseDTO) => (
        <UpdateTimeWithCreateTooltip
          updateTime={record.updateTime}
          createTime={record.createTime}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right' as const,
      render: (_: unknown, record: AiClientResponseDTO) => (
        <TableActionLinks
          onEdit={() => handleEdit(record)}
          onDelete={() => handleDelete(record)}
          deleteConfirmTitle="确定要删除这个客户端配置吗？"
          deleteConfirmContent="删除后无法恢复，请谨慎操作"
        />
      ),
    },
  ];

  // 获取客户端列表数据
  const fetchClientList = async () => {
    setLoading(true);
    try {
      const request: AiClientQueryRequestDTO = {
        clientName: searchText || undefined,
        pageNum: currentPage,
        pageSize: pageSize,
      };

      const result = await aiClientAdminService.queryClientList(request);
      
      if (result.code === '0000') {
        const data = result.data || [];
        setDataSource(data);
        setTotal(data.length); // 简单实现，实际应该从后端返回总数
      } else {
        throw new Error(result.info || '查询失败');
      }
    } catch (error) {
      console.error('获取客户端列表失败:', error);
      Toast.error('获取客户端列表失败，请检查网络连接');
      setDataSource([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  // 删除客户端
  const handleDelete = async (record: AiClientResponseDTO) => {
    try {
      const result = await aiClientAdminService.deleteClientById(record.id);
      
      if (result.code === '0000' && result.data) {
        Toast.success('删除成功');
        // 重新加载数据
        fetchClientList();
      } else {
        throw new Error(result.info || '删除失败');
      }
    } catch (error) {
      console.error('删除客户端失败:', error);
      Toast.error('删除失败，请检查网络连接');
    }
  };

  // 编辑客户端
  const handleEdit = (record: AiClientResponseDTO) => {
    setCurrentEditClient(record);
    setEditModalVisible(true);
  };

  // 处理新增客户端
  const handleCreateClient = () => {
    setCreateModalVisible(true);
  };

  // 处理新增成功
  const handleCreateSuccess = () => {
    setCreateModalVisible(false);
    fetchClientList(); // 重新加载数据
  };

  // 处理新增取消
  const handleCreateCancel = () => {
    setCreateModalVisible(false);
  };

  // 处理编辑成功
  const handleEditSuccess = () => {
    setEditModalVisible(false);
    setCurrentEditClient(null);
    fetchClientList(); // 重新加载数据
  };

  // 处理编辑取消
  const handleEditCancel = () => {
    setEditModalVisible(false);
    setCurrentEditClient(null);
  };

  // 搜索
  const handleSearch = () => {
    setCurrentPage(1);
    fetchClientList();
  };

  // 重置搜索
  const handleReset = () => {
    setSearchText('');
    setCurrentPage(1);
    fetchClientList();
  };

  // 分页变化
  const handlePageChange = (page: number, size?: number) => {
    setCurrentPage(page);
    if (size && size !== pageSize) {
      setPageSize(size);
    }
    fetchClientList();
  };

  // 组件挂载时获取数据
  useEffect(() => {
    fetchClientList();
  }, []);

  return (
    <ClientManagementLayout>
      <Sidebar 
        collapsed={collapsed}
        selectedKey="client-management"
        onSelect={handleNavigation}
      />
      <MainContent $collapsed={collapsed}>
        <ContentArea>
          <Header 
            collapsed={collapsed}
            onToggleSidebar={() => setCollapsed(!collapsed)}
            onLogout={handleLogout}
          />
          <ClientManagementContainer>
            <PageHeader>
              <Title heading={3} style={{ margin: 0 }}>客户端管理</Title>
            </PageHeader>

            <SearchSection>
              <SearchRow>
                <Input
                  placeholder="请输入客户端名称"
                  value={searchText}
                  onChange={setSearchText}
                  style={{ width: 200 }}
                  onEnterPress={handleSearch}
                />
                <Button
                  type="primary"
                  icon={<IconSearch />}
                  onClick={handleSearch}
                >
                  搜索
                </Button>
                <Button
                  icon={<IconRefresh />}
                  onClick={handleReset}
                >
                  重置
                </Button>
                <Button
                  type="primary"
                  theme="solid"
                  icon={<IconPlus />}
                  onClick={handleCreateClient}
                >
                  新增客户端
                </Button>
              </SearchRow>
            </SearchSection>

            <TableContainer>
              <ResourceTableCard>
                <TableWrapper>
                  <Table
                    columns={columns}
                    dataSource={dataSource}
                    loading={loading}
                    size="small"
                    pagination={{
                      currentPage: currentPage,
                      pageSize: pageSize,
                      total: total,
                      showSizeChanger: true,
                      showQuickJumper: true,
                      onChange: handlePageChange
                    }}
                    rowKey="id"
                    scroll={{ x: 900 }}
                    empty={
                      <div style={{ padding: '40px', textAlign: 'center' }}>
                        <Typography.Text type="tertiary">暂无数据</Typography.Text>
                      </div>
                    }
                  />
                </TableWrapper>
              </ResourceTableCard>
            </TableContainer>
          </ClientManagementContainer>

          {/* 新增客户端弹窗 */}
          <ClientCreateModal
            visible={createModalVisible}
            onCancel={handleCreateCancel}
            onSuccess={handleCreateSuccess}
          />

          {/* 编辑客户端弹窗 */}
          <ClientEditModal
            visible={editModalVisible}
            clientData={currentEditClient}
            onCancel={handleEditCancel}
            onSuccess={handleEditSuccess}
          />
        </ContentArea>
      </MainContent>
    </ClientManagementLayout>
  );
};