// Exported from:        http://build:5516/#/templates/Release24bd57fbb07b4b2689eb2b61d2a3f3fa/releasefile
// XL Release version:   8.0.0
// Date created:         Tue Apr 24 15:52:56 CEST 2018

xlr {
    template('DSL') {
        variables {
            stringVariable('var1') {

            }
        }
        scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2018-04-24T15:52:43+0200')
        scriptUsername 'admin'
        scriptUserPassword '###ADMIN_PASSWORD###'
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