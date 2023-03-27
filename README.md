# mod-consortia

Copyright (C) 2022 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

APIs for Consortia module.

## Environment variables:

| Name        | Default value | Description                                                                                                                                            |
|:------------|:-------------:|:-------------------------------------------------------------------------------------------------------------------------------------------------------|
| DB_HOST     |   postgres    | Postgres hostname                                                                                                                                      |
| DB_PORT     |     5432      | Postgres port                                                                                                                                          |
| DB_USERNAME |  folio_admin  | Postgres username                                                                                                                                      |
| DB_PASSWORD |       -       | Postgres username password                                                                                                                             |
| DB_DATABASE | okapi_modules | Postgres database name                                                                                                                                 |
| ENV         |     folio     | Environment                                                                                                                                            |
| KAFKA_HOST  |     kafka     | Kafka broker hostname                                                                                                                                  |
| KAFKA_PORT  |     9092      | Kafka broker port                                                                                                                                      |
| ENV         |     folio     | Logical name of the deployment, must be set if Kafka/Elasticsearch are shared for environments, `a-z (any case)`, `0-9`, `-`, `_` symbols only allowed |

## Additional information
Consortia API provides the following URLs:

| Method | URL                                                     | Permissions                           | Description                                                    |
|--------|---------------------------------------------------------|---------------------------------------|----------------------------------------------------------------|
| GET    | /consortia/{consortiumId}/tenants                       | consortia.tenants.collection.get      | Gets list of tenants based on consortiumId                     |
| GET    | /consortia/{consortiumId}/user-tenants                  | consortia.user-tenants.collection.get | Gets list of user-tenants based on consortiumId                |
| GET    | /consortia/{consortiumId}/user-tenants/{associationId}  | consortia.user-tenants.item.get       | Gets single user-tenant based on consortiumId and consortiumId |
| GET    | /consortia/{consortiumId}                               | consortia.consortium.item.get         | Gets single tenant based on consortiumId                       |
| GET    | /consortia                                              | consortia.consortium.collection.get   | Gets list of consortium                                        |
| POST   | /consortia                                              | consortia.consortium.item.post        | Inserts single consortium                                      |
| POST   | /consortia/{consortiumId}/tenants                       | consortia.tenants.item.post           | Inserts a single tenant based on consortiumId                  |
| PUT    | /consortia/{consortiumId}/tenants/{tenantId}            | consortia.tenants.item.put            | Update a single tenant name based on consortiumId and tenantId |
| PUT    | /consortia/{consortiumId}                               | consortia.consortium.item.put         | Update consortium name based on consortiumId                   |

More detail can be found on Consortia wiki-page: [https://wiki.folio.org/display/DD/Defining+Tenant+Schema+For+Consortia](https://wiki.folio.org/display/DD/Defining+Tenant+Schema+For+Consortia).

### Required Permissions
Institutional users should be granted the following permissions in order to use this Consortia API:
- consortia.all

### Issue tracker

See project [mod-consortia]()
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at
[dev.folio.org](https://dev.folio.org/)
