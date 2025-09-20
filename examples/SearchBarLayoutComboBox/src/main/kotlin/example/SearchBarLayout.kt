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
    var arrowSize = 0
    (cb.getComponent(0) as? JButton)?.also {
      val arrowInsets = it.insets
      val bw = it.preferredSize.width + arrowInsets.left + arrowInsets.right
      it.setBounds(r.x, r.y, bw, r.height)
      arrowSize = bw
    }

    // LoupeButton
    var loupeButton = cb.components
      .filter { it.name == "ComboBox.loupeButton" }
      .firstOrNull()
    var loupeSize = 0
    if (loupeButton is JButton) {
      loupeSize = r.height
      loupeButton.setBounds(r.x + r.width - loupeSize, r.y, loupeSize, r.height)
    }

    // ComboBox Editor
    (cb.editor.editorComponent as? JTextField)?.also {
      it.setBounds(r.x + arrowSize, r.y, r.width - arrowSize - loupeSize, r.height)
    }
  }
}
