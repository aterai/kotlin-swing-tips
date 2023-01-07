package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val icon1 = ColorIcon(Color.RED)
  val icon2 = ColorIcon(Color.GREEN)
  val icon3 = ColorIcon(Color.BLUE)

  val toolBar1 = JToolBar("ToolBarButton").also {
    it.add(JButton(icon1))
    it.add(JButton(icon2))
    it.add(Box.createGlue())
    it.add(JButton(icon3))
  }

  val toolBar2 = JToolBar("JButton").also {
    it.add(createToolBarButton(icon1))
    it.add(createToolBarButton(icon2))
    it.add(Box.createGlue())
    it.add(createToolBarButton(icon3))
  }

  return JPanel(BorderLayout()).also {
    it.add(toolBar1, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea()))
    it.add(toolBar2, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createToolBarButton(icon: Icon) = JButton(icon).also {
  it.isRequestFocusEnabled = false
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.translate(x, y)
    g2.paint = color
    g2.fillOval(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 24

  override fun getIconHeight() = 24
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
