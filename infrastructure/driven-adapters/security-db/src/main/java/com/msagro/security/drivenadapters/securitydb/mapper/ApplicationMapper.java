package com.msagro.security.drivenadapters.securitydb.mapper;

import com.msagro.security.drivenadapters.securitydb.entity.ApplicationEntity;
import com.msagro.security.model.application.Application;
import org.mapstruct.Mapper;

/** MapStruct mapping between the {@link Application} domain model and its R2DBC entity. */
@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface ApplicationMapper {

    ApplicationEntity toEntity(Application model);

    Application toModel(ApplicationEntity entity);
}
