package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("String-String/String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("1234567890123456789012345678901234567890", 12, true),
    arrayOf("BBB", 2, true),
    arrayOf("EEE", 3, false),
    arrayOf("CCC", 4, true),
    arrayOf("FFF", 5, false),
    arrayOf("DDD", 6, true),
    arrayOf("GGG", 7, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component {
      val c = super.prepareRenderer(tcr, row, column)
      if (c is JComponent) {
        val i = c.insets
        val rect = getCellRect(row, column, false)
        rect.width -= i.left + i.right
        val fm = c.getFontMetrics(c.font)
        val str = getValueAt(row, column)?.toString() ?: ""
        val cellTextWidth = fm.stringWidth(str)
        c.toolTipText = if (cellTextWidth > rect.width) str else null
      }
      return c
    }

    override fun updateUI() {
      super.updateUI()
      val r = ToolTipHeaderRenderer()
      for (i in 0 until getColumnModel().columnCount) {
        getColumnModel().getColumn(i).headerRenderer = r
      }
    }
  }
  table.autoCreateRowSorter = true

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ToolTipHeaderRenderer : TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val renderer = table.tableHeader.defaultRenderer
    val l = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
    val i = l.insets
    val rect = table.getCellRect(row, column, false)
    rect.width -= i.left + i.right
    l.icon?.also { rect.width -= it.iconWidth + l.iconTextGap }
    val fm = l.getFontMetrics(l.font)
    val str = value?.toString() ?: ""
    val cellTextWidth = fm.stringWidth(str)
    l.toolTipText = if (cellTextWidth > rect.width) str else null
    return l
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
