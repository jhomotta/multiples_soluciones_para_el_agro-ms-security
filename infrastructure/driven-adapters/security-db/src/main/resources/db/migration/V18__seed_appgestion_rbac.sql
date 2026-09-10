-- ─────────────────────────────────────────────
-- Seed of the APP_GESTION application (msa-appgestion), in one migration:
-- application, professions, the five role profiles, the permission catalogue,
-- the default permission matrix, and the eight reference users of the
-- specification with their access grants and role assignments.
--
-- Source: reference_project/msa-appgestion/especificacion/03-roles-y-usuarios.md
-- Ids are resolved with sub-selects so the identity sequences stay correct.
-- ─────────────────────────────────────────────

-- ── Application ──────────────────────────────
-- Owned by the company already seeded in V17.
INSERT INTO application (company_id, code, name, description)
SELECT c.id, 'APP_GESTION', 'App Gestión',
       'Aplicación de gestión comercial, operativa y financiera (msa-appgestion)'
  FROM company c WHERE c.code = 'MSAGRO';

-- ── Professions ──────────────────────────────
-- Adds the classifications the reference users need; V17 already seeded the rest.
INSERT INTO profession (code, name, description) VALUES
    ('MANAGER',     'Administrador de empresas', 'Business administration professional'),
    ('SALES_AGENT', 'Asesor comercial',          'Commercial sales advisor'),
    ('ACCOUNTANT',  'Contador',                  'Accounting professional');

-- ── Roles ────────────────────────────────────
-- The five profiles of section 1 of the specification. The role code is the
-- explicit identifier the spec recommends instead of matching the role name by
-- substring, which cannot tell "administrativo" from "administrador".
INSERT INTO role (application_id, code, name, description)
SELECT a.id, r.code, r.name, r.description
  FROM application a
  CROSS JOIN (VALUES
      ('ADMIN',        'Administrador',  'Ve toda la cadena y es el único que aprueba dinero'),
      ('TECNICO',      'Técnico',        'Usuario de campo; su recorrido funciona sin señal'),
      ('PUNTO_VENTA',  'Punto de venta', 'Mostrador: cotiza rápido, abre garantías y recibe equipos'),
      ('TALLER',       'Taller',         'Centro de servicio técnico: hojas de vida, OT y garantías'),
      ('CONTABILIDAD', 'Contabilidad',   'Cierra el mes; solo Finanzas')
  ) AS r(code, name, description)
 WHERE a.code = 'APP_GESTION';

-- ── Permissions ──────────────────────────────
-- The eleven permissions of the default matrix. The code follows the
-- RESOURCE_ACTION convention of this schema; the description carries the key
-- the client application uses, so the mapping stays traceable in both directions.
INSERT INTO permission (application_id, code, name, resource, action, description)
SELECT a.id, p.code, p.name, p.resource, p.action, p.description
  FROM application a
  CROSS JOIN (VALUES
      ('MAINTENANCE_ACCESS', 'Informes de mantenimiento',      'MAINTENANCE',  'ACCESS',
       'Informes de mantenimiento; clave de la app: maintenance'),
      ('QUOTE_ACCESS',       'Cotizaciones',                   'QUOTE',        'ACCESS',
       'Cotizaciones; clave de la app: quotes'),
      ('PROJECT_READ',       'Ver proyectos',                  'PROJECT',      'READ',
       'Ver proyectos; clave de la app: projects'),
      ('PROJECT_MANAGE',     'Editar proyectos',               'PROJECT',      'MANAGE',
       'Editar proyectos y programación; clave de la app: manageProjects'),
      ('PROJECT_COST_READ',  'Ver costos y utilidad',          'PROJECT_COST', 'READ',
       'Ver costos, utilidad y margen; clave de la app: projectCosts'),
      ('OWN_WORK_ACCESS',    'Mi trabajo y mis pagos',         'OWN_WORK',     'ACCESS',
       'Mi trabajo y mis pagos; clave de la app: ownWork'),
      ('WORKSHOP_ACCESS',    'Servicio técnico',               'WORKSHOP',     'ACCESS',
       'Servicio técnico y hojas de vida; clave de la app: workshop'),
      ('WORKSHOP_MANAGE',    'Editar equipos y órdenes',       'WORKSHOP',     'MANAGE',
       'Editar equipos, OT y documentos; clave de la app: workshopManage'),
      ('USER_MANAGE',        'Administrar usuarios',           'USER',         'MANAGE',
       'Administrar usuarios, roles y módulos; clave de la app: manageUsers'),
      ('PAYMENT_MANAGE',     'Gestionar pagos a técnicos',     'PAYMENT',      'MANAGE',
       'Registrar y anular pagos a técnicos; clave de la app: managePayments'),
      ('FINANCE_ACCESS',     'Módulo financiero',              'FINANCE',      'ACCESS',
       'Módulo financiero; clave de la app: finanzas')
  ) AS p(code, name, resource, action, description)
 WHERE a.code = 'APP_GESTION';

-- ── Default permission matrix ────────────────
-- Section 2 of the specification, pair by pair. Two absences are deliberate:
-- ADMIN never gets OWN_WORK_ACCESS (RN-009, «Mi trabajo» is exclusive to
-- technicians), and PROJECT_COST_READ belongs to ADMIN alone (RN-008).
INSERT INTO role_permission (role_id, permission_id, application_id)
SELECT r.id, p.id, r.application_id
  FROM application a
  JOIN role r       ON r.application_id = a.id
  JOIN permission p ON p.application_id = a.id
  JOIN (VALUES
      ('ADMIN',        'MAINTENANCE_ACCESS'),
      ('ADMIN',        'QUOTE_ACCESS'),
      ('ADMIN',        'PROJECT_READ'),
      ('ADMIN',        'PROJECT_MANAGE'),
      ('ADMIN',        'PROJECT_COST_READ'),
      ('ADMIN',        'WORKSHOP_ACCESS'),
      ('ADMIN',        'WORKSHOP_MANAGE'),
      ('ADMIN',        'USER_MANAGE'),
      ('ADMIN',        'PAYMENT_MANAGE'),
      ('ADMIN',        'FINANCE_ACCESS'),
      ('TECNICO',      'MAINTENANCE_ACCESS'),
      ('TECNICO',      'PROJECT_READ'),
      ('TECNICO',      'OWN_WORK_ACCESS'),
      ('PUNTO_VENTA',  'QUOTE_ACCESS'),
      ('PUNTO_VENTA',  'WORKSHOP_ACCESS'),
      ('PUNTO_VENTA',  'WORKSHOP_MANAGE'),
      ('TALLER',       'WORKSHOP_ACCESS'),
      ('TALLER',       'WORKSHOP_MANAGE'),
      ('CONTABILIDAD', 'FINANCE_ACCESS')
  ) AS m(role_code, permission_code)
    ON m.role_code = r.code AND m.permission_code = p.code
 WHERE a.code = 'APP_GESTION';

-- ── People ───────────────────────────────────
-- The reference users of section 7, used for testing and acceptance.
INSERT INTO person (profession_id, identification_type, identification_number,
                    first_name, last_name, email, mobile)
SELECT pr.id, 'CC', s.identification_number, s.first_name, s.last_name, s.email, s.mobile
  FROM (VALUES
      ('1010000001', 'Gerente',  'General',      'gerente@msagro.local',                '3000000001', 'MANAGER'),
      ('1010000002', 'Técnico',  'De Campo',     'tecnico.campo@msagro.local',          '3000000002', 'TECHNICIAN'),
      ('1010000003', 'Técnico',  'Sin Asignación','tecnico.sin.asignacion@msagro.local','3000000003', 'TECHNICIAN'),
      ('1010000004', 'Vendedor', 'De Mostrador', 'punto.venta@msagro.local',            '3000000004', 'SALES_AGENT'),
      ('1010000005', 'Jefe',     'De Taller',    'jefe.taller@msagro.local',            '3000000005', 'TECHNICIAN'),
      ('1010000006', 'Auxiliar', 'Contable',     'contabilidad@msagro.local',           '3000000006', 'ACCOUNTANT'),
      ('1010000007', 'Usuario',  'Módulo Apagado','modulo.apagado@msagro.local',        '3000000007', 'TECHNICIAN'),
      ('1010000008', 'Usuario',  'Desactivado',  'usuario.desactivado@msagro.local',    '3000000008', 'OTHER')
  ) AS s(identification_number, first_name, last_name, email, mobile, profession_code)
  JOIN profession pr ON pr.code = s.profession_code;

-- ── Credentials ──────────────────────────────
-- Argon2id hashes of the shared development password, produced with the very
-- parameters of Argon2PasswordHasherAdapter (m=19456, t=2, p=1, 16-byte salt,
-- 32-byte output) and carrying the {argon2} prefix the DelegatingPasswordEncoder
-- dispatches on. Every row has its own salt. must_change_password stays TRUE, so
-- none of these credentials survives the first login in any real environment.
INSERT INTO security_user (person_id, username, password_hash, enabled, must_change_password)
SELECT p.id, s.username, s.password_hash, s.enabled, TRUE
  FROM (VALUES
      ('1010000001', 'gerente',
       '{argon2}$argon2id$v=19$m=19456,t=2,p=1$d6/xMSoJP+4jKLkN1Wjjdw$h9ougWm42AJ2Jt331GQNvCXUGerwSoar/MdfdDB8C2k', TRUE),
      ('1010000002', 'tecnico.campo',
       '{argon2}$argon2id$v=19$m=19456,t=2,p=1$7V9fKO3Wtai1fNHg80qH2g$5lpyHb0X4DaMAtK8X/beTF/rBpSHzz2NgIwOg+dNZ8I', TRUE),
      ('1010000003', 'tecnico.sin.asignacion',
       '{argon2}$argon2id$v=19$m=19456,t=2,p=1$i9oAVoZIKwbSia+FzZ/GIQ$emhozHAXEPwElnL72Sexg0b+Adwqu0ZcoD3MCs412vk', TRUE),
      ('1010000004', 'punto.venta',
       '{argon2}$argon2id$v=19$m=19456,t=2,p=1$lxHDJELN4SVzx5xXjiYoXw$VVhb25Py+4SvR8bqUmz2kxQGbMVzbRfPCkbmbiHazoE', TRUE),
      ('1010000005', 'jefe.taller',
       '{argon2}$argon2id$v=19$m=19456,t=2,p=1$n2SQxvLYzm51qJ2uI0GB9Q$K7lEubDuP+R9/CIv1zBk0CP0YOSdTi4PlHVgS7UeKxs', TRUE),
      ('1010000006', 'contabilidad',
       '{argon2}$argon2id$v=19$m=19456,t=2,p=1$p7HxyNW6noJwOi0H5LIEZA$TNaWPSaKxKFk+GMng74VTRFzSTRhn0lHxWS/MmGLN5g', TRUE),
      ('1010000007', 'modulo.apagado',
       '{argon2}$argon2id$v=19$m=19456,t=2,p=1$YZkVjInu+MefFwxYkQIfCQ$1zQ8YGI8sthZ8RgyAEMmniAOTuLIDTgpO6JKZIgDyxU', TRUE),
      -- Deactivated on purpose: cannot log in, and the history of the person stays.
      ('1010000008', 'usuario.desactivado',
       '{argon2}$argon2id$v=19$m=19456,t=2,p=1$c/LgIHs9rud6Rys9Mz9JbQ$+PHUG3TCHqPrDWCCdLcy793+IszxQS1pka4HRx6RpgY', FALSE)
  ) AS s(identification_number, username, password_hash, enabled)
  JOIN person p ON p.identification_type = 'CC'
               AND p.identification_number = s.identification_number;

-- ── Access grants ────────────────────────────
-- The grant is kept active even for the deactivated user: the block lives in
-- security_user.enabled, so deactivating never erases the access history.
INSERT INTO user_application (security_user_id, application_id)
SELECT u.id, a.id
  FROM security_user u
  JOIN person p ON p.id = u.person_id
             AND p.identification_type = 'CC'
             AND p.identification_number BETWEEN '1010000001' AND '1010000008'
  CROSS JOIN application a
 WHERE a.code = 'APP_GESTION';

-- ── Role assignments ─────────────────────────
INSERT INTO user_role (user_application_id, role_id, application_id)
SELECT ua.id, r.id, ua.application_id
  FROM user_application ua
  JOIN application a     ON a.id = ua.application_id AND a.code = 'APP_GESTION'
  JOIN security_user u   ON u.id = ua.security_user_id
  JOIN (VALUES
      ('gerente',                'ADMIN'),
      ('tecnico.campo',          'TECNICO'),
      ('tecnico.sin.asignacion', 'TECNICO'),
      ('punto.venta',            'PUNTO_VENTA'),
      ('jefe.taller',            'TALLER'),
      ('contabilidad',           'CONTABILIDAD'),
      -- Same role as any technician. The «module turned off» scenario the spec
      -- describes needs the per-user module catalogue, which this schema does
      -- not model yet; only the account exists here.
      ('modulo.apagado',         'TECNICO'),
      ('usuario.desactivado',    'TECNICO')
  ) AS s(username, role_code) ON s.username = u.username
  JOIN role r ON r.application_id = ua.application_id AND r.code = s.role_code;
