import React, { useEffect, useMemo, useState } from 'react';
import { Card, Col, Empty, Row, Skeleton, Space, Table, Typography, Toast, Tag, Progress } from '@douyinfe/semi-ui';
import styled from 'styled-components';

import { FinanceService, FinanceAccountItemDTO } from '../services';
import { theme } from '../styles/theme';

const { Title, Text } = Typography;

const HeaderRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: ${theme.spacing.base};
  margin-bottom: ${theme.spacing.lg};
`;

function formatValue(value: number, unit: string): string {
  if (!Number.isFinite(value)) return '-';
  if (unit?.toLowerCase() === 'token') return Math.floor(value).toLocaleString();
  if (unit?.toLowerCase() === 'gb') return value.toFixed(2);
  return value.toLocaleString();
}

function calcRemaining(item: FinanceAccountItemDTO): number {
  const total = Number(item.totalBalance || 0);
  const frozen = Number(item.frozenAmount || 0);
  return total - frozen;
}

export const FinanceAssetsPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<FinanceAccountItemDTO[]>([]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const list = await FinanceService.getAccountList();
      setData(Array.isArray(list) ? list : []);
    } catch (e: any) {
      console.error(e);
      Toast.error(e?.message || '获取资产概览失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const cards = useMemo(() => {
    return data.map(item => {
      const remaining = calcRemaining(item);
      const total = Number(item.totalBalance || 0);
      const percent = total > 0 ? Math.max(0, Math.min(100, (remaining / total) * 100)) : 0;
      return { item, remaining, total, percent };
    });
  }, [data]);

  return (
    <div>
      <HeaderRow>
        <div>
          <Title heading={4} style={{ margin: 0 }}>
            资源包 / 额度
          </Title>
          <Text type="tertiary">展示重点：当前还剩多少（余额 - 冻结）</Text>
        </div>
        <Space>
          <Tag color="blue" onClick={fetchData} style={{ cursor: 'pointer' }}>
            刷新
          </Tag>
        </Space>
      </HeaderRow>

      {loading ? (
        <Skeleton placeholder={<Card style={{ height: 160 }} />} style={{ width: '100%' }} />
      ) : data.length === 0 ? (
        <Card>
          <Empty description="暂无资产数据" />
        </Card>
      ) : (
        <Row gutter={[16, 16]}>
          {cards.map(({ item, remaining, total, percent }) => (
            <Col key={`${item.goodsType}_${item.resKey}`} span={12}>
              <Card
                style={{
                  borderRadius: theme.borderRadius.lg,
                  borderColor: theme.colors.border.secondary,
                }}
                title={item.displayName || `${item.goodsType}:${item.resKey}`}
              >
                <Space vertical spacing="tight" style={{ width: '100%' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                    <Text type="tertiary">剩余</Text>
                    <Text style={{ fontSize: theme.typography.fontSize.xl, fontWeight: 700 }}>
                      {formatValue(remaining, item.unit)} {item.unit}
                    </Text>
                  </div>
                  <Progress percent={Number.isFinite(percent) ? percent : 0} showInfo />
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Text type="tertiary">
                      总额：{formatValue(total, item.unit)} {item.unit}
                    </Text>
                    <Text type="tertiary">
                      冻结：{formatValue(Number(item.frozenAmount || 0), item.unit)} {item.unit}
                    </Text>
                  </div>
                </Space>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <div style={{ height: theme.spacing.xl }} />

      <Card
        title="资产明细"
        style={{
          borderRadius: theme.borderRadius.lg,
          borderColor: theme.colors.border.secondary,
        }}
      >
        <Table
          loading={loading}
          dataSource={data}
          rowKey={(r: FinanceAccountItemDTO) => `${r.goodsType}_${r.resKey}`}
          pagination={false}
          columns={[
            {
              title: '资源',
              dataIndex: 'displayName',
              render: (v: string, r: FinanceAccountItemDTO) => v || `${r.goodsType}:${r.resKey}`,
            },
            {
              title: '类型',
              dataIndex: 'goodsType',
              width: 140,
              render: (v: string) => <Tag color="grey">{v}</Tag>,
            },
            { title: 'resKey', dataIndex: 'resKey', width: 160 },
            {
              title: '剩余',
              width: 160,
              render: (_: any, r: FinanceAccountItemDTO) => `${formatValue(calcRemaining(r), r.unit)} ${r.unit}`,
            },
            {
              title: '总额',
              width: 160,
              render: (_: any, r: FinanceAccountItemDTO) => `${formatValue(Number(r.totalBalance || 0), r.unit)} ${r.unit}`,
            },
            {
              title: '冻结',
              width: 160,
              render: (_: any, r: FinanceAccountItemDTO) => `${formatValue(Number(r.frozenAmount || 0), r.unit)} ${r.unit}`,
            },
          ]}
        />
      </Card>
    </div>
  );
};

