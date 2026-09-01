package com.msagro.security.drivenadapters.securitydb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** R2DBC mapping for the {@code person} table. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("person")
public class PersonEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("profession_id")
    private Long professionId;

    @Column("identification_type")
    private String identificationType;

    @Column("identification_number")
    private String identificationNumber;

    @Column("first_name")
    private String firstName;

    @Column("middle_name")
    private String middleName;

    @Column("last_name")
    private String lastName;

    @Column("second_last_name")
    private String secondLastName;

    @Column("email")
    private String email;

    @Column("phone")
    private String phone;

    @Column("mobile")
    private String mobile;

    @Column("birth_date")
    private LocalDate birthDate;

    @Column("active")
    private Boolean active;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("created_by")
    private Long createdBy;

    @Column("updated_at")
    private OffsetDateTime updatedAt;

    @Column("updated_by")
    private Long updatedBy;
}
