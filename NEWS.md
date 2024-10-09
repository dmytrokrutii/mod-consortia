## 1.2.0 - Unreleased
### Stories
* [MODLD-450](https://folio-org.atlassian.net/browse/MODLD-450) - ECS support for Linked Data
* [MODINV-1079](https://folio-org.atlassian.net/browse/MODINV-1079) - Update module permissions in the ModuleDescriptor

## 1.1.0 - Released (Quesnelia R1 2024)
The focus of this release was to implement ECS tenant soft delete functionality and fix bugs

### Stories
* [MODCON-146](https://folio-org.atlassian.net/browse/MODCON-146) - Update users schema to not fail when unknown field arrives
* [MODCON-144](https://folio-org.atlassian.net/browse/MODCON-144) - Spring upgrade
* [MODCON-142](https://folio-org.atlassian.net/browse/MODCON-142) - Upgrade to the latest spring-base versions
* [MODCON-137](https://folio-org.atlassian.net/browse/MODCON-137) - Stabilize acquisition/consortia karate tests
* [MODCON-130](https://folio-org.atlassian.net/browse/MODCON-130) - Adjust process to add ECS tenant with soft deleted functionality
* [MODCON-127](https://folio-org.atlassian.net/browse/MODCON-127) - Implement ECS tenant soft deletion functionality
* [MODCON-123](https://folio-org.atlassian.net/browse/MODCON-123) - Implement mechanism to update system shadow users permissions
* [MODCON-120](https://folio-org.atlassian.net/browse/MODCON-120) - Add more fields to shadow user that are required in UI

### Bugfixes
* [MODCON-139](https://folio-org.atlassian.net/browse/MODCON-139) - SYSTEM_USER_PASSWORD is not propogated AND permissions already set issues
* [MODCON-122](https://folio-org.atlassian.net/browse/MODCON-122) - Allow Contributor Name Types To Be Updateable By Publish Coordinator
* [MODCON-121](https://folio-org.atlassian.net/browse/MODCON-121) - Allow Holding Sources To Be Updated With Publish Coordinator

### Dependencies
* Bump `spring-boot` from `3.1.4` to `3.2.3`
* Bump `folio-service-tools` from `3.1.0` to `4.0.0`

## 1.0.0 - Released (Poppy R2 2023)
The focus of this release was to implement backend logic for consortia

### Stories
* [MODCON-108](https://issues.folio.org/browse/MODCON-108) - Secure setup of system users by default
* [MODCON-103](https://issues.folio.org/browse/MODCON-103) - Skip patron users when migration existing users
* [MODCON-101](https://issues.folio.org/browse/MODCON-101) - Create primary affiliation after changing user type from patron to staff/staff to patron
* [MODCON-95](https://issues.folio.org/browse/MODCON-95) - Increase character limit of consortia member Code
* [MODCON-94](https://issues.folio.org/browse/MODCON-94) - Add required permissions during setup only for ECS mode
* [MODCON-93](https://issues.folio.org/browse/MODCON-93) - Improve transaction behavior when migrating existing users to consortia
* [MODCON-92](https://issues.folio.org/browse/MODCON-92) - Populate system users with type='system' in different modules
* [MODCON-91](https://issues.folio.org/browse/MODCON-91) - Make adjustments after integration with sharing instance functionality
* [MODCON-89](https://issues.folio.org/browse/MODCON-89) - Add logic to update shadow users firstName lastName
* [MODCON-88](https://issues.folio.org/browse/MODCON-88) - Allow sequential only adding tenants to the consortium
* [MODCON-86](https://issues.folio.org/browse/MODCON-86) - Switch deprecated /login endpoint to /login-with-expirity for system user
* [MODCON-85](https://issues.folio.org/browse/MODCON-85) - Add action to update setting to local when deletion was failed
* [MODCON-84](https://issues.folio.org/browse/MODCON-84) - Use consortia system user to share instance functionality
* [MODCON-83](https://issues.folio.org/browse/MODCON-83) - Add originalTenantId custom field to shadow users
* [MODCON-80](https://issues.folio.org/browse/MODCON-80) - Add shadow system user when adding tenant to consortia
* [MODCON-78](https://issues.folio.org/browse/MODCON-78) - Add externalSystemId, barcode fields support in consortia for SAML login
* [MODCON-77](https://issues.folio.org/browse/MODCON-77) - Populate shadow user's type when creating shadow user
* [MODCON-73](https://issues.folio.org/browse/MODCON-73) - Test simultaneous create tenant requests and apply improvements found after automated pipeline work
* [MODCON-71](https://issues.folio.org/browse/MODCON-71) - Implement endpoint to delete shared setting uuid from all tenants
* [MODCON-66](https://issues.folio.org/browse/MODCON-66) - Add source field and new value Consortium to all config entities
* [MODCON-65](https://issues.folio.org/browse/MODCON-65) - Implement endpoint to save shared setting uuid in all tenants
* [MODCON-64](https://issues.folio.org/browse/MODCON-64) - User can save tenant with duplicated code and name
* [MODCON-62](https://issues.folio.org/browse/MODCON-62) - Implement kafka producers and listeners to support flow to promoting local instance
* [MODCON-61](https://issues.folio.org/browse/MODCON-61) - Implement logic to pulling down a shared instance to create shadow one in the desired tenant
* [MODCON-60](https://issues.folio.org/browse/MODCON-60) - Implement GET endpoint to retrieve sharing action by search query
* [MODCON-59](https://issues.folio.org/browse/MODCON-59) - Implement POST endpoint to initiate instance sharing process
* [MODCON-58](https://issues.folio.org/browse/MODCON-58) - Implement Scheduling Cleanup Job to delete publication data
* [MODCON-57](https://issues.folio.org/browse/MODCON-57) - Implement Delete endpoint for publication
* [MODCON-56](https://issues.folio.org/browse/MODCON-56) - Implement Get endpoint for retrieving publication results
* [MODCON-50](https://issues.folio.org/browse/MODCON-50) - Create affiliation in central tenant after creating user in the institutional tenant
* [MODCON-49](https://issues.folio.org/browse/MODCON-49) - Implement Get endpoint for retrieving single publication details
* [MODCON-48](https://issues.folio.org/browse/MODCON-48) - Save general PC request state including results of each tenant request into DB
* [MODCON-46](https://issues.folio.org/browse/MODCON-46) - Implement initial version of POST /publications endpoint
* [MODCON-43](https://issues.folio.org/browse/MODCON-43) - Create system user to support all actions during Consortia tenant setup
* [MODCON-42](https://issues.folio.org/browse/MODCON-42) - Populate user-tenant with dummy user when adding tenant to consortium
* [MODCON-40](https://issues.folio.org/browse/MODCON-40) - Populate admin user with predefined permissions set during adding tenant to Consortia
* [MODCON-38](https://issues.folio.org/browse/MODCON-38) - Process USER_UPDATED event to update username if changed
* [MODCON-36](https://issues.folio.org/browse/MODCON-36) - Populate default user permissions when creating real/shadow users
* [MODCON-33](https://issues.folio.org/browse/MODCON-33) - Delete shadow users when real user was deleted
* [MODCON-32](https://issues.folio.org/browse/MODCON-32) - Store central tenant id for each institutional tenant in mod-consortia
* [MODCON-31](https://issues.folio.org/browse/MODCON-31) - Change unique index to use userId and tenantId
* [MODCON-30](https://issues.folio.org/browse/MODCON-30) - Exception handling for Assign/unassign a users affiliations
* [MODCON-27](https://issues.folio.org/browse/MODCON-27) - Make Get Active Consortium and Get Tenants endpoints public
* [MODCON-26](https://issues.folio.org/browse/MODCON-26) - Add tenant code to tenant profile
* [MODCON-25](https://issues.folio.org/browse/MODCON-25) - Create Primary Affiliations When Adding New Tenant to Consortium
* [MODCON-23](https://issues.folio.org/browse/MODCON-23) - Add createdDate and modifiedDate to each mod-consortia table
* [MODCON-19](https://issues.folio.org/browse/MODCON-19) - Implement CRUD for consortium DB table
* [MODCON-18](https://issues.folio.org/browse/MODCON-18) - Create consortium DB table
* [MODCON-14](https://issues.folio.org/browse/MODCON-14) - Process kafka event from mod-users to create primary user affiliation
* [MODCON-12](https://issues.folio.org/browse/MODCON-12) - Implement endpoint to remove user association from a tenant
* [MODCON-11](https://issues.folio.org/browse/MODCON-11) - Process kafka event from mod-users to delete primary user affiliation
* [MODCON-10](https://issues.folio.org/browse/MODCON-10) - Implement endpoint to add user affiliation and shadow user
* [MODCON-9](https://issues.folio.org/browse/MODCON-9) - Implement endpoint to retrieve list of registered affiliations for a user
* [MODCON-7](https://issues.folio.org/browse/MODCON-7) - Implement endpoint to update tenant name
* [MODCON-6](https://issues.folio.org/browse/MODCON-6) - Implement endpoint to add tenant
* [MODCON-4](https://issues.folio.org/browse/MODCON-4) - Scaffold mod-consortia repository
* [MODCON-3](https://issues.folio.org/browse/MODCON-3) - Define DB schema for storing user and affiliation associations and create table
* [MODCON-2](https://issues.folio.org/browse/MODCON-2) - Implement endpoint to retrieve list of registered tenants
* [MODCON-1](https://issues.folio.org/browse/MODCON-1) - Define DB schema for storing tenants and create table

### Bugfixes
* [MODCON-81](https://issues.folio.org/browse/MODCON-81) - Member user created by administrator from the same member tenant can not log in
* [MODCON-29](https://issues.folio.org/browse/MODCON-29) - Upgrade spring-boot-starter-actuator and spring-kafka fixing vulns
