package example

import java.awt.event.FocusEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

open class SearchBarComboBox<E : SearchEngine> : JComboBox<E> {
  constructor(model: ComboBoxModel<E>) : super(model)

  @SafeVarargs
  constructor(vararg items: E) : super(items)

  override fun getUIClassID() = UI_CLASS_ID

  override fun getUI() = ui as? SearchBarComboBoxUI

  // override fun setUI(newUI: SearchBarComboBoxUI) {
  //   super.setUI(newUI)
  // }

  override fun updateUI() {
    // "Accidental override" reported when a @JvmStatic method in a Kotlin class has
    //   the same signature as a static method in a Java base class : KT-12993
    // https://youtrack.jetbrains.com/issue/KT-12993
    // super.updateUI()
    // if (UIManager.get(getUIClassID()) != null) {
    //   setUI(UIManager.getUI(this) as SearchBarComboBoxUI)
    // } else {
    setUI(BasicSearchBarComboBoxUI())
    // }
    UIManager.put("ComboBox.font", font) // XXX: ???
    getItemAt(0)?.also {
      // set ArrowButton Icon
      (getComponent(0) as? JButton)?.icon = it.favicon
    }
  }

  override fun processFocusEvent(e: FocusEvent) {
    println("processFocusEvent")
  }

  companion object {
    private const val UI_CLASS_ID = "SearchBarComboBoxUI"
  }
}
