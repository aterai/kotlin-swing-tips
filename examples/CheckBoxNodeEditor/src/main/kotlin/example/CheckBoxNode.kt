package example

data class CheckBoxNode(
  val label: String,
  val status: Status,
) {
  override fun toString() = label
}
