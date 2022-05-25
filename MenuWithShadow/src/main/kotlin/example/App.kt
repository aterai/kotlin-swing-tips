package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/test.png")
  val image = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  it.add(JLabel(ImageIcon(image)))
  it.preferredSize = Dimension(320, 240)
}

private fun createMenuBar(): JMenuBar {
  val mb = JMenuBar()
  var menu = JMenu("File")
  mb.add(menu)
  menu.add("Open")
  menu.add("Save")
  menu.add("Close")
  menu.add("Exit")
  menu = JMenu("Edit")
  mb.add(menu)
  menu.add("Cut")
  menu.add("Copy")
  menu.add("Paste")
  val sub = JMenu("Edit").also {
    it.add("Cut")
    it.add("Copy")
    it.add("Paste")
  }
  menu.add(sub)
  return mb
}

private fun makeMissingImage(): Image {
  val missingIcon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class MissingIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.color = Color.WHITE
    g2.fillRect(x, y, w, h)
    g2.color = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 320

  override fun getIconHeight() = 240
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      // https://youtrack.jetbrains.com/issue/KT-12993
      UIManager.put("PopupMenuUI", "example.CustomPopupMenuUI")
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
