package stress.utils

object Ids {
  def toDomainId(ciId: String) = s"Applications/${ciId.replace('-', '/')}"
}
