# XL Release Stress tests

There are two projects in this repository :

- Data Generator : an application that populates an XL Release instance with active, completed releases and templates.
- Runner : an application that connects to an XL Release instance and performs stress tests.

# Data Generator

## Requirements


- Java 7 SDK
- XL Release 4.0.x or greater


## Running the data generator

The data generator should **not** be run on a production environment, as it will generate many active, completed and template releases.

It should be run only once a newly installed XL Release instance, running it several times on the same XL Release instance will result in errors.

The application can be started with the following command :

    ./gradlew :data-generator:run

or on windows

    gradlew :data-generator:run

It uses the following optional parameters :

- **Server URL**: The URL of the XL Release server instance
    - Syntax : -Pserver-url=http://url.to.server:5516
    - The default value is http://localhost:5516
- **Username**: The username that will be used to to connect to the server instance. This username needs "admin" permissions in order to populate data
    - Syntax : -Pusername=admin
    - The default value is 'admin'
- **Password**: The password of the user account that will be used to connect to the server instance.
    - Syntax : -Ppassword=password
    - The default value is 'admin'
- **Active Releases count**: The number of active releases that should be created.
    - Syntax : -Pactive-releases=100
    - The default value is 10
- **Completed Releases count**: The number of active releases that should be created.
    - Syntax : -Pcompleted-releases=500
    - The default value is 10
- **Templates count**: The number of templates that should be created.
    - Syntax : -Ptemplates=100
    - The default value is 10

Example :

    ./gradlew :data-generator:run -Pserver-url=http://localhost:5516 -Pusername=admin -Ppassword=admin -Ptemplates=20 -Pactive-releases=20 -Pcompleted-releases=20

# Runner

The runner is a java application that connects to an XL Release instance and performs stress-tests

## Installing the runner

Unzip/Untar the distribution archive :

    unzip runner.zip

or

    tar xvf runner.zip

This will create a **runner** directory

## Running

The application can be started using the startup scripts in the **runner/bin** directory :

- **runner** : Unix startup script
- **runner.bat** : Windows startup script