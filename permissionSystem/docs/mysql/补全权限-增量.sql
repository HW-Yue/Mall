-- =============================================
-- 补全权限（增量脚本，在已有库上执行）
-- 解决：管理员登录后访问角色/用户列表等接口报 Access Denied
-- 原因：Controller 使用权限 ID 1,2,3,4,5,6,8,9，库中原来只有 1-5
-- =============================================

-- 1. 补全缺失的权限 ID：6, 8, 9（若已存在会报错，可忽略或先查再插）
INSERT IGNORE INTO permissions (id, name) VALUES
(6, 'role:delete'),
(8, 'user:page'),
(9, 'role:page');

-- 2. 为管理员角色（role_id=1）分配权限 6、8、9（若已存在会报主键冲突，可忽略）
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES
(1, 6),
(1, 8),
(1, 9);

-- 执行后请让管理员用户重新登录（或清除 token 再登录），使新权限生效。
