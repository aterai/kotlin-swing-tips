package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

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
  val table = JTable(model)
  table.autoCreateRowSorter = true

  val check1 = JCheckBox("UpdateSelectionOnSort", true)
  check1.addActionListener { e ->
    table.updateSelectionOnSort = (e.source as? JCheckBox)?.isSelected == true
  }

  val check2 = JCheckBox("ClearSelectionOnSort", false)
  val ml = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (!check2.isSelected) {
        return
      }
      if (table.isEditing) {
        table.cellEditor.stopCellEditing()
      }
      table.clearSelection()
    }
  }
  table.tableHeader.addMouseListener(ml)

  val p = JPanel(FlowLayout(FlowLayout.LEFT))
  p.add(check1)
  p.add(check2)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(table))
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
