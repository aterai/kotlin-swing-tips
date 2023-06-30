package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val desktop = JDesktopPane()
  for ((idx, c) in listOf(Color.RED, Color.GREEN, Color.BLUE).withIndex()) {
    desktop.add(makeInternalFrame(c, idx + 1))
  }
  EventQueue.invokeLater { desktop.allFrames.forEach { it.isVisible = true } }
  desktop.preferredSize = Dimension(320, 240)
  return desktop
}

private fun makeInternalFrame(color: Color, idx: Int): JInternalFrame {
  val f = JInternalFrame("Document #$idx", true, true, true, true)
  f.frameIcon = ColorIcon(color)
  f.setSize(240, 120)
  f.setLocation(10 + 20 * idx, 20 * idx)
  return f
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
