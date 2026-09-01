-- ─────────────────────────────────────────────
-- Minimum master data so the service is usable right after the first start:
-- one company, one application, a few professions, the ADMIN and USER roles,
-- and the permission catalogue wired to those roles.
-- Ids are resolved with sub-selects so the identity sequences stay correct.
-- ─────────────────────────────────────────────

INSERT INTO company (code, name, description)
VALUES ('MSAGRO', 'Multiples Soluciones para el Agro',
        'Company that owns the agro platform applications');

INSERT INTO application (company_id, code, name, description)
SELECT c.id, 'AGRO_CORE', 'Agro Core', 'Core application of the agro platform'
  FROM company c WHERE c.code = 'MSAGRO';

INSERT INTO application (company_id, code, name, description)
SELECT c.id, 'AGRO_FIELD', 'Agro Field', 'Field operations application'
  FROM company c WHERE c.code = 'MSAGRO';

INSERT INTO profession (code, name, description) VALUES
    ('AGRONOMIST',   'Agrónomo',              'Professional in agronomy'),
    ('VETERINARIAN', 'Veterinario',           'Professional in veterinary medicine'),
    ('ENGINEER',     'Ingeniero',             'Engineering professional'),
    ('TECHNICIAN',   'Técnico agropecuario',  'Agricultural technician'),
    ('FARMER',       'Agricultor',            'Farm owner or worker'),
    ('OTHER',        'Otra',                  'Not classified');

-- Roles of AGRO_CORE.
INSERT INTO role (application_id, code, name, description)
SELECT a.id, 'ADMIN', 'Administrador', 'Full access to the security microservice'
  FROM application a WHERE a.code = 'AGRO_CORE';

INSERT INTO role (application_id, code, name, description)
SELECT a.id, 'USER', 'Usuario', 'Default role assigned on self-registration'
  FROM application a WHERE a.code = 'AGRO_CORE';

-- Permission catalogue of AGRO_CORE.
INSERT INTO permission (application_id, code, name, resource, action, description)
SELECT a.id, p.code, p.name, p.resource, p.action, p.description
  FROM application a
  CROSS JOIN (VALUES
      ('COMPANY_READ',     'Ver empresas',       'COMPANY',     'READ',   'List and read companies'),
      ('COMPANY_CREATE',   'Crear empresas',     'COMPANY',     'CREATE', 'Create a company'),
      ('APPLICATION_READ', 'Ver aplicaciones',   'APPLICATION', 'READ',   'List and read applications'),
      ('APPLICATION_CREATE','Crear aplicaciones','APPLICATION', 'CREATE', 'Create an application'),
      ('PROFESSION_READ',  'Ver profesiones',    'PROFESSION',  'READ',   'List and read professions'),
      ('PROFESSION_CREATE','Crear profesiones',  'PROFESSION',  'CREATE', 'Create a profession'),
      ('ROLE_READ',        'Ver roles',          'ROLE',        'READ',   'List and read roles'),
      ('ROLE_CREATE',      'Crear roles',        'ROLE',        'CREATE', 'Create a role'),
      ('ROLE_ASSIGN',      'Asignar roles',      'ROLE',        'ASSIGN', 'Assign or revoke roles of a user'),
      ('PERMISSION_READ',  'Ver permisos',       'PERMISSION',  'READ',   'List and read permissions'),
      ('PERMISSION_CREATE','Crear permisos',     'PERMISSION',  'CREATE', 'Create a permission'),
      ('PERMISSION_GRANT', 'Otorgar permisos',   'PERMISSION',  'GRANT',  'Grant a permission to a role'),
      ('USER_READ',        'Ver usuarios',       'USER',        'READ',   'List and read users'),
      ('USER_CREATE',      'Crear usuarios',     'USER',        'CREATE', 'Create a user'),
      ('USER_GRANT_ACCESS','Otorgar accesos',    'USER',        'GRANT',  'Grant a user access to an application'),
      ('AUDIT_READ',       'Ver auditoría',      'AUDIT',       'READ',   'Read the security audit trail')
  ) AS p(code, name, resource, action, description)
 WHERE a.code = 'AGRO_CORE';

-- ADMIN gets every permission of the application.
INSERT INTO role_permission (role_id, permission_id, application_id)
SELECT r.id, p.id, r.application_id
  FROM role r
  JOIN permission p ON p.application_id = r.application_id
  JOIN application a ON a.id = r.application_id
 WHERE a.code = 'AGRO_CORE' AND r.code = 'ADMIN';

-- USER only reads the master catalogues.
INSERT INTO role_permission (role_id, permission_id, application_id)
SELECT r.id, p.id, r.application_id
  FROM role r
  JOIN permission p ON p.application_id = r.application_id
  JOIN application a ON a.id = r.application_id
 WHERE a.code = 'AGRO_CORE'
   AND r.code = 'USER'
   AND p.code IN ('PROFESSION_READ', 'APPLICATION_READ', 'ROLE_READ');
