# Keycloak Dumy User Provider
  
Provides a [Keycloak][0] dummy user provider for local Keycloak development.
It is best used for local development integration where the developers can use any email and password 
to login and create users as they see fit.

## Key features of the migration provider

1. Allows to create any user with any email address
2. Allows the develop to create custom attributes and assign roles when trying to log in the first time

> **NOTE:** This should never be used in production

## How it Works

If the provider is enabled it will be called when logging in. if the user does not exist in the local keycloak instance
it will be created as a migrated user using the email address provided on the log-in screen.
Also it will use the password as a pattern to find out which role should be assign to the user as well as any other attribute that
was defined in the configuration.

> **Example:** Using **(?<clientId>\d+):(?<roleName>\w+)** inside the configuration will allow, the password 28:admin, to create a new user with the role admin and an attribute clientId with the value 28

## Installing the Federation Provider

TBD

[0]: http://keycloak.jboss.org/

