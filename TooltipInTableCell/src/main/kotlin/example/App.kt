package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

private const val LIST_ICON_COLUMN = 1
private val columnNames = arrayOf("String", "List<Icon>")
private val informationIcon = getOptionPaneIcon("OptionPane.informationIcon")
private val errorIcon = getOptionPaneIcon("OptionPane.errorIcon")
private val questionIcon = getOptionPaneIcon("OptionPane.questionIcon")
private val warningIcon = getOptionPaneIcon("OptionPane.warningIcon")
private val data = arrayOf(
  arrayOf("aa", listOf(informationIcon, errorIcon)),
  arrayOf("bb", listOf(errorIcon, informationIcon, warningIcon, questionIcon)),
  arrayOf("cc", listOf(questionIcon, errorIcon, warningIcon)),
  arrayOf("dd", listOf(informationIcon)),
  arrayOf("ee", listOf(warningIcon, questionIcon)),
)
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = if (column == 1) {
    List::class.java
  } else {
    String::class.java
  }
}
private val table = object : JTable(model) {
  override fun getToolTipText(e: MouseEvent): String? {
    val pt = e.point
    val row = rowAtPoint(pt)
    val column = columnAtPoint(pt)
    val modelColumnIndex = convertColumnIndexToModel(column)
    if (modelColumnIndex == LIST_ICON_COLUMN) {
      val c = prepareRenderer(getCellRenderer(row, column), row, column)
      if (c is JPanel) {
        val r = getCellRect(row, column, true)
        c.bounds = r
        // @see https://stackoverflow.com/questions/10854831/tool-tip-in-jpanel-in-jtable-not-working
        c.doLayout()
        pt.translate(-r.x, -r.y)
        return SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y)
          ?.let { it as? JLabel }
          ?.let { (it.icon as? ImageIcon)?.description }
          ?: super.getToolTipText(e)
      }
    }
    return super.getToolTipText(e)
  }
}

fun makeUI(): Component {
  table.autoCreateRowSorter = true
  table.rowHeight = 40
  table.columnModel.getColumn(LIST_ICON_COLUMN).cellRenderer = ListIconRenderer()

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getOptionPaneIcon(key: String) = (UIManager.getIcon(key) as? ImageIcon)?.also {
  it.description = key
}

private class ListIconRenderer : TableCellRenderer {
  private val renderer = JPanel(FlowLayout(FlowLayout.LEFT))

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    renderer.removeAll()
    if (isSelected) {
      renderer.isOpaque = true
      renderer.background = table.selectionBackground
    } else {
      renderer.isOpaque = false
      // renderer.setBackground(table.getBackground())
    }
    if (value is List<*>) {
      value.filterIsInstance<Icon>()
        .map { makeLabel(it) }
        .forEach { renderer.add(it) }
    }
    return renderer
  }

  private fun makeLabel(icon: Icon) = JLabel(icon).also {
    it.toolTipText = icon.toString()
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
