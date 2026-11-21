package example

import javax.swing.DefaultComboBoxModel

class SearchEngineComboBoxModel<E : SearchEngine> : DefaultComboBoxModel<E>() {
  // Ignore the selection so that the editor text is not replaced by the selected item
  override fun setSelectedItem(anObject: Any) {
    // println("model: $anObject")
  }
}
