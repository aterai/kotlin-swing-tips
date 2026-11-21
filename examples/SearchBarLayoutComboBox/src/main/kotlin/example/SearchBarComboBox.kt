package example

import java.awt.event.FocusEvent
import javax.swing.*

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
    // Show the favicon of the first search engine on the newly created arrow button
    val se = getItemAt(0)
    val button = if (componentCount > 0) getComponent(0) as? JButton else null
    if (se != null && button != null) {
      BasicSearchBarComboBoxUI.setArrowButtonIcon(button, se.favicon)
    }
  }

  override fun processFocusEvent(e: FocusEvent) {
    // println("processFocusEvent")
  }

  companion object {
    private const val UI_CLASS_ID = "SearchBarComboBoxUI"
  }
}
