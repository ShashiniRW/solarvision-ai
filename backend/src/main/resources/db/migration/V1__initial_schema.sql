-- ============================================================
-- SolarVision AI
-- V1: Initial Identity and Organization Schema
-- ============================================================

-- ------------------------------------------------------------
-- Organizations
-- Represents a solar company/organization using SolarVision.
-- ------------------------------------------------------------
CREATE TABLE organizations (
                               id BIGSERIAL PRIMARY KEY,
                               name VARCHAR(150) NOT NULL,
                               email VARCHAR(255),
                               phone VARCHAR(30),
                               address TEXT,
                               status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT chk_organization_status
                                   CHECK (status IN ('ACTIVE', 'INACTIVE'))
);


-- ------------------------------------------------------------
-- Users
-- Stores the people who use SolarVision.
-- ------------------------------------------------------------
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       organization_id BIGINT NOT NULL,
                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       phone VARCHAR(30),
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_users_organization
                           FOREIGN KEY (organization_id)
                               REFERENCES organizations(id),

                       CONSTRAINT uq_users_email
                           UNIQUE (email),

                       CONSTRAINT chk_user_status
                           CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);


-- ------------------------------------------------------------
-- Roles
-- Defines what type of user someone is.
-- ------------------------------------------------------------
CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL,
                       description VARCHAR(255),

                       CONSTRAINT uq_roles_name
                           UNIQUE (name)
);


-- ------------------------------------------------------------
-- User Roles
-- Many-to-many relationship between users and roles.
-- ------------------------------------------------------------
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,

                            PRIMARY KEY (user_id, role_id),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_roles_role
                                FOREIGN KEY (role_id)
                                    REFERENCES roles(id)
                                    ON DELETE CASCADE
);


-- ------------------------------------------------------------
-- Initial system roles
-- ------------------------------------------------------------
INSERT INTO roles (name, description)
VALUES
    ('ADMIN', 'System administrator'),
    ('MANAGER', 'Solar company manager'),
    ('ENGINEER', 'Solar project engineer'),
    ('TECHNICIAN', 'Technical maintenance staff'),
    ('WORKER', 'Solar installation worker'),
    ('CUSTOMER', 'Solar system owner/customer');