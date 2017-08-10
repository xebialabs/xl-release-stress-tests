package stress.utils

object Converters {
  def toDomainId(ciId: String) = s"Applications/${ciId.replace('-', '/')}"
}
