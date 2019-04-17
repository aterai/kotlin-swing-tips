package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

class MainPanel : JPanel(BorderLayout()) {
  protected val columnNames = arrayOf("String", "Integer", "Boolean")
  protected val data = arrayOf(
      arrayOf<Any>("aaa", 12, true),
      arrayOf<Any>("bbb", 5, false),
      arrayOf<Any>("CCC", 92, true),
      arrayOf<Any>("DDD", 0, false))
  protected val model: DefaultTableModel = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> String::class.java
      1 -> Number::class.java
      2 -> Boolean::class.java
      else -> super.getColumnClass(column)
    }
  }
  protected val table = JTable(model)
  protected val createAction: Action = object : AbstractAction("add") {
    override fun actionPerformed(e: ActionEvent) {
      createActionPerformed()
    }
  }
  protected val deleteAction: Action = object : AbstractAction("delete") {
    override fun actionPerformed(e: ActionEvent) {
      deleteActionPerformed()
    }
  }

  init {
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
    add(scroll)
    add(JButton(createAction), BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  protected fun createActionPerformed() {
    model.addRow(arrayOf<Any>("New name", model.getRowCount(), false))
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

  protected fun deleteActionPerformed() {
    val selection = table.getSelectedRows()
    if (selection.size == 0) {
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

  companion object {
    protected const val START_HEIGHT = 4
    protected const val END_HEIGHT = 24
    protected const val DELAY = 10
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
