package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("family", "name", "postscript name")
  val model = object : DefaultTableModel(null, columnNames) {
    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = false

    override fun getColumnClass(column: Int) = String::class.java
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true

  GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts
    .map { arrayOf(it.family, it.name, it.psName) }
    .forEach { model.addRow(it) }

  val p = JPanel(BorderLayout())
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
