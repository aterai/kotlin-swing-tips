package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(GridLayout(3, 1, 5, 5))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val hotSpot = Point(16, 16)
  val bi1 = makeStringBufferedImage("?")
  val label1 = JButton("?")
  label1.cursor = p.toolkit.createCustomCursor(bi1, hotSpot, "?")
  p.add(makeTitledPanel("String", label1))

  val bi2 = makeOvalBufferedImage()
  val label2 = JButton("Oval")
  label2.cursor = p.toolkit.createCustomCursor(bi2, hotSpot, "oval")
  label2.icon = ImageIcon(bi2)
  p.add(makeTitledPanel("drawOval", label2))

  val icon = GreenBlueIcon()
  val label3 = JButton("Rect")
  val bi3 = makeIconBufferedImage(icon, label3)
  label3.cursor = p.toolkit.createCustomCursor(bi3, hotSpot, "rect")
  label3.icon = icon
  p.add(makeTitledPanel("paintIcon", label3))

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeStringBufferedImage(str: String): BufferedImage {
  val bi = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  g2.paint = Color.BLACK
  g2.drawString(str, 16, 28)
  g2.dispose()
  return bi
}

private fun makeOvalBufferedImage(): BufferedImage {
  val bi = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  g2.paint = Color.RED
  g2.drawOval(8, 8, 16, 16)
  g2.dispose()
  return bi
}

private fun makeIconBufferedImage(icon: Icon, c: Component): BufferedImage {
  val w = icon.iconWidth
  val h = icon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  icon.paintIcon(c, g2, 0, 0)
  g2.dispose()
  return bi
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private class GreenBlueIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.GREEN
    g2.fillRect(8, 8, 8, 8)
    g2.paint = Color.BLUE
    g2.fillRect(16, 16, 8, 8)
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 32
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
