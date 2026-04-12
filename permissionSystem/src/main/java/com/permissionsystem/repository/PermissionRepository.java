package com.permissionsystem.repository;

import com.permissionsystem.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Permission Repository, 用于操作 permissions 表
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
}