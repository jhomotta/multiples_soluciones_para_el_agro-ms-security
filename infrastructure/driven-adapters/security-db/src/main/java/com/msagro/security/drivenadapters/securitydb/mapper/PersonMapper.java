package com.msagro.security.drivenadapters.securitydb.mapper;

import com.msagro.security.drivenadapters.securitydb.entity.PersonEntity;
import com.msagro.security.model.person.Person;
import org.mapstruct.Mapper;

/** MapStruct mapping between the {@link Person} domain model and its R2DBC entity. */
@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface PersonMapper {

    PersonEntity toEntity(Person model);

    Person toModel(PersonEntity entity);
}
