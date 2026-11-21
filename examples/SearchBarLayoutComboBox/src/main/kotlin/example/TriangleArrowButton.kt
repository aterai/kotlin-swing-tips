package example

import java.awt.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.CompoundBorder

class TriangleArrowButton : JButton() {
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val m = getModel()
    val highlighted = m.isArmed || (isRolloverEnabled && m.isRollover) || hasFocus()
    g2.paint = if (highlighted) highlightColor else background
    // Fill in this button's own coordinate space,
    // getBounds() returns the location in the parent JComboBox
    val w = width
    val h = height
    g2.fillRect(0, 0, w, h)
    g2.dispose()

    super.paintComponent(g)
    val i = insets
    val x = w - i.right - triangleIcon.iconWidth - 2
    val y = i.top + (h - i.top - i.bottom - triangleIcon.iconHeight) / 2
    triangleIcon.paintIcon(this, g, x, y)
  }

  override fun getPreferredSize(): Dimension {
    val i = insets
    val favicon = icon
    val iconWidth = favicon?.iconWidth ?: 16
    val w = iconWidth + triangleIcon.iconWidth + i.left + i.right
    return Dimension(w, w)
  }

  // Ignore the LookAndFeel default border, accept only the CompoundBorder
  // set by BasicSearchBarComboBoxUI#configureArrowButton()
  override fun setBorder(border: Border) {
    if (border is CompoundBorder) {
      super.setBorder(border)
    }
  }

  companion object {
    private val triangleIcon = TriangleIcon()
    private val highlightColor = Color(0xDC_DC_DC)
  }
}

class TriangleIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.GRAY
    g2.drawLine(2, 3, 6, 3)
    g2.drawLine(3, 4, 5, 4)
    g2.drawLine(4, 5, 4, 5)
    g2.dispose()
  }

  override fun getIconWidth() = 9

  override fun getIconHeight() = 9
}
