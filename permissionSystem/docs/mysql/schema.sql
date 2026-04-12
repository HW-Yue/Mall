-- =============================================
-- 权限系统数据库脚本 (MySQL)
-- 根据项目实体与 Mapper 生成
-- =============================================

-- 创建数据库（若连接已有库可注释掉）
-- CREATE DATABASE IF NOT EXISTS permissions DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE permissions;

-- ---------------------------------------------
-- 1. 权限表 permissions
-- ---------------------------------------------
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS sys_log;

CREATE TABLE permissions (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    name VARCHAR(100) DEFAULT NULL COMMENT '权限名称（供 getPermissionNamesByPermissionIds 等查询使用）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ---------------------------------------------
-- 2. 角色表 role
-- ---------------------------------------------
CREATE TABLE role (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    uuid VARCHAR(36) NOT NULL COMMENT '角色唯一标识',
    name VARCHAR(100) NOT NULL COMMENT '角色名称',
    created_at TIMESTAMP NULL DEFAULT NULL COMMENT '创建时间',
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ---------------------------------------------
-- 3. 用户表 user
-- ---------------------------------------------
CREATE TABLE user (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    uuid VARCHAR(36) NOT NULL COMMENT '用户唯一标识',
    username VARCHAR(100) NOT NULL COMMENT '登录名',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    created_at TIMESTAMP NULL DEFAULT NULL COMMENT '创建时间',
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_uuid (uuid),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ---------------------------------------------
-- 4. 用户-角色关联表 user_role
-- ---------------------------------------------
CREATE TABLE user_role (
    user_id INT NOT NULL COMMENT '用户ID',
    role_id INT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_role_id (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ---------------------------------------------
-- 5. 角色-权限关联表 role_permissions
-- ---------------------------------------------
CREATE TABLE role_permissions (
    role_id INT NOT NULL COMMENT '角色ID',
    permission_id INT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permissions_permission_id (permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ---------------------------------------------
-- 6. 操作日志表 sys_log
-- ---------------------------------------------
CREATE TABLE sys_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    username VARCHAR(100) DEFAULT NULL COMMENT '操作人用户名',
    operation_type VARCHAR(50) DEFAULT NULL COMMENT '操作类型(login/create/update/delete等)',
    method VARCHAR(255) DEFAULT NULL COMMENT '操作描述/方法',
    ip VARCHAR(50) DEFAULT NULL COMMENT '用户IP',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
    create_date DATETIME DEFAULT NULL COMMENT '操作时间',
    cost_time BIGINT DEFAULT NULL COMMENT '耗时(毫秒)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';

-- =============================================
-- 初始数据（可选，按需执行）
-- =============================================

-- 初始权限（与 Controller 中 @PreAuthorize hasAuthority('x') 的 ID 对应）
-- 1=user:read, 2=user:write, 3=role:read, 4=role:write, 5=log:read, 6=role:delete, 8=user:page, 9=role:page
INSERT INTO permissions (id, name) VALUES
(1, 'user:read'),
(2, 'user:write'),
(3, 'role:read'),
(4, 'role:write'),
(5, 'log:read'),
(6, 'role:delete'),
(8, 'user:page'),
(9, 'role:page');

-- 初始角色（DataInitializer 中依赖 id=1 的角色）
INSERT INTO role (id, uuid, name, created_at, updated_at) VALUES
(1, UUID(), '管理员', NOW(), NOW()),
(2, UUID(), '普通用户', NOW(), NOW());

-- 为管理员角色分配全部权限（1,2,3,4,5,6,8,9）
INSERT INTO role_permissions (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 8), (1, 9);

-- 管理员用户（密码需与 BCrypt 加密一致：此处为 123456 的 BCrypt 密文示例，请用应用启动后登录或自行生成后替换）
-- 若使用 DataInitializer 初始化用户，可省略下面这行
-- INSERT INTO user (uuid, username, name, email, password, created_at, updated_at) VALUES
-- (UUID(), 'admin', '管理员', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', NOW(), NOW());
-- INSERT INTO user_role (user_id, role_id) VALUES (LAST_INSERT_ID(), 1);
