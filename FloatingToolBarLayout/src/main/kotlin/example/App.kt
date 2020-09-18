package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicToolBarUI

private const val ICON_SIZE = 32

private fun makeUI() = JPanel(BorderLayout()).also {
  it.add(Box.createHorizontalStrut(10), BorderLayout.NORTH)
  it.add(Box.createHorizontalStrut(10), BorderLayout.SOUTH)
  it.add(makeVerticalToolBar(), BorderLayout.EAST)
  it.add(JScrollPane(JTree()))
  it.preferredSize = Dimension(320, 240)
}

private fun makeVerticalToolBar() = JToolBar(SwingConstants.VERTICAL).also {
  val panel = object : JPanel() {
    override fun getPreferredSize() = if ((it.ui as? BasicToolBarUI)?.isFloating == true) {
      layout = GridLayout(0, 3)
      Dimension(ICON_SIZE * 3, ICON_SIZE * 2)
    } else {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      super.getPreferredSize()
    }

    override fun getMinimumSize() = preferredSize

    override fun getMaximumSize() = preferredSize
  }
  panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
  panel.add(ColorPanel(Color.RED))
  panel.add(ColorPanel(Color.GREEN))
  panel.add(ColorPanel(Color.BLUE))
  panel.add(ColorPanel(Color.ORANGE))
  panel.add(ColorPanel(Color.CYAN))
  it.add(panel)
  it.add(Box.createGlue())
}

private class ColorPanel(color: Color) : JPanel() {
  override fun getPreferredSize() = Dimension(ICON_SIZE, ICON_SIZE)

  override fun getMinimumSize() = preferredSize

  override fun getMaximumSize() = preferredSize

  init {
    background = color
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
