package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val combo = JComboBox(arrayOf("A", "BB", "CCC", "DD", "E"))
  combo.selectedIndex = -1

  val b1 = JButton("setSelectedIndex(0)")
  b1.addActionListener { combo.selectedIndex = 0 }

  val b2 = JButton("setSelectedIndex(-1)")
  b2.addActionListener { combo.selectedIndex = -1 }

  val b3 = JButton("setSelectedItem(null)")
  b3.addActionListener { combo.selectedItem = null }

  val box = JPanel(GridLayout(0, 1, 10, 10))
  listOf<JComponent>(b1, b2, b3, combo).forEach { box.add(it) }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.preferredSize = Dimension(320, 240)
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
