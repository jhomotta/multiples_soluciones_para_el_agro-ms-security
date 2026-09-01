package com.msagro.security.drivenadapters.securitydb.mapper;

import com.msagro.security.drivenadapters.securitydb.entity.PermissionEntity;
import com.msagro.security.model.permission.Permission;
import org.mapstruct.Mapper;

/** MapStruct mapping between the {@link Permission} domain model and its R2DBC entity. */
@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface PermissionMapper {

    PermissionEntity toEntity(Permission model);

    Permission toModel(PermissionEntity entity);
}
