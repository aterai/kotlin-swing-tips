package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    private val evenColor = Color(0xFA_FA_FA)

    override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int) =
      super.prepareRenderer(tcr, row, column).also {
        if (isCellSelected(row, column)) {
          it.foreground = getSelectionForeground()
          it.background = getSelectionBackground()
        } else {
          it.foreground = foreground
          it.background = if (row % 2 == 0) evenColor else background
        }
      }
  }
  table.cellSelectionEnabled = true
  val check = JCheckBox("Header click: Select all cells in a column", true)
  val header = table.tableHeader
  val ml = object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (!check.isSelected) {
        return
      }
      if (table.isEditing) {
        table.cellEditor.stopCellEditing()
      }
      val col = header.columnAtPoint(e.point)
      table.changeSelection(0, col, false, false)
      table.changeSelection(table.rowCount - 1, col, false, true)
    }
  }
  header.addMouseListener(ml)

  val button = JButton("clear selection")
  button.addActionListener { table.clearSelection() }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.add(button, BorderLayout.SOUTH)
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
