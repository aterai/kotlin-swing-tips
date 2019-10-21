package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

const val START_HEIGHT = 4
const val END_HEIGHT = 24
const val DELAY = 10

val columnNames = arrayOf("String", "Integer", "Boolean")
val data = arrayOf(
  arrayOf("aaa", 12, true),
  arrayOf("bbb", 5, false),
  arrayOf("CCC", 92, true),
  arrayOf("DDD", 0, false))
val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = when (column) {
    0 -> String::class.java
    1 -> Number::class.java
    2 -> java.lang.Boolean::class.java
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
  table.setFillsViewportHeight(true)
  table.setAutoCreateRowSorter(true)
  table.setRowHeight(START_HEIGHT)
  for (i in 0 until model.getRowCount()) {
    table.setRowHeight(i, END_HEIGHT)
  }

  val popup = object : JPopupMenu() {
    override fun show(c: Component, x: Int, y: Int) {
      val table = c as? JTable ?: return
      deleteAction.setEnabled(table.getSelectedRowCount() > 0)
      super.show(table, x, y)
    }
  }
  popup.add(createAction)
  popup.addSeparator()
  popup.add(deleteAction)

  val scroll = JScrollPane(table)
  scroll.setComponentPopupMenu(popup)
  table.setInheritsPopupMenu(true)
  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(JButton(createAction), BorderLayout.SOUTH)
    it.setPreferredSize(Dimension(320, 240))
  }
}

fun createActionPerformed() {
  model.addRow(arrayOf("New name", model.getRowCount(), false))
  Timer(DELAY, object : ActionListener {
    private val index = table.convertRowIndexToView(model.getRowCount() - 1)
    private var height = START_HEIGHT
    override fun actionPerformed(e: ActionEvent) {
      if (height < END_HEIGHT) {
        table.setRowHeight(index, height++)
      } else {
        (e.getSource() as? Timer)?.stop()
      }
    }
  }).start()
}

fun deleteActionPerformed() {
  val selection = table.getSelectedRows()
  if (selection.isEmpty()) {
    return
  }
  Timer(DELAY, object : ActionListener {
    private var height = END_HEIGHT
    override fun actionPerformed(e: ActionEvent) {
      height--
      if (height > START_HEIGHT) {
        for (i in selection.indices.reversed()) {
          table.setRowHeight(selection[i], height)
        }
      } else {
        (e.getSource() as? Timer)?.stop()
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }).start()
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
