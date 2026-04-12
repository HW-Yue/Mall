/**
 * 资源管理列表 - 通用样式
 * 供客户端管理、顾问管理、模型管理等页面复用
 */
import styled from 'styled-components';
import { Card } from '@douyinfe/semi-ui';
import { theme } from '../../styles/theme';

/** 表格卡片：紧凑行高、hover、单元格内边距 */
export const ResourceTableCard = styled(Card)`
  flex: 1;
  display: flex;
  flex-direction: column;
  .semi-card-body {
    padding: 0;
    flex: 1;
    display: flex;
    flex-direction: column;
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
    padding: ${theme.spacing.xs} ${theme.spacing.sm};
  }
  .semi-table-thead .semi-table-cell {
    padding: ${theme.spacing.sm};
  }
`;

/** 名称 + ID 合并列（名称加粗，ID 小字灰色，可复制） */
export const NameIdCellWrap = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
  .resource-name {
    font-weight: ${theme.typography.fontWeight.medium};
    color: ${theme.colors.text.primary};
    max-width: 220px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .resource-id {
    font-size: ${theme.typography.fontSize.xs};
    color: #999;
    cursor: pointer;
    user-select: none;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    &:hover {
      color: ${theme.colors.primary};
    }
  }
`;

/** 描述列：固定最大宽度，超出省略 */
export const DescriptionCellWrap = styled.div`
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

/** 状态 Tag 前的圆点 */
export const StatusDot = styled.span<{ $enabled: boolean }>`
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: ${(p) => (p.$enabled ? theme.colors.success : theme.colors.error)};
  margin-right: 6px;
  vertical-align: middle;
`;

/** 操作列：纯文字链接容器 */
export const ActionLinksWrap = styled.div`
  display: flex;
  align-items: center;
  gap: ${theme.spacing.base};
  a.action-link {
    font-size: ${theme.typography.fontSize.sm};
    cursor: pointer;
    text-decoration: none;
    background: none;
    border: none;
    padding: 0;
  }
  a.action-link-primary {
    color: ${theme.colors.primary};
  }
  a.action-link-primary:hover {
    color: ${theme.colors.primaryHover};
  }
  a.action-link-danger {
    color: ${theme.colors.error};
  }
  a.action-link-danger:hover {
    color: #ff7875;
  }
`;
