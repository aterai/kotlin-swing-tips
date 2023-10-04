package example

import java.awt.*
import javax.swing.*

class CloseTabIcon(private val color: Color) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.drawLine(2, 2, 9, 9)
    g2.drawLine(2, 3, 8, 9)
    g2.drawLine(3, 2, 9, 8)
    g2.drawLine(9, 2, 2, 9)
    g2.drawLine(9, 3, 3, 9)
    g2.drawLine(8, 2, 2, 8)
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
}

class PlusIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)

    viewRect.bounds = c.bounds
    val r = SwingUtilities.calculateInnerArea(c as? JComponent, viewRect)
    OperaTabViewButtonUI.tabPainter(g2, r)

    g2.paint = Color.WHITE
    var w = viewRect.width
    val a = w / 2
    val b = w / 3
    w -= 2
    g2.drawLine(a, b, a, w - b)
    g2.drawLine(a - 1, b, a - 1, w - b)
    g2.drawLine(b, a, w - b, a)
    g2.drawLine(b, a - 1, w - b, a - 1)
    g2.dispose()
  }

  override fun getIconWidth() = 24

  override fun getIconHeight() = 24

  companion object {
    private val viewRect = Rectangle()
  }
}
