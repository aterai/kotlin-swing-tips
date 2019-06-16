package example

class CheckBoxNode {
  var label: String = ""
  var status: Status = Status.INDETERMINATE

  constructor(label: String) {
    this.label = label
  }

  constructor(label: String, status: Status) {
    this.label = label
    this.status = status
  }

  override fun toString() = label
}
