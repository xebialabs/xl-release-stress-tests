# Stress tests runner

This repository contains stress tests that can be executed against XLR instance.

## Running stress tests via gradle

Usage: `gradle run [arguments]`

Possible arguments are:

* `simulation` is the name of simulation to run: the class name of simulation. If not defined then all simulations will be run;
* `users [=10]` is a number of parallel threads that Gatling will instantiate to execute scenarios;
* `baseUrl [='http://localhost:5516']` Base URL of XL release server;
* `maxResponseSeconds [=300]` is the maximum response time before Gatling considers a test to be failing. It is 5 minutes by default.