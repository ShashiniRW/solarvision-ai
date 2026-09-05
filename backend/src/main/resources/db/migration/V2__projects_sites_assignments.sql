-- ============================================================
-- SolarVision AI
-- V2: Projects, Sites, and Project Assignments Schema
-- ============================================================

-- ------------------------------------------------------------
-- Projects
-- A solar installation project, owned by an organization,
-- optionally linked to a customer (a user with role CUSTOMER).
-- ------------------------------------------------------------
CREATE TABLE projects (
                          id BIGSERIAL PRIMARY KEY,
                          organization_id BIGINT NOT NULL,
                          customer_id BIGINT,
                          name VARCHAR(150) NOT NULL,
                          description TEXT,
                          status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
                          start_date DATE,
                          end_date DATE,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_projects_organization
                              FOREIGN KEY (organization_id) REFERENCES organizations(id),

                          CONSTRAINT fk_projects_customer
                              FOREIGN KEY (customer_id) REFERENCES users(id),

                          CONSTRAINT chk_project_status
                              CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD', 'CANCELLED'))
);


-- ------------------------------------------------------------
-- Sites
-- A physical location where a project's solar installation
-- exists. One project can have multiple sites.
-- ------------------------------------------------------------
CREATE TABLE sites (
                       id BIGSERIAL PRIMARY KEY,
                       project_id BIGINT NOT NULL,
                       name VARCHAR(150) NOT NULL,
                       address TEXT,
                       latitude DECIMAL(9,6),
                       longitude DECIMAL(9,6),
                       capacity_kw DECIMAL(8,2),
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_sites_project
                           FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,

                       CONSTRAINT chk_site_status
                           CHECK (status IN ('ACTIVE', 'INACTIVE', 'DECOMMISSIONED'))
);


-- ------------------------------------------------------------
-- Project Assignments
-- Which users (staff) are assigned to which project, and
-- their role specifically on that project.
-- ------------------------------------------------------------
CREATE TABLE project_assignments (
                                     id BIGSERIAL PRIMARY KEY,
                                     project_id BIGINT NOT NULL,
                                     user_id BIGINT NOT NULL,
                                     role_in_project VARCHAR(20) NOT NULL,
                                     assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_assignments_project
                                         FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,

                                     CONSTRAINT fk_assignments_user
                                         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

                                     CONSTRAINT uq_project_user
                                         UNIQUE (project_id, user_id),

                                     CONSTRAINT chk_assignment_role
                                         CHECK (role_in_project IN ('MANAGER', 'ENGINEER', 'TECHNICIAN', 'WORKER'))
);