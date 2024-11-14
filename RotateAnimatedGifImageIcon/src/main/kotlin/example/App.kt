package example

import java.awt.*
import java.awt.geom.AffineTransform
import javax.swing.*

fun makeUI(): Component {
  val path = "example/duke.running.gif"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val imageIcon = ImageIcon(url)
  val label0 = JLabel(imageIcon)
  val title0 = "Default ImageIcon"
  label0.border = BorderFactory.createTitledBorder(title0)

  val label1 = JLabel(ClockwiseRotateIcon(imageIcon))
  val title1 = "Wrapping with another Icon"
  label1.border = BorderFactory.createTitledBorder(title1)

  val img = imageIcon.image
  val label2 = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      val x = width / 2.0
      val y = height / 2.0
      g2.transform = AffineTransform.getQuadrantRotateInstance(1, x, y)
      val x2 = x - img.getWidth(this) / 2.0
      val y2 = y - img.getHeight(this) / 2.0
      g2.drawImage(img, x2.toInt(), y2.toInt(), this)
      g2.dispose()
    }
  }
  val title2 = "Override JPanel#paintComponent(...)"
  label2.border = BorderFactory.createTitledBorder(title2)

  val icon3 = object : ImageIcon(url) {
    @Synchronized
    override fun paintIcon(
      c: Component,
      g: Graphics,
      x: Int,
      y: Int,
    ) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.translate(x + iconHeight, y)
      g2.transform(AffineTransform.getQuadrantRotateInstance(1))
      super.paintIcon(c, g2, 0, 0)
      g2.dispose()
    }

    override fun getIconWidth() = super.getIconHeight()

    override fun getIconHeight() = super.getIconWidth()
  }
  val label3 = JLabel(icon3)
  val title3 = "Override ImageIcon#paintIcon(...)"
  label3.border = BorderFactory.createTitledBorder(title3)

  return JPanel(GridLayout(2, 2)).also {
    it.add(label0)
    it.add(label1)
    it.add(label2)
    it.add(label3)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ClockwiseRotateIcon(
  private val icon: Icon,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x + icon.iconHeight, y)
    g2.transform(AffineTransform.getQuadrantRotateInstance(1))
    icon.paintIcon(c, g2, 0, 0)
    g2.dispose()
  }

  override fun getIconWidth() = icon.iconHeight

  override fun getIconHeight() = icon.iconWidth
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
