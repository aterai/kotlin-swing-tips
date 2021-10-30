package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class SearchBarLayout : LayoutManager {
  override fun addLayoutComponent(name: String, comp: Component) {
    /* not needed */
  }

  override fun removeLayoutComponent(comp: Component) {
    /* not needed */
  }

  override fun preferredLayoutSize(parent: Container): Dimension? = parent.preferredSize

  override fun minimumLayoutSize(parent: Container): Dimension? = parent.minimumSize

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
    var loupeButton: JButton? = null
    var loupeSize = 0
    for (c in cb.components) {
      if ("ComboBox.loupeButton" == c.name) {
        loupeButton = c as? JButton
        break
      }
    }
    if (loupeButton != null) {
      loupeSize = r.height
      loupeButton.setBounds(r.x + r.width - loupeSize, r.y, loupeSize, r.height)
    }

    // ComboBox Editor
    (cb.editor.editorComponent as? JTextField)?.also {
      it.setBounds(r.x + arrowSize, r.y, r.width - arrowSize - loupeSize, r.height)
    }
  }
}
