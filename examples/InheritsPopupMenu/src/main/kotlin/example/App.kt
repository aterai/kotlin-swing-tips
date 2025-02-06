package example

import java.awt.*
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
    override fun getColumnClass(column: Int) = when (column) {
      0 -> String::class.java
      1 -> Number::class.java
      2 -> Boolean::class.javaObjectType
      else -> super.getColumnClass(column)
    }
  }

  val table = JTable(model)
  table.autoCreateRowSorter = true

  val delete = JMenuItem("delete")
  delete.addActionListener {
    val selection = table.selectedRows
    for (i in selection.indices.reversed()) {
      model.removeRow(table.convertRowIndexToModel(selection[i]))
    }
  }

  val popup = object : JPopupMenu() {
    override fun show(
      c: Component?,
      x: Int,
      y: Int,
    ) {
      delete.isEnabled = table.selectedRowCount > 0
      super.show(c, x, y)
    }
  }
  popup.add("add").addActionListener {
    model.addRow(arrayOf("example", 0, false))
  }
  popup.addSeparator()
  popup.add(delete)

  val scroll = JScrollPane(table)
  scroll.background = Color.RED
  scroll.viewport.background = Color.GREEN
  scroll.componentPopupMenu = popup

  table.inheritsPopupMenu = true
  table.fillsViewportHeight = true
  table.background = Color.YELLOW
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF

  val cb1 = JCheckBox("InheritsPopupMenu", true)
  cb1.addActionListener { e ->
    table.inheritsPopupMenu = (e.source as? JCheckBox)?.isSelected == true
  }

  val cb2 = JCheckBox("FillsViewportHeight", true)
  cb2.addActionListener { e ->
    table.fillsViewportHeight = (e.source as? JCheckBox)?.isSelected == true
  }

  val box = Box.createHorizontalBox()
  box.add(cb1)
  box.add(cb2)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(scroll)
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
