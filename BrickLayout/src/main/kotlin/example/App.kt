package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val X_SIZE = 6
private const val Y_SIZE = 8
private const val WIDTH = 2

fun makeUI(): Component {
  val panel = JPanel(GridBagLayout())
  panel.border = BorderFactory.createTitledBorder("Brick Layout")
  val c = GridBagConstraints()
  c.fill = GridBagConstraints.HORIZONTAL
  for (y in 0 until Y_SIZE) {
    c.gridx = y and 1 // start x offset
    c.gridwidth = WIDTH
    (0 until X_SIZE).forEach { _ ->
      panel.add(makeBrick(), c)
      c.gridx += WIDTH
    }
  }
  // GridBagLayout to create a board
  // https://community.oracle.com/thread/1357310
  // <guide-row>
  c.gridwidth = 1
  c.gridx = 0
  while (c.gridx <= WIDTH * X_SIZE) {
    panel.add(Box.createHorizontalStrut(24), c)
    c.gridx++
  }
  // </guide-row>

  panel.preferredSize = Dimension(320, 240)
  return panel
}

private fun makeBrick() = JButton(" ")

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
