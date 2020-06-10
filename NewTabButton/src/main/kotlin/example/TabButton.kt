package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class TabButton : JRadioButton {
  var textColor: Color? = null // = Color.WHITE;
  var pressedTextColor: Color? = null // = Color.WHITE.darker();
  var rolloverTextColor: Color? = null // = Color.WHITE;
  var rolloverSelectedTextColor: Color? = null // = Color.WHITE;
  var selectedTextColor: Color? = null // = Color.WHITE;

  override fun updateUI() {
    // "Accidental override" reported when a @JvmStatic method in a Kotlin class has
    //   the same signature as a static method in a Java base class : KT-12993
    // https://youtrack.jetbrains.com/issue/KT-12993
    // if (UIManager.get(getUIClassID()) != null) {
    //   setUI(UIManager.getUI(this))
    // } else {
    //   setUI(BasicTabViewButtonUI())
    // }
    setUI(OperaTabViewButtonUI())
  }

  override fun getUIClassID() = "TabViewButtonUI"

  override fun getUI() = ui as? TabViewButtonUI

  constructor() : super(null, null)

  constructor(icon: Icon) : super(null, icon)

  constructor(text: String) : super(text, null)

  constructor(a: Action) : super(a)

  constructor(text: String, icon: Icon) : super(text, icon)

  override fun fireStateChanged() {
    val model = getModel()
    foreground = if (model.isEnabled) {
      if (model.isPressed && model.isArmed) {
        pressedTextColor
      } else if (model.isSelected) {
        selectedTextColor
      } else if (isRolloverEnabled && model.isRollover) {
        rolloverTextColor
      } else {
        textColor
      }
    } else {
      Color.GRAY
    }
    super.fireStateChanged()
  }
}
