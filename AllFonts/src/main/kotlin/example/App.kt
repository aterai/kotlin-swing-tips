package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("family", "name", "postscript name")
  val model = object : DefaultTableModel(null, columnNames) {
    override fun isCellEditable(row: Int, column: Int) = false

    override fun getColumnClass(column: Int) = String::class.java
  }
  val table = JTable(model)
  table.setAutoCreateRowSorter(true)

  GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()
    .map { arrayOf(it.getFamily(), it.getName(), it.getPSName()) }
    .forEach { model.addRow(it) }

  val p = JPanel(BorderLayout())
  p.add(JScrollPane(table))
  p.setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
