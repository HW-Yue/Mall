/**
 * 资源管理列表 - 通用单元格组件
 * 使用 Semi Design，供各资源子页面复用
 */
import React from 'react';
import { Tooltip, Tag, Toast, Modal } from '@douyinfe/semi-ui';
import { NameIdCellWrap, DescriptionCellWrap, StatusDot, ActionLinksWrap } from './styles';

const { confirm } = Modal;

/** 复制到剪贴板 */
export function copyToClipboard(text: string, label = 'ID') {
  navigator.clipboard.writeText(text).then(
    () => Toast.success(`${label}已复制`),
    () => Toast.error('复制失败')
  );
}

/** 名称 + ID 合并列（名称加粗，ID 小字灰色 + 点击复制） */
export interface NameIdCellProps {
  name: string;
  id: string;
  idLabel?: string;
  /** 复制时展示的 label，如 "客户端ID" */
  copyLabel?: string;
}

export const NameIdCell: React.FC<NameIdCellProps> = ({
  name,
  id,
  idLabel,
  copyLabel = 'ID',
}) => (
  <NameIdCellWrap>
    <span className="resource-name">{name || '-'}</span>
    <Tooltip content="点击复制">
      <span
        className="resource-id"
        onClick={(e) => {
          e.stopPropagation();
          copyToClipboard(id, copyLabel);
        }}
      >
        {idLabel ? `${idLabel}: ${id}` : id}
      </span>
    </Tooltip>
  </NameIdCellWrap>
);

/** 描述列：最大宽度 220px，超出省略，悬浮 Tooltip */
export interface DescriptionCellProps {
  text: string | null | undefined;
}

export const DescriptionCell: React.FC<DescriptionCellProps> = ({ text }) => {
  const content = text || '-';
  return (
    <Tooltip content={content}>
      <DescriptionCellWrap>{content}</DescriptionCellWrap>
    </Tooltip>
  );
};

/** 状态列：圆点 + Tag，启用=绿色 */
export interface StatusDotTagProps {
  status: number;
  enabledValue?: number;
}

export const StatusDotTag: React.FC<StatusDotTagProps> = ({
  status,
  enabledValue = 1,
}) => {
  const enabled = status === enabledValue;
  return (
    <Tag color={enabled ? 'green' : 'red'}>
      <StatusDot $enabled={enabled} />
      {enabled ? '启用' : '禁用'}
    </Tag>
  );
};

/** 仅展示更新时间，悬浮显示创建时间 */
export interface UpdateTimeWithCreateTooltipProps {
  updateTime: string | null | undefined;
  createTime: string | null | undefined;
}

export const UpdateTimeWithCreateTooltip: React.FC<UpdateTimeWithCreateTooltipProps> = ({
  updateTime,
  createTime,
}) => {
  const updateStr = updateTime ? new Date(updateTime).toLocaleString('zh-CN') : '-';
  const createStr = createTime ? new Date(createTime).toLocaleString('zh-CN') : null;
  const content = (
    <span style={{ fontSize: '12px' }}>{updateStr}</span>
  );
  if (createStr) {
    return (
      <Tooltip content={<span>创建时间：{createStr}</span>}>
        {content}
      </Tooltip>
    );
  }
  return content;
};

/** 操作列：可选前置链接、可选查看、可选编辑、删除（危险色链接 + 确认） */
export interface TableActionLinksProps {
  /** 渲染在左侧的额外链接（如「加载」） */
  extraLeft?: React.ReactNode;
  onView?: () => void;
  viewLabel?: string;
  onEdit?: () => void;
  onDelete: () => void;
  deleteConfirmTitle?: string;
  deleteConfirmContent?: string;
}

export const TableActionLinks: React.FC<TableActionLinksProps> = ({
  extraLeft,
  onView,
  viewLabel = '查看',
  onEdit,
  onDelete,
  deleteConfirmTitle = '确定要删除吗？',
  deleteConfirmContent = '删除后无法恢复，请谨慎操作',
}) => {
  const handleDeleteClick = () => {
    confirm({
      title: deleteConfirmTitle,
      content: deleteConfirmContent,
      onOk: onDelete,
    });
  };

  return (
    <ActionLinksWrap>
      {extraLeft}
      {onView && (
        <a
          className="action-link action-link-primary"
          onClick={(e) => { e.stopPropagation(); onView(); }}
        >
          {viewLabel}
        </a>
      )}
      {onEdit && (
        <a
          className="action-link action-link-primary"
          onClick={(e) => { e.stopPropagation(); onEdit(); }}
        >
          编辑
        </a>
      )}
      <a
        className="action-link action-link-danger"
        onClick={(e) => { e.stopPropagation(); handleDeleteClick(); }}
      >
        删除
      </a>
    </ActionLinksWrap>
  );
};
