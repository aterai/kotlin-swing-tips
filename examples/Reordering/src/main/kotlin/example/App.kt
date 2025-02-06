package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val table = JTable(6, 3)
  val header = table.tableHeader
  header.reorderingAllowed = false
  val checkBox = JCheckBox("Disable reordering by dragging columns", true)
  checkBox.addActionListener {
    header.reorderingAllowed = (it.source as? JCheckBox)?.isSelected != true
  }
  val p = JPanel(BorderLayout())
  p.add(checkBox, BorderLayout.NORTH)
  p.add(JScrollPane(table))
  p.preferredSize = Dimension(320, 240)
  return p
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
