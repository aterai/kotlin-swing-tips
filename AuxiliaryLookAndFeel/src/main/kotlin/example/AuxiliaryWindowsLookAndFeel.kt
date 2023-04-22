package example

import javax.swing.* // ktlint-disable no-wildcard-imports

class AuxiliaryWindowsLookAndFeel : LookAndFeel() {
  override fun getName() = "AuxiliaryWindows"

  override fun getID() = "Not well known"

  override fun getDescription() = "Auxiliary Windows Look and Feel"

  override fun isSupportedLookAndFeel() = true

  override fun isNativeLookAndFeel() = false

  override fun getDefaults(): UIDefaults {
    val table = object : UIDefaults() {
      override fun getUIError(msg: String) {
        // not needed
      }
    }
    val uiDefaults = arrayOf<Any>("ComboBoxUI", "example.AuxiliaryWindowsComboBoxUI")
    table.putDefaults(uiDefaults)
    return table
  }
}
