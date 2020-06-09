package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class SearchBarLayout : LayoutManager {
  override fun addLayoutComponent(name: String, comp: Component) { /* not needed */ }

  override fun removeLayoutComponent(comp: Component) { /* not needed */ }

  override fun preferredLayoutSize(parent: Container): Dimension? = parent.preferredSize

  override fun minimumLayoutSize(parent: Container): Dimension? = parent.minimumSize

  override fun layoutContainer(parent: Container) {
    val cb = parent as? JComboBox<*> ?: return
    val width = cb.width
    val height = cb.height
    val insets = cb.insets
    val buttonHeight = height - insets.top - insets.bottom
    var buttonWidth = buttonHeight

    (cb.getComponent(0) as? JButton)?.also {
      val arrowInsets = it.insets
      buttonWidth = it.preferredSize.width + arrowInsets.left + arrowInsets.right
      it.setBounds(insets.left, insets.top, buttonWidth, buttonHeight)
    }

    var loupeButton: JButton? = null
    for (c in cb.components) {
      if ("ComboBox.loupeButton" == c.name) {
        loupeButton = c as? JButton
        break
      }
    }
    loupeButton?.setBounds(width - insets.right - buttonHeight, insets.top, buttonHeight, buttonHeight)

    (cb.editor.editorComponent as? JTextField)?.also {
      it.setBounds(
        insets.left + buttonWidth,
        insets.top,
        width - insets.left - insets.right - buttonWidth - buttonHeight,
        height - insets.top - insets.bottom
      )
    }
  }
}
