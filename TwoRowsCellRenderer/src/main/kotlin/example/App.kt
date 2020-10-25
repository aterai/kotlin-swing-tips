package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("A", "B")
  val data = arrayOf(
    arrayOf("123456789012345678901234567890123456789012345678901234567890", "12345"),
    arrayOf("bbb", "abcdefghijklmnopqrstuvwxyz----abcdefghijklmnopqrstuvwxyz")
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun isCellEditable(row: Int, column: Int) = false

    override fun getColumnClass(column: Int) = String::class.java
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.rowHeight = table.rowHeight * 2
  table.setDefaultRenderer(String::class.java, TwoRowsCellRenderer())
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TwoRowsCellRenderer : JPanel(GridLayout(2, 1, 0, 0)), TableCellRenderer {
  private val top = JLabel()
  private val bottom = JLabel()

  init {
    add(top)
    add(bottom)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    if (isSelected) {
      foreground = table.selectionForeground
      background = table.selectionBackground
    } else {
      foreground = table.foreground
      background = table.background
    }
    font = table.font
    val fm = top.getFontMetrics(top.font)
    val text = value?.toString() ?: ""
    var first = text
    var second = ""
    val columnWidth = table.getCellRect(0, column, false).width
    var textWidth = 0
    var i = 0
    while (i < text.length) {
      val cp = text.codePointAt(i)
      textWidth += fm.charWidth(cp)
      if (textWidth > columnWidth) {
        first = text.substring(0, i)
        second = text.substring(i)
        break
      }
      i += Character.charCount(cp)
    }
    top.text = first
    bottom.text = second
    return this
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
