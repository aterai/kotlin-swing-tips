package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

internal class SearchEngineComboBoxModel<E : SearchEngine> : DefaultComboBoxModel<E>() {
  override fun setSelectedItem(anObject: Any) {
    // System.out.println("model: " + anObject)
  }
}
