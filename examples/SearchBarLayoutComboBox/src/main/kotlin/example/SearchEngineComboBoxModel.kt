package example

import javax.swing.DefaultComboBoxModel

class SearchEngineComboBoxModel<E : SearchEngine> : DefaultComboBoxModel<E>() {
  override fun setSelectedItem(anObject: Any) {
    // println("model: $anObject")
  }
}
