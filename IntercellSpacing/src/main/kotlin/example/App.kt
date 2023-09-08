package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun isCellEditable(row: Int, column: Int) = column == 2

    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int) =
      super.prepareEditor(editor, row, column).also {
        (it as? JCheckBox)?.background = getSelectionBackground()
      }
  }
  table.autoCreateRowSorter = true
  table.rowSelectionAllowed = true
  table.fillsViewportHeight = true
  table.isFocusable = false
  table.showVerticalLines = false
  table.showHorizontalLines = false
  table.intercellSpacing = Dimension()

  val verticalLinesButton = JCheckBox("setShowVerticalLines")
  verticalLinesButton.addActionListener { e ->
    val d = table.intercellSpacing
    if ((e.source as? JCheckBox)?.isSelected == true) {
      table.showVerticalLines = true
      table.intercellSpacing = Dimension(1, d.height)
    } else {
      table.showVerticalLines = false
      table.intercellSpacing = Dimension(0, d.height)
    }
  }
  val horizontalLinesButton = JCheckBox("setShowHorizontalLines")
  horizontalLinesButton.addActionListener { e ->
    val d = table.intercellSpacing
    if ((e.source as? JCheckBox)?.isSelected == true) {
      table.showHorizontalLines = true
      table.intercellSpacing = Dimension(d.width, 1)
    } else {
      table.showHorizontalLines = false
      table.intercellSpacing = Dimension(d.width, 0)
    }
  }

  val p = JPanel(BorderLayout())
  p.add(verticalLinesButton, BorderLayout.WEST)
  p.add(horizontalLinesButton, BorderLayout.EAST)

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
