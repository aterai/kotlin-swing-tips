package example

import java.awt.*
import javax.swing.*
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
        c.toolTipText = if (cellTextWidth > rect.width) str else toolTipText
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
    val header = table.tableHeader
    val r = header.defaultRenderer
    val c = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    if (c is JLabel) {
      val i = c.insets
      val rect = header.getHeaderRect(column)
      rect.width -= i.left + i.right
      c.toolTipText = if (isClipped(c, rect)) c.text else header.toolTipText
    }
    return c
  }

  private fun isClipped(label: JLabel, viewR: Rectangle): Boolean {
    val iconR = Rectangle()
    val textR = Rectangle()
    val str = SwingUtilities.layoutCompoundLabel(
      label,
      label.getFontMetrics(label.font),
      label.text,
      label.icon,
      label.verticalAlignment,
      label.horizontalAlignment,
      label.verticalTextPosition,
      label.horizontalTextPosition,
      viewR,
      iconR,
      textR,
      label.iconTextGap
    )
    return label.text != str
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
