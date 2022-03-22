package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Path2D
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val r1 = makeRadioButton("red", ColorIcon(Color.RED))
  r1.selectedIcon = SelectedIcon(r1.icon, Color.GREEN)

  val r2 = makeRadioButton("green", ColorIcon(Color.GREEN))
  r2.selectedIcon = SelectedIcon(r2.icon, Color.BLUE)

  val r3 = makeRadioButton("blue", ColorIcon(Color.BLUE))
  r3.selectedIcon = SelectedIcon(r3.icon, Color.RED)

  val p = JPanel()
  val bg1 = ButtonGroup()
  listOf(r1, r2, r3).forEach {
    bg1.add(it)
    p.add(it)
  }

  val r4 = makeRadioButton("test1.jpg", makeIcon("example/test1.jpg"))
  r4.selectedIcon = SelectedIcon(r4.icon, Color.GREEN)

  val r5 = makeRadioButton("test2.jpg", makeIcon("example/test2.jpg"))
  r5.selectedIcon = SelectedIcon(r5.icon, Color.BLUE)

  val r6 = makeRadioButton("test3.jpg", makeIcon("example/test3.jpg"))
  r6.selectedIcon = SelectedIcon(r6.icon, Color.RED)

  val bg2 = ButtonGroup()
  listOf(r4, r5, r6).forEach {
    bg2.add(it)
    p.add(it)
  }
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeRadioButton(text: String, icon: Icon) = JRadioButton(text, icon).also {
  it.verticalAlignment = SwingConstants.BOTTOM
  it.verticalTextPosition = SwingConstants.BOTTOM
  it.horizontalAlignment = SwingConstants.CENTER
  it.horizontalTextPosition = SwingConstants.CENTER
}

private fun makeIcon(path: String): Icon {
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  return url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) }
    ?: UIManager.getIcon("OptionPane.errorIcon")
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 64

  override fun getIconHeight() = 48
}

private class SelectedIcon(private val icon: Icon, private val color: Color) : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.translate(x, y)
    icon.paintIcon(c, g2, 0, 0)
    val triangle = Path2D.Double()
    triangle.moveTo(iconWidth.toDouble(), iconHeight / 2.0)
    triangle.lineTo(iconWidth.toDouble(), iconHeight.toDouble())
    triangle.lineTo(iconWidth - iconHeight / 2.0, iconHeight.toDouble())
    triangle.closePath()
    g2.paint = color
    g2.fill(triangle)
    g2.stroke = BasicStroke(3f)
    g2.drawRect(0, 0, iconWidth, iconHeight)
    g2.paint = Color.WHITE
    val f = g2.font
    g2.drawString("✔", iconWidth - f.size, iconHeight - 3)
    g2.dispose()
  }

  override fun getIconWidth() = icon.iconWidth

  override fun getIconHeight() = icon.iconHeight
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
