package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class TabButton : JRadioButton {
  var textColor: Color? = null // = Color.WHITE
  var pressedTc: Color? = null // = Color.WHITE.darker()
  var rolloverTc: Color? = null // = Color.WHITE
  var rolloverSelTc: Color? = null // = Color.WHITE
  var selectedTc: Color? = null // = Color.WHITE

  constructor() : super(null, null)

  constructor(icon: Icon) : super(null, icon)

  constructor(text: String) : super(text, null)

  constructor(a: Action) : super(a)

  constructor(text: String, icon: Icon) : super(text, icon)

  override fun updateUI() {
    // "Accidental override" reported when a @JvmStatic method in a Kotlin class has
    //   the same signature as a static method in a Java base class : KT-12993
    // https://youtrack.jetbrains.com/issue/KT-12993
    // val tmp = if (UIManager.get(getUIClassID()) != null) {
    //   UIManager.getUI(this)
    // } else {
    //   BasicTabViewButtonUI()
    // }
    // setUI(tmp)
    setUI(OperaTabViewButtonUI())
  }

  override fun getUIClassID() = "TabViewButtonUI"

  override fun getUI() = ui as? TabViewButtonUI

  override fun fireStateChanged() {
    val model = getModel()
    foreground = if (model.isEnabled) {
      if (model.isPressed && model.isArmed) {
        pressedTc
      } else if (model.isSelected) {
        selectedTc
      } else if (isRolloverEnabled && model.isRollover) {
        rolloverTc
      } else {
        textColor
      }
    } else {
      Color.GRAY
    }
    super.fireStateChanged()
  }
}
