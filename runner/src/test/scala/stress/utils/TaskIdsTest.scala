package stress.utils

import org.scalatest.{Matchers, FunSpec}

class TaskIdsTest extends FunSpec with Matchers {

  describe("TaskIds") {

    it("should generate release no ids if one of dimensions is 0") {
      TaskIds.generate(0, 0, 0) shouldBe Seq()
      TaskIds.generate(1, 0, 0) shouldBe Seq()
      TaskIds.generate(1, 1, 0) shouldBe Seq()
    }

    it("should generate a single phase and task per release") {
      TaskIds.generate(2, 1, 1) shouldBe Seq("Release0-Phase0-Task0", "Release1-Phase0-Task0")
    }

    it("should generate a single release with multiple phases and a single task") {
      TaskIds.generate(1, 2, 1) shouldBe Seq(
        "Release0-Phase0-Task0",
        "Release0-Phase1-Task0"
      )
    }

    it("should generate 2 releases with 2 tasks and 1 task") {
      TaskIds.generate(2, 2, 1) shouldBe Seq(
        "Release0-Phase0-Task0",
        "Release0-Phase1-Task0",
        "Release1-Phase0-Task0",
        "Release1-Phase1-Task0"
      )
    }

    it("should generate 2 release with 2 phase and 2 tasks") {
      TaskIds.generate(2, 2, 2) shouldBe Seq(
        "Release0-Phase0-Task0",
        "Release0-Phase0-Task1",
        "Release0-Phase1-Task0",
        "Release0-Phase1-Task1",
        "Release1-Phase0-Task0",
        "Release1-Phase0-Task1",
        "Release1-Phase1-Task0",
        "Release1-Phase1-Task1"
      )
    }

    it("should convert taskId into domainId ") {
      TaskIds.toDomainId("Release0-Phase0-Task0") shouldBe "Applications/Release0/Phase0/Task0"
    }

  }

}
