# Stress tests runner

This repository contains stress tests that can be executed against XLR instance.


## Running stress tests via gradle

Usage:

```
gradle stressTests  -Pargument1
```

Possible arguments are:

* `users [=10]` is a number of parallel threads that Gatling will instantiate to execute scenarios;
* `baseUrl [='http://localhost:4516']` Base URL of XL release server;
* `activeReleases` and `completedReleases` is a number of active and completed releases to be created before tests start. Note that total number of releases should not be less than 20;
* `templates` is a number of templates;
* `maxResponseSeconds [=300]` is the maximum response time before Gatling considers a test to be failing. It is 5 minutes by default;
* `simulation` is the name of simulation to run: the name of simulation class _without_ the `.scala` extension. If not defined then all simulations will be run.

Normally these stress tests are run by Jenkins jobs on a specific slave to make results comparable.

## Test simulations

First a clean repository is populated with the following data:

* 100 user profiles;
* `activeReleases` active releases;
* `completedReleases` completed releases;
	* with 5 phases in each release;
	* with 10 tasks in each phase, thus 50 tasks in each release.

All the simulations can be found at [simulations](simulation/src/test/scala/simulations/) folder. Most of them have human-readable description in class-level scaladoc.


## Running the tests

Tests are run on a specific Jenkins slave: *performance.xebialabs.com*, so that test results can be compared between code improvements.

There are several jobs with specific data settings, and one job with configurable settings which can be used to test on a different setup. You can find all the jobs here: https://dexter.xebialabs.com/jenkinsng/job/XL%20Release/job/master/job/Stress%20tests/ .

To see how the performance.xebialabs.com server feels while stress tests are running you can check this monitoring graph: http://monitor.xebialabs.com/cgp/host.php?h=performance.xebialabs.com

All generated repositories are cached in `/tmp/stress-tests/repository-$active-$completed-$templates` folders on the performance server. If you define a new repository setup it will be generated automatically. However the performance server does not perform well enough for big repositories, so the solution is to generate them locally on your laptop and then upload into the correct folder location.