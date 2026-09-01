package com.msagro.security.drivenadapters.securitydb.mapper;

import com.msagro.security.drivenadapters.securitydb.entity.UserApplicationEntity;
import com.msagro.security.model.userapplication.UserApplication;
import org.mapstruct.Mapper;

/** MapStruct mapping between the {@link UserApplication} domain model and its R2DBC entity. */
@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface UserApplicationMapper {

    UserApplicationEntity toEntity(UserApplication model);

    UserApplication toModel(UserApplicationEntity entity);
}
