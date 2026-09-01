package com.msagro.security.drivenadapters.securitydb.mapper;

import com.msagro.security.drivenadapters.securitydb.entity.CompanyEntity;
import com.msagro.security.model.company.Company;
import org.mapstruct.Mapper;

/** MapStruct mapping between the {@link Company} domain model and its R2DBC entity. */
@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface CompanyMapper {

    CompanyEntity toEntity(Company model);

    Company toModel(CompanyEntity entity);
}
