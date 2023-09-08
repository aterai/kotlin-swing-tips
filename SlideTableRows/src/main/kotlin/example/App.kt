package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

const val START_HEIGHT = 4
const val END_HEIGHT = 24
const val DELAY = 10

val columnNames = arrayOf("String", "Integer", "Boolean")
val data = arrayOf(
  arrayOf("aaa", 12, true),
  arrayOf("bbb", 5, false),
  arrayOf("CCC", 92, true),
  arrayOf("DDD", 0, false),
)
val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = when (column) {
    0 -> String::class.java
    1 -> Number::class.java
    2 -> Boolean::class.javaObjectType
    else -> super.getColumnClass(column)
  }
}
val table = JTable(model)
val createAction = object : AbstractAction("add") {
  override fun actionPerformed(e: ActionEvent) {
    createActionPerformed()
  }
}
val deleteAction = object : AbstractAction("delete") {
  override fun actionPerformed(e: ActionEvent) {
    deleteActionPerformed()
  }
}

fun makeUI(): Component {
  table.fillsViewportHeight = true
  table.autoCreateRowSorter = true
  table.rowHeight = START_HEIGHT
  for (i in 0 until model.rowCount) {
    table.setRowHeight(i, END_HEIGHT)
  }

  val popup = object : JPopupMenu() {
    override fun show(c: Component?, x: Int, y: Int) {
      if (c is JTable) {
        deleteAction.isEnabled = c.selectedRowCount > 0
        super.show(c, x, y)
      }
    }
  }
  popup.add(createAction)
  popup.addSeparator()
  popup.add(deleteAction)

  val scroll = JScrollPane(table)
  scroll.componentPopupMenu = popup
  table.inheritsPopupMenu = true
  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(JButton(createAction), BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun createActionPerformed() {
  model.addRow(arrayOf("New name", model.rowCount, false))
  val index = table.convertRowIndexToView(model.rowCount - 1)
  var height = START_HEIGHT
  Timer(DELAY) { e ->
    if (height < END_HEIGHT) {
      table.setRowHeight(index, height++)
    } else {
      (e.source as? Timer)?.stop()
    }
  }.start()
}

fun deleteActionPerformed() {
  val selection = table.selectedRows
  if (selection.isEmpty()) {
    return
  }
  var height = END_HEIGHT
  Timer(DELAY) { e ->
    height--
    if (height > START_HEIGHT) {
      for (i in selection.indices.reversed()) {
        table.setRowHeight(selection[i], height)
      }
    } else {
      (e.source as? Timer)?.stop()
      for (i in selection.indices.reversed()) {
        model.removeRow(table.convertRowIndexToModel(selection[i]))
      }
    }
  }.start()
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
