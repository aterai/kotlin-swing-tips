package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.HyperlinkEvent
import javax.swing.table.DefaultTableModel

private const val PLACEHOLDER = "<html>No data! <a href='dummy'>Input hint(beep)</a></html>"

fun makeUI(): Component {
  val editor = JEditorPane("text/html", PLACEHOLDER)
  editor.isOpaque = false
  editor.isEditable = false
  editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  editor.addHyperlinkListener { e ->
    if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
      Toolkit.getDefaultToolkit().beep()
    }
  }

  val columnNames = arrayOf("Integer", "String", "Boolean")
  val model = object : DefaultTableModel(null, columnNames) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> Number::class.java
      2 -> java.lang.Boolean::class.java
      else -> String::class.java
    }
  }
  model.addTableModelListener { e ->
    (e.source as? DefaultTableModel)?.also {
      editor.isVisible = it.rowCount == 0
    }
  }

  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()
  table.layout = GridBagLayout()
  table.add(editor)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (table is JTable && model is DefaultTableModel) {
        model.addRow(arrayOf(model.rowCount, "New row", false))
        val r = table.getCellRect(model.rowCount - 1, 0, true)
        table.scrollRectToVisible(r)
      }
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (table is JTable && model is DefaultTableModel) {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    val table = c as? JTable ?: return
    delete.isEnabled = table.selectedRowCount > 0
    super.show(table, x, y)
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
