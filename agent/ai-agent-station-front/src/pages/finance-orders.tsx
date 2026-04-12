import React, { useEffect, useMemo, useState } from 'react';
import { Card, Form, Select, Space, Table, Tag, Toast, Typography } from '@douyinfe/semi-ui';
import styled from 'styled-components';

import { FinanceDeliveryLogItemDTO, FinanceService } from '../services';
import { theme } from '../styles/theme';

const { Title, Text } = Typography;

const HeaderRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: ${theme.spacing.base};
  margin-bottom: ${theme.spacing.lg};
`;

const FilterBar = styled(Card)`
  border-radius: ${theme.borderRadius.lg} !important;
  border-color: ${theme.colors.border.secondary} !important;
  margin-bottom: ${theme.spacing.lg};
`;

const statusMap: Record<number, { label: string; color: 'green' | 'orange' | 'red' | 'grey' }> = {
  1: { label: '进行中', color: 'orange' },
  2: { label: '已到账', color: 'green' },
  3: { label: '下发失败', color: 'red' },
};

const bizTypeFallbackMap: Record<string, string> = {
  GROUP_BUY: '拼团成功奖励',
  DIRECT: '充值购买',
  SIGN: '每日签到',
};

function formatResValue(value: number, unit: string, goodsType?: string): string {
  if (!Number.isFinite(value)) return '-';
  if (goodsType === 'ai_model') return `${Math.floor(value).toLocaleString()} ${unit}`;
  if (unit?.toLowerCase() === 'gb') return `${value.toFixed(2)} ${unit}`;
  return `${value.toLocaleString()} ${unit}`;
}

export const FinanceOrdersPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [list, setList] = useState<FinanceDeliveryLogItemDTO[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [bizType, setBizType] = useState<string | undefined>(undefined);
  const [status, setStatus] = useState<number | undefined>(undefined);

  const fetchData = async (next?: Partial<{ pageNum: number; pageSize: number; bizType?: string; status?: number }>) => {
    const q = {
      pageNum: next?.pageNum ?? pageNum,
      pageSize: next?.pageSize ?? pageSize,
      bizType: next?.bizType ?? bizType,
      status: typeof (next?.status ?? status) === 'number' ? (next?.status ?? status) : undefined,
    };

    setLoading(true);
    try {
      const data = await FinanceService.getDeliveryLogs(q);
      setList(Array.isArray(data.list) ? data.list : []);
      setTotal(Number(data.total || 0));
    } catch (e: any) {
      console.error(e);
      Toast.error(e?.message || '获取订单流水失败');
      setList([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageNum, pageSize, bizType, status]);

  const bizTypeOptions = useMemo(
    () => [
      { label: '全部', value: '' },
      { label: '拼团成功奖励', value: 'GROUP_BUY' },
      { label: '充值购买', value: 'DIRECT' },
      { label: '每日签到', value: 'SIGN' },
    ],
    []
  );

  const statusOptions = useMemo(
    () => [
      { label: '全部', value: '' },
      { label: '进行中', value: 1 },
      { label: '已到账', value: 2 },
      { label: '下发失败', value: 3 },
    ],
    []
  );

  return (
    <div>
      <HeaderRow>
        <div>
          <Title heading={4} style={{ margin: 0 }}>
            订单管理
          </Title>
          <Text type="tertiary">展示重点：资源怎么来的（下发流水）</Text>
        </div>
      </HeaderRow>

      <FilterBar>
        <Form layout="horizontal" style={{ width: '100%' }}>
          <Space wrap>
            <Form.Slot label="业务类型">
              <Select
                style={{ width: 220 }}
                optionList={bizTypeOptions}
                value={bizType ?? ''}
                onChange={(v: any) => {
                  const nextBizType = v ? String(v) : undefined;
                  setPageNum(1);
                  setBizType(nextBizType);
                }}
              />
            </Form.Slot>
            <Form.Slot label="状态">
              <Select
                style={{ width: 180 }}
                optionList={statusOptions}
                value={typeof status === 'number' ? status : ''}
                onChange={(v: any) => {
                  const nextStatus = v === '' || v === null || typeof v === 'undefined' ? undefined : Number(v);
                  setPageNum(1);
                  setStatus(nextStatus);
                }}
              />
            </Form.Slot>
            <Tag color="blue" onClick={() => fetchData({ pageNum: 1 })} style={{ cursor: 'pointer' }}>
              刷新
            </Tag>
          </Space>
        </Form>
      </FilterBar>

      <Card
        title="下发流水"
        style={{
          borderRadius: theme.borderRadius.lg,
          borderColor: theme.colors.border.secondary,
        }}
      >
        <Table
          loading={loading}
          dataSource={list}
          rowKey={(r: FinanceDeliveryLogItemDTO) => r.orderId}
          pagination={{
            currentPage: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            onPageChange: (p: number) => setPageNum(p),
            onPageSizeChange: (s: number) => {
              setPageNum(1);
              setPageSize(s);
            },
          }}
          columns={[
            { title: '订单号', dataIndex: 'orderId', width: 220 },
            {
              title: '业务类型',
              dataIndex: 'bizType',
              width: 160,
              render: (v: string, r: FinanceDeliveryLogItemDTO) => (
                <Tag color="green">{r.bizTypeName || bizTypeFallbackMap[v] || v}</Tag>
              ),
            },
            { title: 'goodsType', dataIndex: 'goodsType', width: 120, render: (v: string) => <Tag color="grey">{v}</Tag> },
            { title: 'resKey', dataIndex: 'resKey', width: 160 },
            {
              title: '下发值',
              width: 180,
              render: (_: any, r: FinanceDeliveryLogItemDTO) => formatResValue(Number(r.resValue), r.unit, r.goodsType),
            },
            {
              title: '状态',
              dataIndex: 'status',
              width: 120,
              render: (v: number) => {
                const m = statusMap[v] || { label: String(v), color: 'grey' as const };
                return <Tag color={m.color}>{m.label}</Tag>;
              },
            },
            { title: '创建时间', dataIndex: 'createTime', width: 180 },
          ]}
        />
      </Card>
    </div>
  );
};

