package com.msagro.security.drivenadapters.securitydb.mapper;

import com.msagro.security.drivenadapters.securitydb.entity.RolePermissionEntity;
import com.msagro.security.model.rolepermission.RolePermission;
import org.mapstruct.Mapper;

/** MapStruct mapping between the {@link RolePermission} domain model and its R2DBC entity. */
@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface RolePermissionMapper {

    RolePermissionEntity toEntity(RolePermission model);

    RolePermission toModel(RolePermissionEntity entity);
}
