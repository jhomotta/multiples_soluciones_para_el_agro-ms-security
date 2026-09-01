package com.msagro.security.drivenadapters.securitydb.mapper;

import com.msagro.security.drivenadapters.securitydb.entity.UserRoleEntity;
import com.msagro.security.model.userrole.UserRole;
import org.mapstruct.Mapper;

/** MapStruct mapping between the {@link UserRole} domain model and its R2DBC entity. */
@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface UserRoleMapper {

    UserRoleEntity toEntity(UserRole model);

    UserRole toModel(UserRoleEntity entity);
}
