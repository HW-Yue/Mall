import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Layout,
  Table, 
  Button, 
  Input, 
  Typography, 
  Toast,
  Card,
  Modal,
  Select,
  TextArea
} from '@douyinfe/semi-ui';
import { 
  IconSearch, 
  IconPlus, 
  IconRefresh
} from '@douyinfe/semi-icons';
import {
  ResourceTableCard,
  NameIdCell,
  DescriptionCell,
  StatusDotTag,
  UpdateTimeWithCreateTooltip,
  TableActionLinks,
} from '../components/resource-list-table';
import styled from 'styled-components';
import { theme } from '../styles/theme';
import { Sidebar, Header } from '../components/layout';
import { 
  aiClientSystemPromptAdminService, 
  AiClientSystemPromptQueryRequestDTO, 
  AiClientSystemPromptResponseDTO,
  AiClientSystemPromptRequestDTO
} from '../services/ai-client-system-prompt-admin-service';

const { Content } = Layout;
const { Title } = Typography;

// 样式组件
const SystemPromptManagementLayout = styled(Layout)`
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

const SystemPromptManagementContainer = styled.div`
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

export const ClientSystemPromptManagement: React.FC = () => {
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<AiClientSystemPromptResponseDTO[]>([]);
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  // 模态框状态
  const [modalVisible, setModalVisible] = useState(false);
  const [modalType, setModalType] = useState<'create' | 'edit' | 'view'>('create');
  const [currentRecord, setCurrentRecord] = useState<AiClientSystemPromptResponseDTO | null>(null);
  const [formData, setFormData] = useState<AiClientSystemPromptRequestDTO>({});

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

  // 表格列定义（通用资源列表模板）
  const columns = [
    {
      title: '提示词名称',
      key: 'promptNameId',
      width: 200,
      render: (_: unknown, record: AiClientSystemPromptResponseDTO) => (
        <NameIdCell
          name={record.promptName}
          id={record.promptId}
          idLabel="提示词ID"
          copyLabel="提示词ID"
        />
      ),
    },
    {
      title: '提示词内容',
      dataIndex: 'promptContent',
      key: 'promptContent',
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
      render: (_: unknown, record: AiClientSystemPromptResponseDTO) => (
        <UpdateTimeWithCreateTooltip
          updateTime={record.updateTime}
          createTime={record.createTime}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      fixed: 'right' as const,
      render: (_: unknown, record: AiClientSystemPromptResponseDTO) => (
        <TableActionLinks
          onView={() => handleView(record)}
          viewLabel="查看"
          onEdit={() => handleEdit(record)}
          onDelete={() => handleDelete(record)}
          deleteConfirmTitle="确定要删除这个系统提示词配置吗？"
          deleteConfirmContent="删除后无法恢复，请谨慎操作"
        />
      ),
    },
  ];

  // 获取系统提示词列表数据
  const fetchSystemPromptList = async () => {
    setLoading(true);
    try {
      const request: AiClientSystemPromptQueryRequestDTO = {
        promptName: searchText || undefined,
        status: statusFilter,
        pageNum: currentPage,
        pageSize: pageSize,
      };

      const result = await aiClientSystemPromptAdminService.querySystemPromptList(request);
      
      if (result.code === '0000') {
        const data = result.data || [];
        setDataSource(data);
        setTotal(data.length); // 简单实现，实际应该从后端返回总数
      } else {
        throw new Error(result.info || '查询失败');
      }
    } catch (error) {
      console.error('获取系统提示词列表失败:', error);
      Toast.error('获取系统提示词列表失败，请检查网络连接');
      setDataSource([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  // 删除系统提示词
  const handleDelete = async (record: AiClientSystemPromptResponseDTO) => {
    try {
      const result = await aiClientSystemPromptAdminService.deleteSystemPromptById(record.id);
      
      if (result.code === '0000' && result.data) {
        Toast.success('删除成功');
        // 重新加载数据
        fetchSystemPromptList();
      } else {
        throw new Error(result.info || '删除失败');
      }
    } catch (error) {
      console.error('删除系统提示词失败:', error);
      Toast.error('删除失败，请检查网络连接');
    }
  };

  // 查看系统提示词
  const handleView = (record: AiClientSystemPromptResponseDTO) => {
    setCurrentRecord(record);
    setModalType('view');
    setFormData(record);
    setModalVisible(true);
  };

  // 编辑系统提示词
  const handleEdit = (record: AiClientSystemPromptResponseDTO) => {
    setCurrentRecord(record);
    setModalType('edit');
    setFormData(record);
    setModalVisible(true);
  };

  // 新增系统提示词
  const handleCreate = () => {
    setCurrentRecord(null);
    setModalType('create');
    setFormData({});
    setModalVisible(true);
  };

  // 保存系统提示词
  const handleSave = async () => {
    try {
      // 简单验证
      if (!formData.promptName?.trim()) {
        Toast.error('请输入提示词名称');
        return;
      }
      if (!formData.promptContent?.trim()) {
        Toast.error('请输入提示词内容');
        return;
      }
      
      const request: AiClientSystemPromptRequestDTO = {
        ...formData,
        id: currentRecord?.id,
      };

      let result;
      if (modalType === 'create') {
        result = await aiClientSystemPromptAdminService.createSystemPrompt(request);
      } else {
        result = await aiClientSystemPromptAdminService.updateSystemPromptById(request);
      }

      if (result.code === '0000' && result.data) {
        Toast.success(modalType === 'create' ? '创建成功' : '更新成功');
        setModalVisible(false);
        fetchSystemPromptList();
      } else {
        throw new Error(result.info || '操作失败');
      }
    } catch (error) {
      console.error('保存系统提示词失败:', error);
      Toast.error('保存失败，请检查输入信息');
    }
  };

  // 搜索
  const handleSearch = () => {
    setCurrentPage(1);
    fetchSystemPromptList();
  };

  // 重置搜索
  const handleReset = () => {
    setSearchText('');
    setStatusFilter(undefined);
    setCurrentPage(1);
    fetchSystemPromptList();
  };

  // 分页变化
  const handlePageChange = (page: number, size?: number) => {
    setCurrentPage(page);
    if (size && size !== pageSize) {
      setPageSize(size);
    }
    fetchSystemPromptList();
  };

  // 组件挂载时获取数据
  useEffect(() => {
    fetchSystemPromptList();
  }, []);

  return (
    <SystemPromptManagementLayout>
      <Sidebar 
        collapsed={collapsed}
        selectedKey="client-system-prompt-management"
        onSelect={handleNavigation}
      />
      <MainContent $collapsed={collapsed}>
        <ContentArea>
          <Header 
            collapsed={collapsed}
            onToggleSidebar={() => setCollapsed(!collapsed)}
            onLogout={handleLogout}
          />
          <SystemPromptManagementContainer>
            <PageHeader>
              <Title heading={3} style={{ margin: 0 }}>系统提示词管理</Title>
            </PageHeader>

            <SearchSection>
              <SearchRow>
                <Input
                  placeholder="请输入提示词名称"
                  value={searchText}
                  onChange={setSearchText}
                  style={{ width: 200 }}
                  onEnterPress={handleSearch}
                />
                <Select
                  placeholder="选择状态"
                  value={statusFilter}
                  onChange={(value) => setStatusFilter(value as number | undefined)}
                  style={{ width: 120 }}
                >
                  <Select.Option value={1}>启用</Select.Option>
                  <Select.Option value={0}>禁用</Select.Option>
                </Select>
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
                  onClick={handleCreate}
                >
                  新增提示词
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
                    scroll={{ x: 880 }}
                    empty={
                      <div style={{ padding: '40px', textAlign: 'center' }}>
                        <Typography.Text type="tertiary">暂无数据</Typography.Text>
                      </div>
                    }
                  />
                </TableWrapper>
              </ResourceTableCard>
            </TableContainer>
          </SystemPromptManagementContainer>
        </ContentArea>
      </MainContent>

      {/* 新增/编辑/查看模态框 */}
      <Modal
        title={
          modalType === 'create' ? '新增系统提示词' :
          modalType === 'edit' ? '编辑系统提示词' : '查看系统提示词'
        }
        visible={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={modalType === 'view' ? () => setModalVisible(false) : handleSave}
        okText={modalType === 'view' ? '关闭' : '保存'}
        cancelText="取消"
        width={800}
        style={{ maxHeight: '80vh' }}
      >
        <div style={{ padding: '16px 0' }}>
          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 500 }}>提示词ID</label>
            <Input
              value={formData.promptId || ''}
              onChange={(value) => setFormData({ ...formData, promptId: value })}
              placeholder="请输入提示词ID"
              disabled={modalType === 'edit' || modalType === 'view'}
            />
          </div>
          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 500 }}>提示词名称</label>
            <Input
              value={formData.promptName || ''}
              onChange={(value) => setFormData({ ...formData, promptName: value })}
              placeholder="请输入提示词名称"
              disabled={modalType === 'view'}
            />
          </div>
          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 500 }}>提示词内容</label>
            <TextArea
              value={formData.promptContent || ''}
              onChange={(value) => setFormData({ ...formData, promptContent: value })}
              placeholder="请输入提示词内容"
              autosize={{ minRows: 4, maxRows: 8 }}
              disabled={modalType === 'view'}
            />
          </div>
          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 500 }}>状态</label>
            <Select
              value={formData.status}
              onChange={(value) => setFormData({ ...formData, status: value as number })}
              placeholder="请选择状态"
              disabled={modalType === 'view'}
              style={{ width: '100%' }}
            >
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </div>
        </div>
      </Modal>
    </SystemPromptManagementLayout>
  );
};