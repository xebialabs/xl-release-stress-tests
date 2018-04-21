package com.xebialabs.xlrelease.stress.scenarios

import cats.implicits._
import com.xebialabs.xlrelease.stress.api.Program
import freestyle.free._

object CreateReleases extends Scenario {
  override val name: String = s"Simple scenario to create release from groovy"

  val releasefile: String = s"""xlr {
  release('DSL') {
    variables {
      stringVariable('var1') {

      }
    }
    scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2018-04-20T13:19:10+0200')
    scriptUsername 'admin'
    scriptUserPassword '{b64}EIyCZiy3eK8T+iHra4hinQ=='
    phases {
      phase('Automated') {
        color '#0099CC'
        tasks {
          parallelGroup('Automated Tasks') {
            tasks {
              parallelGroup('Jython Tasks') {
                tasks {
                  script('J1') {
                    script 'import time\n' +
                           'import math\n' +
                           ' \n' +
                           'for i in range(0, 5):\n' +
                           '    mem = \' \' * 1048576\n' +
                           '    print(math.factorial(100))\n' +
                           '    time.sleep(1)'
                  }
                  script('J2') {
                    script 'import time\n' +
                           'import math\n' +
                           ' \n' +
                           'for i in range(0, 10):\n' +
                           '    mem = \' \' * 1048576\n' +
                           '    print(math.factorial(100))\n' +
                           '    time.sleep(1)'
                  }
                }
              }
              parallelGroup('Groovy Tasks') {
                tasks {
                  groovyScript('G1') {
                    script 'def factorial = { n -> (n == 1) ? 1 : n * call(n - 1) }\n' +
                           '(1..6).each {\n' +
                           '    \'_\'.multiply(1048576)\n' +
                           '    println(factorial(20))\n' +
                           '    sleep(1000)\n' +
                           '}'
                  }
                  groovyScript('G2') {
                    script 'def factorial = { n -> (n == 1) ? 1 : n * call(n - 1) }\n' +
                           '(1..8).each {\n' +
                           '    \'_\'.multiply(1048576)\n' +
                           '    println(factorial(20))\n' +
                           '    sleep(1000)\n' +
                           '}'
                  }
                }
              }
            }
          }
        }
      }
      phase('Manual') {
        color '#0099CC'
        tasks {
          userInput('UI') {
            description 'Please enter the required information below.'
            variables {
              variable 'var1'
            }
          }
        }
      }
    }
    teams {
      team('Release Admin') {
        permissions 'release#edit', 'release#lock_task', 'release#start', 'release#reassign_task', 'release#edit_blackout', 'template#view', 'release#edit_security', 'release#abort', 'release#view', 'release#edit_task'
      }
      team('Template Owner') {
        permissions 'template#edit', 'template#lock_task', 'template#view', 'template#edit_triggers', 'template#edit_security', 'template#create_release'
      }
    }
  }
}""".stripMargin

  override def program: Program[Unit] =
    api.xlr.users.admin() flatMap { implicit session =>
      for {
        phaseId <- api.xlr.releases.createRelease("test with releasefile")
        _ <- api.log.info(s"Release created: ${phaseId.release}")
        taskId <- api.xlr.tasks.appendScriptTask(phaseId, "dsl", "xlrelease.GroovyScriptTask", releasefile)
        _ <- api.log.info(s"Task created: ${taskId.task}")
      } yield ()
    }
}
