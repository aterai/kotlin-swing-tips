package example

import java.awt.*
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val textArea = JTextArea()
  textArea.isEditable = false
  val model = makeModel()

  val table0 = JTable(model)
  table0.cellSelectionEnabled = true
  val selectionListener0 = object : AbstractTableCellSelectionListener() {
    override fun valueChanged(e: ListSelectionEvent) {
      val sr = table0.selectedRow
      val sc = table0.selectedColumn
      if (!e.valueIsAdjusting && updateRowColumnInfo(sr, sc)) {
        val o = table0.getValueAt(sr, sc)
        textArea.append("($sr, $sc) $o\n")
        textArea.caretPosition = textArea.document.length
      }
    }
  }
  table0.selectionModel.addListSelectionListener(selectionListener0)
  table0.columnModel.selectionModel.addListSelectionListener(selectionListener0)

  val table1 = JTable(model)
  table1.cellSelectionEnabled = true
  val selectionListener1 = object : AbstractTableCellSelectionListener() {
    override fun valueChanged(e: ListSelectionEvent) {
      val sr = table1.selectionModel.leadSelectionIndex
      val sc = table1.columnModel.selectionModel.leadSelectionIndex
      if (!e.valueIsAdjusting && updateRowColumnInfo(sr, sc)) {
        val o = table1.getValueAt(sr, sc)
        textArea.append("($sr, $sc) $o\n")
        textArea.caretPosition = textArea.document.length
      }
    }
  }
  table1.selectionModel.addListSelectionListener(selectionListener1)
  table1.columnModel.selectionModel.addListSelectionListener(selectionListener1)

  val table2 = JTable(model)
  table2.cellSelectionEnabled = true
  table2.selectionModel.addListSelectionListener { e ->
    if (!e.valueIsAdjusting) {
      textArea.append("row first, last: ${e.firstIndex}, ${e.lastIndex}\n")
      (e.source as? ListSelectionModel)?.also {
        val anchor = it.anchorSelectionIndex
        val lead = it.leadSelectionIndex
        val msg = "row anchor->lead: $anchor->$lead\n"
        textArea.append(msg)
      }
      textArea.caretPosition = textArea.document.length
    }
  }
  table2.columnModel.selectionModel.addListSelectionListener { e ->
    if (!e.valueIsAdjusting) {
      textArea.append("column first, last: ${e.firstIndex}, ${e.lastIndex}\n")
      (e.source as? ListSelectionModel)?.also {
        val anchor = it.anchorSelectionIndex
        val lead = it.leadSelectionIndex
        val msg = "column anchor->lead: $anchor->$lead\n"
        textArea.append(msg)
      }
      textArea.caretPosition = textArea.document.length
    }
  }

  val table3 = object : JTable(model) {
    override fun changeSelection(
      rowIndex: Int,
      colIndex: Int,
      toggle: Boolean,
      extend: Boolean,
    ) {
      super.changeSelection(rowIndex, colIndex, toggle, extend)
      textArea.append("changeSelection: $rowIndex, $colIndex\n")
      textArea.caretPosition = textArea.document.length
    }
  }
  table3.cellSelectionEnabled = true

  val tabbedPane = JTabbedPane()
  tabbedPane.addChangeListener { textArea.text = "" }
  tabbedPane.addTab("JTable", table0)
  tabbedPane.addTab("SelectionModel", table1)
  tabbedPane.addTab("Row/Column", table2)
  tabbedPane.addTab("changeSelection", table3)

  return JPanel(GridLayout(2, 1)).also {
    it.add(tabbedPane)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel() = object : DefaultTableModel() {
  override fun getColumnClass(column: Int) = Number::class.java

  override fun getRowCount() = 6

  override fun getColumnCount() = 7

  override fun getValueAt(
    row: Int,
    column: Int,
  ) = row * columnCount + column

  override fun isCellEditable(
    row: Int,
    column: Int,
  ) = false
}

private abstract class AbstractTableCellSelectionListener : ListSelectionListener {
  private var prevRow = -1
  private var prevCol = -1

  protected fun updateRowColumnInfo(
    sr: Int,
    sc: Int,
  ): Boolean {
    val flg = prevRow == sr && prevCol == sc
    prevRow = sr
    prevCol = sc
    return !flg
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
