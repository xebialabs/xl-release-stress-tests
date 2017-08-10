package stress.utils

object Converters {
  def toDomainId(taskId: String) = s"Applications/${taskId.replace('-', '/')}"

  def seqToJsonString(seq: Seq[String]): String = s"""[${seq.mkString(", ")}]"""
}
