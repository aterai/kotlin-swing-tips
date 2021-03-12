package example

protected data class CheckBoxNode(val label: String = "", val status: Status = Status.INDETERMINATE) {
  override fun toString() = label
}
