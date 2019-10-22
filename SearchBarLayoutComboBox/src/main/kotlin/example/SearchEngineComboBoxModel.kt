package example

import javax.swing.DefaultComboBoxModel

internal class SearchEngineComboBoxModel<E : SearchEngine> : DefaultComboBoxModel<E>() {
  override fun setSelectedItem(anObject: Any) {
    // println("model: $anObject")
  }
}
