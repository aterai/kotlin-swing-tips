package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class SearchBarLayout : LayoutManager {
  override fun addLayoutComponent(name: String, comp: Component) { /* not needed */ }

  override fun removeLayoutComponent(comp: Component) { /* not needed */ }

  override fun preferredLayoutSize(parent: Container) = parent.getPreferredSize()

  override fun minimumLayoutSize(parent: Container) = parent.getMinimumSize()

  override fun layoutContainer(parent: Container) {
    val cb = parent as? JComboBox<*> ?: return
    val width = cb.getWidth()
    val height = cb.getHeight()
    val insets = cb.getInsets()
    val buttonHeight = height - insets.top - insets.bottom
    var buttonWidth = buttonHeight

    (cb.getComponent(0) as? JButton)?.also {
      val arrowInsets = it.getInsets()
      buttonWidth = it.getPreferredSize().width + arrowInsets.left + arrowInsets.right
      it.setBounds(insets.left, insets.top, buttonWidth, buttonHeight)
    }
    var loupeButton: JButton? = null
    for (c in cb.getComponents()) {
      if ("ComboBox.loupeButton" == c.getName()) {
        loupeButton = c as? JButton
        break
      }
    }
    if (loupeButton != null) {
      loupeButton.setBounds(
          width - insets.right - buttonHeight,
          insets.top,
          buttonHeight,
          buttonHeight)
    }
    (cb.getEditor().getEditorComponent() as? JTextField)?.also {
      it.setBounds(
          insets.left + buttonWidth,
          insets.top,
          width - insets.left - insets.right - buttonWidth - buttonHeight,
          height - insets.top - insets.bottom)
    }
  }
}
