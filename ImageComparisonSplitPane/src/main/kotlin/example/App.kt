package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
  split.isContinuousLayout = true
  split.resizeWeight = .5

  val check = JCheckBox("setXORMode(Color.BLUE)", true)
  check.addActionListener { split.repaint() }

  val path = "example/test.png"
  val cl = Thread.currentThread().contextClassLoader
  val img = cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val icon = ImageIcon(img)

  val beforeCanvas = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      icon.paintIcon(this, g, 0, 0)
    }
  }
  split.leftComponent = beforeCanvas

  val afterCanvas = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      if (check.isSelected) {
        g2.color = background
        g2.setXORMode(Color.BLUE)
      } else {
        g2.setPaintMode()
      }
      g2.translate(-location.x + split.insets.left, 0)
      icon.paintIcon(this, g2, 0, 0)
      g2.dispose()
    }
  }
  split.rightComponent = afterCanvas

  return JPanel(BorderLayout()).also {
    it.add(split)
    it.add(check, BorderLayout.SOUTH)
    it.isOpaque = false
    it.preferredSize = Dimension(320, 240)
  }
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
