package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

fun makeUI(): Component {
  val columnNames = arrayOf("AAA", "BBB", "CCC")
  val data = arrayOf(
    arrayOf("aaa", "eee", "fff"),
    arrayOf("bbb", "lll", "kk"),
    arrayOf("CCC", "g", "hh"),
    arrayOf("DDD", "iii", "j")
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = String::class.java
  }
  val table = JTable(model)
  // table.setAutoCreateColumnsFromModel(true)
  table.fillsViewportHeight = true
  table.tableHeader.componentPopupMenu = TablePopupMenu(columnNames)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TablePopupMenu(columnNames: Array<String>) : JPopupMenu() {
  private var index = -1
  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTableHeader)?.also { header ->
      header.draggedColumn = null
      header.repaint()
      header.table.repaint()
      index = header.columnAtPoint(Point(x, y))
      super.show(c, x, y)
    }
  }

  init {
    val textField = JTextField()
    textField.addAncestorListener(FocusAncestorListener())
    add("Edit: setHeaderValue").addActionListener {
      val header = invoker as JTableHeader
      val column = header.columnModel.getColumn(index)
      val name = column.headerValue.toString()
      textField.text = name
      val result = JOptionPane.showConfirmDialog(
        header.table, textField, "Edit: setHeaderValue",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
      )
      if (result == JOptionPane.OK_OPTION) {
        val str = textField.text.trim()
        if (str != name) {
          column.headerValue = str
          header.repaint(header.getHeaderRect(index))
        }
      }
    }
    add("Edit: setColumnIdentifiers").addActionListener {
      val header = invoker as JTableHeader
      val table = header.table
      val model = table.model as DefaultTableModel
      val name = table.getColumnName(index)
      textField.text = name
      val result = JOptionPane.showConfirmDialog(
        table, textField, "Edit: setColumnIdentifiers",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
      )
      if (result == JOptionPane.OK_OPTION) {
        val str = textField.text.trim { it <= ' ' }
        if (str != name) {
          columnNames[table.convertColumnIndexToModel(index)] = str
          model.setColumnIdentifiers(columnNames)
        }
      }
    }
  }
}

private class FocusAncestorListener : AncestorListener {
  override fun ancestorAdded(e: AncestorEvent) {
    e.component.requestFocusInWindow()
  }

  override fun ancestorMoved(e: AncestorEvent) {
    /* not needed */
  }

  override fun ancestorRemoved(e: AncestorEvent) {
    /* not needed */
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
