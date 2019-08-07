package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

open class JSearchBar<E : SearchEngine> : JComboBox<E> {
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
    UIManager.put("ComboBox.font", getFont()) // XXX: ???
    getItemAt(0)?.also {
      // set ArrwoButton Icon
      (getComponent(0) as? JButton)?.setIcon(it.favicon)
    }
  }

  constructor(model: ComboBoxModel<E>) : super(model)

  @SafeVarargs
  constructor(vararg items: E) : super(items)

  protected override fun processFocusEvent(e: FocusEvent) {
    println("processFocusEvent")
  }

  companion object {
    private const val UI_CLASS_ID = "SearchBarComboBoxUI"
  }
}
