xlr {
  template('Master Seed Template') {
    variables {
      stringVariable('user') {

      }
      stringVariable('releaseId') {
        required false
      }
    }
    scheduledStartDate new Date()
    scriptUsername 'admin'
    scriptUserPassword '###ADMIN_PASSWORD###'
    phases {
      phase('Create Releases') {
        color '#0099CC'
        tasks {
          script('Print releaseId') {
            script '\n' +
                'print \'releaseId:\'\n' +
                'print releaseVariables[\'releaseId\']\n' +
                'print \'end\''
          }
          parallelGroup('Generate Releases') {
            tasks {
              createRelease('CR1') {
                precondition 'if releaseVariables[\'releaseId\'] == \'\':\n' +
                    '  result = True\n' +
                    'else:\n' +
                    '  result = False'
                newReleaseTitle 'Automated release by ${user}'
                template 'Master Automation Template'
                templateVariables {
                  stringVariable('var1') {
                    value 'some var'
                  }
                }
                createdReleaseId '${releaseId}'
                releaseTags 'stress', 'auto'
              }
            }
          }
        }
      }
      phase('Wait for Releases') {
        color '#0099CC'
        tasks {
          parallelGroup('Waiting...') {
            precondition 'if releaseVariables[\'releaseId\'] != \'\':\n' +
                '  result = True\n' +
                'else:\n' +
                '  result = False'
            tasks {
              gate('G1') {
                dependencies {
                  dependency {
                    variable 'releaseId'
                  }
                }
              }
            }
          }
        }
      }
    }
    teams {
      team('Template Owner') {
        members 'admin'
        permissions 'template#edit', 'template#lock_task', 'template#view', 'template#edit_triggers', 'template#edit_security', 'template#create_release'
      }
      team('Release Admin') {
        permissions 'release#edit', 'release#lock_task', 'release#start', 'release#reassign_task', 'release#edit_blackout', 'template#view', 'release#edit_security', 'release#abort', 'release#view', 'release#edit_task'
      }
    }
  }
}
