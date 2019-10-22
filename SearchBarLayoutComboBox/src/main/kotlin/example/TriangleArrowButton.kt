package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.border.CompoundBorder

class TriangleArrowButton : JButton() {
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    if (getModel().isArmed()) {
      g2.setPaint(Color(0xDC_DC_DC))
    } else if (isRolloverEnabled() && getModel().isRollover()) {
      g2.setPaint(Color(0xDC_DC_DC))
    } else if (hasFocus()) {
      g2.setPaint(Color(0xDC_DC_DC))
    } else {
      g2.setPaint(getBackground())
    }
    val r = getBounds()
    r.grow(1, 1)
    g2.fill(r)
    g2.dispose()

    super.paintComponent(g)
    val i = getInsets()
    val x = r.width - i.right - triangleIcon.getIconWidth() - 2
    val y = i.top + (r.height - i.top - i.bottom - triangleIcon.getIconHeight()) / 2
    triangleIcon.paintIcon(this, g, x, y)
  }

  override fun getPreferredSize(): Dimension {
    val i = getInsets()
    val favicon = getIcon()
    val fw = favicon?.getIconWidth() ?: 16
    val w = fw + triangleIcon.getIconWidth() + i.left + i.right
    return Dimension(w, w)
  }

  override fun setBorder(border: Border) {
    if (border is CompoundBorder) {
      super.setBorder(border)
    }
  }

  companion object {
    private val triangleIcon = TriangleIcon()
  }
}

class TriangleIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.setPaint(Color.GRAY)
    g2.drawLine(2, 3, 6, 3)
    g2.drawLine(3, 4, 5, 4)
    g2.drawLine(4, 5, 4, 5)
    g2.dispose()
  }

  override fun getIconWidth() = 9

  override fun getIconHeight() = 9
}
