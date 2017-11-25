package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain.{Ci, RiskProfile}

object RiskProfilesGenerator {
  def generateRiskProfiles(): Seq[Ci] = {
    (1 to 1000).map(cnt => RiskProfile(
      "Configuration/riskProfiles/RiskProfile" + cnt, cnt.toString,
      Map("xlrelease.TaskRetriesRiskAssessor" -> "50",
        "xlrelease.TaskWithTwoOrThreeFlagsNeedsAttentionRiskAssessor" -> "20",
        "xlrelease.MoreThanOneTaskOverDueRiskAssessor" -> "35",
        "xlrelease.ReleaseDueDateRiskAssessor" -> "45",
        "xlrelease.TaskRetriesRiskAssessor4Retries" -> "80",
        "xlrelease.ReleaseFlaggedAttentionNeededRiskAssessor" -> "30",
        "xlrelease.TaskRetriesRiskAssessor5Retries" -> "90",
        "xlrelease.TaskRetriesRiskAssessor3Retries" -> "70",
        "xlrelease.TaskWithMoreThanSixFlagsAtRiskRiskAssessor" -> "80",
        "xlrelease.TaskRetriesRiskAssessorMoreThan5Retries" -> "100",
        "xlrelease.OneTaskOverDueRiskAssessor" -> "25",
        "xlrelease.TaskWithFourFiveOrSixFlagsNeedsAttentionRiskAssessor" -> "30",
        "xlrelease.ReleaseStatusFailedRiskAssessor" -> "90",
        "xlrelease.TaskWithOneFlagAtRiskRiskAssessor" -> "65",
        "xlrelease.ReleaseFlaggedAtRiskAssessor" -> "80",
        "xlrelease.TaskWithMoreThanSixFlagsNeedsAttentionRiskAssessor" -> "40",
        "xlrelease.ReleaseStatusFailingRiskAssessor" -> "70",
        "xlrelease.TaskWithTwoOrThreeFlagsAtRiskRiskAssessor" -> "70",
        "xlrelease.TaskRetriesRiskAssessor2Retries" -> "60",
        "xlrelease.TaskWithOneFlagNeedsAttentionRiskAssessor" -> "10",
        "xlrelease.TaskWithFourFiveOrSixFlagsAtRiskRiskAssessor" -> "75")))
  }
}
