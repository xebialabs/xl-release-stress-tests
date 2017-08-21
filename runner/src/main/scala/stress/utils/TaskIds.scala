package stress.utils


object TaskIds {

  /**
   * Generates $releases * $phases * $tasks task ids.
   */
  def generate(releases: Int, phases: Int, tasks: Int): Seq[String] = for (
    r   <- List.range(0, releases).map(i => s"Release$i");
    ph  <- List.range(0, phases).map(i => s"Phase$i");
    t   <- List.range(0, tasks).map(i => s"Task$i")
  ) yield Seq(r, ph, t).mkString("-")
}
