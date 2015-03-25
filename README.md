# Data Generator

The data generator is a java application that populates an XL Release instance with active and completed releases and templates.

## Installing the data generator

Unzip/Untar the distribution archive :

    unzip data-generator.zip

or

    tar xvf data-generator.zip

This will create a **data-generator** directory


## Running the data generator

The application can be started using the startup scripts in the **data-generator/bin** directory :

- **data-generator** : Unix startup script
- **data-generator.bat** : Windows startup script


The application uses the following optional JVM parameters :

- **Server URL**: The URL of the XL Release server instance
    - Syntax : -Dxl.data-generator.server-url=http://url.to.server:5516
    - The default value is http://localhost:5516
- **Username**: The username that will be used to to connect to the server instance. This username needs "admin" permissions in order to populate data
    - Syntax : -Dxl.data-generator.username=admin
    - The default value is 'admin'
- **Password**: The password of the user account that will be used to connect to the server instance.
    - Syntax : -Dxl.data-generator.password=password
    - The default value is 'admin'
- **Active Releases count**: The number of active releases that should be created.
    - Syntax : -Dxl.data-generator.active-releases=100
    - The default value is 10
- **Completed Releases count**: The number of active releases that should be created.
    - Syntax : -Dxl.data-generator.completed-releases=500
    - The default value is 10
- **Templates count**: The number of templates that should be created.
    - Syntax : -Dxl.data-generator.templates=100
    - The default value is 10

The parameters have to be specified in the DATA_GENERATOR_OPTS variable

    export DATA_GENERATOR_OPTS="-Dxl.data-generator.server-url=http://url.to.server:5516 -Dxl.data-generator.templates=100 -Dxl.data-generator.active-releases=100"

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