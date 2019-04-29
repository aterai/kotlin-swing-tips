package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.HyperlinkEvent
import javax.swing.table.DefaultTableModel

class MainPanel : JPanel(BorderLayout()) {
  private val editor = JEditorPane("text/html", PLACEHOLDER)
  private val columnNames = arrayOf("Integer", "String", "Boolean")
  private val model = object : DefaultTableModel(null, columnNames) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> Number::class.java
      2 -> java.lang.Boolean::class.java
      else -> String::class.java
    }
  }
  private val table = JTable(model)

  init {
    editor.setOpaque(false)
    editor.setEditable(false)
    editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, java.lang.Boolean.TRUE)
    editor.addHyperlinkListener { e ->
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        Toolkit.getDefaultToolkit().beep()
      }
    }

    model.addTableModelListener { e ->
      val m = e.getSource() as DefaultTableModel
      editor.setVisible(m.getRowCount() == 0)
    }

    table.setAutoCreateRowSorter(true)
    table.setFillsViewportHeight(true)
    table.setComponentPopupMenu(TablePopupMenu())
    table.setLayout(GridBagLayout())
    table.add(editor)
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    private const val PLACEHOLDER = "<html>No data! <a href='dummy'>Input hint(beep)</a></html>"
  }
}

internal class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = getInvoker() as JTable
      val model = table.getModel() as DefaultTableModel
      model.addRow(arrayOf<Any>(model.getRowCount(), "New row", false))
      val r = table.getCellRect(model.getRowCount() - 1, 0, true)
      table.scrollRectToVisible(r)
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = getInvoker() as JTable
      val model = table.getModel() as DefaultTableModel
      val selection = table.getSelectedRows()
      for (i in selection.indices.reversed()) {
        model.removeRow(table.convertRowIndexToModel(selection[i]))
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    val table = c as? JTable ?: return
    delete.setEnabled(table.getSelectedRowCount() > 0)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
