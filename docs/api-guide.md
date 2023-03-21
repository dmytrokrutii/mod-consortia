# Mod Consortia API Guide

## Tenants API

### Endpoints

| METHOD | URL      | DESCRIPTION                                       |
|--------|----------|---------------------------------------------------|
| POST   | /tenants | Return saved tenant                               |
| GET    | /tenants | Return list of tenants based on `limit`, `offset` |


## User Tenants Associations API

### Endpoints

| METHOD | URL                           | DESCRIPTION                                                                                          |
|--------|-------------------------------|------------------------------------------------------------------------------------------------------|
| GET    | /user-tenants                 | Return list of user tenant associations based on `userId`, `username`, `tenantId`, `limit`, `offset` |
| GET    | /user-tenants/{associationId} | Return user-tenant with provided `associationId`                                                     |

### Supported params options

| Option             | Example                                       | Description                                                             |
|--------------------|-----------------------------------------------|-------------------------------------------------------------------------|
| `userId`           | `id = "1bddbe67-fd27-436f-901e-9fa66fe4ad1d"` | Find user-tenants with ID (UUID) `1bddbe67-fd27-436f-901e-9fa66fe4ad1d` |
| `username`         | `username = "testuser"`                       | Find user-tenants with username `testuser`                              |

