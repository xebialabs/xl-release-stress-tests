String XLD_URL = System.getenv('XLD_URL')
String XLD_USERNAME = System.getenv('XLD_USERNAME')
String XLD_PASSWORD = System.getenv('XLD_PASSWORD')
String CONTROL_CI_ID = System.getenv('CONTROL_CI_ID')
String CONTROL_ACTION = System.getenv('CONTROL_ACTION')
int CONTROL_TASK_TIMEOUT = 120


@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.6' )
import static groovyx.net.http.ContentType.*
import groovyx.net.http.RESTClient

def xld = new RESTClient(XLD_URL)
xld.auth.basic(XLD_USERNAME, XLD_PASSWORD)

def controlXml = xld.get(path: "/deployit/control/prepare/$CONTROL_ACTION/$CONTROL_CI_ID",
    contentType: TEXT, headers: [Accept : 'application/xml']).data.text
def taskId = xld.post(path: '/deployit/control', body: controlXml,
    contentType: TEXT, headers: ['Content-Type' : XML, Accept : JSON]).data.text

println "Starting control task '$CONTROL_ACTION' of CI [$CONTROL_CI_ID] (task ID: $taskId)"
xld.post(path: "/deployit/tasks/v2/$taskId/start")

def tries = CONTROL_TASK_TIMEOUT / 2
while (tries-- > 0) {
  println "Waiting for control task to finish ..."
  Thread.sleep(2000)
  def state = xld.get(path: "/deployit/tasks/v2/$taskId").data.@state.text()
  if (state != "EXECUTING") {
    if (state == "EXECUTED") {
      println "Control task '$CONTROL_ACTION' of CI [$CONTROL_CI_ID] has successfully finished (task ID: $taskId)"
      return
    } else {
      throw new RuntimeException("Control task '$CONTROL_ACTION' of CI [$CONTROL_CI_ID] failed with state $state, " +
          "please check XL Deploy log files (task ID: $taskId)")
    }
  }
}

throw new RuntimeException("Control task '$CONTROL_ACTION' of CI [$CONTROL_CI_ID] did not finish in " +
    "${CONTROL_TASK_TIMEOUT} seconds, please check XL Deploy log files (task ID: $taskId)")
