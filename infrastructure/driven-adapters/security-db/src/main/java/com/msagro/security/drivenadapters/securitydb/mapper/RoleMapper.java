package com.msagro.security.drivenadapters.securitydb.mapper;

import com.msagro.security.drivenadapters.securitydb.entity.RoleEntity;
import com.msagro.security.model.role.Role;
import org.mapstruct.Mapper;

/** MapStruct mapping between the {@link Role} domain model and its R2DBC entity. */
@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface RoleMapper {

    RoleEntity toEntity(Role model);

    Role toModel(RoleEntity entity);
}
