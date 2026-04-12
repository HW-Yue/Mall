/**
 * 资源管理列表 - 通用表格组件与单元格
 * 用于：客户端管理、顾问管理、模型管理等，保证信息合并、描述省略、状态圆点、操作链接风格一致
 */
export { ResourceTableCard, DescriptionCellWrap } from './styles';
export {
  copyToClipboard,
  NameIdCell,
  DescriptionCell,
  StatusDotTag,
  UpdateTimeWithCreateTooltip,
  TableActionLinks,
} from './cells';
export type {
  NameIdCellProps,
  DescriptionCellProps,
  StatusDotTagProps,
  UpdateTimeWithCreateTooltipProps,
  TableActionLinksProps,
} from './cells';
