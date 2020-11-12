package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.border.TitledBorder

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/16x16.png")
  val path = url?.toString()
  val image = ImageIcon(url)
  val title1 = "<html><img src='$path' />test"
  val title2 = "<html><table cellpadding='0'><tr><td><img src='$path'></td><td>test</td></tr></table></html>"

  val border3 = object : TitledBorder("  test") {
    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
      super.paintBorder(c, g, x, y, width, height)
      g.drawImage(image.image, 5, 0, c)
    }
  }

  val label = JLabel("test")
  label.icon = image
  val border4 = ComponentTitledBorder(label, UIManager.getBorder("TitledBorder.border"))

  return JPanel(GridLayout(4, 1, 5, 5)).also {
    it.add(makeComponent(title1, BorderFactory.createTitledBorder(title1)))
    it.add(makeComponent(title2, BorderFactory.createTitledBorder(title2)))
    it.add(makeComponent("TitledBorder#paintBorder(...)", border3))
    it.add(makeComponent("ComponentTitledBorder", border4))

    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeComponent(str: String, border: Border) = JLabel().also {
  it.border = border
  it.putClientProperty("html.disable", true)
  it.text = str
}

private class ComponentTitledBorder(
  private val comp: Component,
  private val border: Border
) : Border, SwingConstants {
  init {
    (comp as? JComponent)?.isOpaque = true
  }

  override fun isBorderOpaque() = true

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    if (c is Container) {
      val borderInsets = border.getBorderInsets(c)
      val insets = getBorderInsets(c)
      val temp = (insets.top - borderInsets.top) / 2
      border.paintBorder(c, g, x, y + temp, width, height - temp)
      val size = comp.preferredSize
      val rect = Rectangle(OFFSET, 0, size.width, size.height)
      SwingUtilities.paintComponent(g, comp, c, rect)
      comp.bounds = rect
    }
  }

  override fun getBorderInsets(c: Component): Insets {
    val size = comp.preferredSize
    val insets = border.getBorderInsets(c)
    insets.top = insets.top.coerceAtLeast(size.height)
    return insets
  }

  companion object {
    private const val OFFSET = 5
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
