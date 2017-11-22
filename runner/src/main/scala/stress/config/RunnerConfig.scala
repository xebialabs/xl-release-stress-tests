package stress.config

import java.util.concurrent.TimeUnit.MILLISECONDS

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.{FiniteDuration, Duration}

/**
 * When adding a new configuration option don't forget to check that it has the same path at `runner.conf`.
 * If you add a duration, use [[RunnerConfig.duration()]] for accessing it.
 */
object RunnerConfig extends LazyLogging {

  private val CONFIG_OBJECT_PATH = "xl.runner"

  lazy private val rootConfig = ConfigFactory.load("runner.conf")

  lazy private val runnerConfig = rootConfig.getConfig(CONFIG_OBJECT_PATH)

  private val durationDilation = runnerConfig.getDouble("durationDilation")

  /**
   * This object contains public, user-facing config parameters.
   */
  object input {

    val users: Int = rootConfig.getInt("xl.runner.input.users")

    def baseUrls: List[String] = runnerConfig.getString("input.baseUrl").split(",").toList

    val username: String = runnerConfig.getString("input.username")

    val password: String = runnerConfig.getString("input.password")

    val teams: Int = runnerConfig.getInt("input.teams")

    val ops: Int = runnerConfig.getInt("input.ops")

    val opsBulk: Int = runnerConfig.getInt("input.opsBulk")

    val ciso: Int = runnerConfig.getInt("input.ciso")

    val releaseManagers: Int = runnerConfig.getInt("input.releaseManagers")

    val sshHost: String = runnerConfig.getString("input.sshHost")

    val sshUser: String = runnerConfig.getString("input.sshUser")

    val sshPassword: String = runnerConfig.getString("input.sshPassword")
  }

  val releaseManagerPauseMin: FiniteDuration = duration("releaseManagerPauseMin")

  val releaseManagerPauseMax: FiniteDuration = duration("releaseManagerPauseMax")

  val opsPauseMin: FiniteDuration = duration("opsPauseMin")

  val opsPauseMax: FiniteDuration = duration("opsPauseMax")

  val opsBulkPauseMin: FiniteDuration = duration("opsBulkPauseMin")

  val opsBulkPauseMax: FiniteDuration = duration("opsBulkPauseMax")

  val devPause: FiniteDuration = duration("devPause")

  val userPauseMin: FiniteDuration = duration("userPauseMin")

  val userPauseMax: FiniteDuration = duration("userPauseMax")

  val taskPollPause: FiniteDuration = duration("taskPollPause")

  val taskPollDuration: FiniteDuration = duration("taskPollDuration")

  object queries {
    object search {
      val numberByPage: Int = runnerConfig.getInt("queries.search.numberByPage")
    }
  }

  object simulations {

    val postWarmUpPause: FiniteDuration =  duration("simulations.postWarmUpPause")

    val rampUpPeriod: FiniteDuration = duration("simulations.rampUpPeriod")

    val repeats: Int = runnerConfig.getInt("simulations.repeats")

    object realistic {

      val rampUpPeriod: FiniteDuration = duration("simulations.realistic.rampUpPeriod")

      val repeats: Int = runnerConfig.getInt("simulations.realistic.repeats")

    }
  }


  // Helpers

  /**
   * Always use this method to calculate duration. It will also take into account [[durationDilation]].
   */
  private def duration(path: String): FiniteDuration = {

    val duration = Duration(runnerConfig.getDuration(path, MILLISECONDS), MILLISECONDS)

    duration * durationDilation match {
      case fd: FiniteDuration => fd
      case _: Duration =>
        logger.warn(s"Using dilation factor $durationDilation resulted in infinite duration for $path. Falling back to non-dilated value: $duration.")
        duration
    }
  }
}
