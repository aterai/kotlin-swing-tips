package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val path = "example/duke.gif"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val bi = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val icon = makeLabelIcon(bi)
  icon.cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
  icon.size = icon.preferredSize
  icon.setLocation(20, 20)

  val desktop = JDesktopPane()
  desktop.isOpaque = false
  desktop.add(icon)

  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeLabelIcon(image: BufferedImage): JLabel {
  val handler = ComponentMoveHandler()
  return object : JLabel(ImageIcon(image)) {
    override fun contains(x: Int, y: Int) =
      super.contains(x, y) && image.getRGB(x, y) shr 24 and 0xFF != 0

    override fun updateUI() {
      removeMouseListener(handler)
      removeMouseMotionListener(handler)
      super.updateUI()
      addMouseListener(handler)
      addMouseMotionListener(handler)
    }
  }
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class ComponentMoveHandler : MouseAdapter() {
  private val startPt = Point()
  override fun mousePressed(e: MouseEvent) {
    startPt.location = e.point
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    c.setLocation(c.x - startPt.x + e.x, c.y - startPt.y + e.y)
  }
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
