package com.msagro.security.drivenadapters.securitydb.mapper;

import com.msagro.security.drivenadapters.securitydb.entity.ProfessionEntity;
import com.msagro.security.model.profession.Profession;
import org.mapstruct.Mapper;

/** MapStruct mapping between the {@link Profession} domain model and its R2DBC entity. */
@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface ProfessionMapper {

    ProfessionEntity toEntity(Profession model);

    Profession toModel(ProfessionEntity entity);
}
