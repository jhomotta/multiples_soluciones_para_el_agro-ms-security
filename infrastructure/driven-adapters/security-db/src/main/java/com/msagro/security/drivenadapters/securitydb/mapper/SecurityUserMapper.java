package com.msagro.security.drivenadapters.securitydb.mapper;

import com.msagro.security.drivenadapters.securitydb.entity.SecurityUserEntity;
import com.msagro.security.model.securityuser.SecurityUser;
import org.mapstruct.Mapper;

/** MapStruct mapping between the {@link SecurityUser} domain model and its R2DBC entity. */
@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface SecurityUserMapper {

    SecurityUserEntity toEntity(SecurityUser model);

    SecurityUser toModel(SecurityUserEntity entity);
}
