package example

import java.awt.*
import javax.swing.*

class SearchBarLayout : LayoutManager {
  override fun addLayoutComponent(
    name: String,
    comp: Component,
  ) {
    // not needed
  }

  override fun removeLayoutComponent(comp: Component) {
    // not needed
  }

  override fun preferredLayoutSize(p: Container): Dimension? = p.preferredSize

  override fun minimumLayoutSize(p: Container): Dimension? = p.minimumSize

  override fun layoutContainer(parent: Container) {
    val cb = parent as? JComboBox<*> ?: return
    val r = SwingUtilities.calculateInnerArea(cb, null)

    // ArrowButton
    var arrowWidth = 0
    // BasicSearchBarComboBoxUI#installComponents() adds the arrow button first
    val arrowButton = if (cb.componentCount > 0) cb.getComponent(0) else null
    if (arrowButton is JButton) {
      val arrowInsets = arrowButton.insets
      arrowWidth = arrowButton.preferredSize.width + arrowInsets.left + arrowInsets.right
      arrowButton.setBounds(r.x, r.y, arrowWidth, r.height)
    }

    // LoupeButton
    val loupeButton = cb.components.firstOrNull { it.name == "ComboBox.loupeButton" }
    var loupeWidth = 0
    if (loupeButton is JButton) {
      loupeWidth = r.height // square button
      loupeButton.setBounds(r.x + r.width - loupeWidth, r.y, loupeWidth, r.height)
    }

    // ComboBox Editor
    (cb.editor.editorComponent as? JTextField)?.also {
      it.setBounds(r.x + arrowWidth, r.y, r.width - arrowWidth - loupeWidth, r.height)
    }
  }
}
