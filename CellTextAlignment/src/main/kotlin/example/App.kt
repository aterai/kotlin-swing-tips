package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

private val leftRadio = JRadioButton("left", true)
private val centerRadio = JRadioButton("center")
private val rightRadio = JRadioButton("right")
private val customRadio = JRadioButton("custom")

fun makeUI(): Component {
  val columnNames = arrayOf("Integer", "String", "Boolean")
  val data = arrayOf(
    arrayOf(12, "aaa", true),
    arrayOf(5, "bbb", false),
    arrayOf(92, "CCC", true),
    arrayOf(0, "DDD", false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true

  var col = table.columnModel.getColumn(0)
  col.minWidth = 60
  col.maxWidth = 60
  col.resizable = false
  col = table.columnModel.getColumn(1)
  col.cellRenderer = HorizontalAlignmentTableRenderer()
  col = table.columnModel.getColumn(2)
  col.headerRenderer = HeaderRenderer()

  val bg = ButtonGroup()
  val p = JPanel()
  listOf(leftRadio, centerRadio, rightRadio, customRadio)
    .forEach {
      bg.add(it)
      p.add(it)
      it.addActionListener { table.repaint() }
    }
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class HorizontalAlignmentTableRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    if (c is JLabel) {
      initLabel(c, row)
    }
    return c
  }
}

fun initLabel(l: JLabel, row: Int) {
  l.horizontalAlignment = when {
    leftRadio.isSelected -> SwingConstants.LEFT
    centerRadio.isSelected -> SwingConstants.CENTER
    rightRadio.isSelected -> SwingConstants.RIGHT
    else -> when { // customRadio.isSelected
      row % 3 == 0 -> SwingConstants.LEFT
      row % 3 == 1 -> SwingConstants.CENTER
      else -> SwingConstants.RIGHT
    }
  }
}

private class HeaderRenderer : TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val hr = table.tableHeader.defaultRenderer
    val c = hr.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    if (c is JLabel) {
      c.horizontalAlignment = SwingConstants.CENTER
      c.font = FONT
    }
    return c
  }

  companion object {
    private val FONT = Font(Font.SANS_SERIF, Font.BOLD, 14)
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
