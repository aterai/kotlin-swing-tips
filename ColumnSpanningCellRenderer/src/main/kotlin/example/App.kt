package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val see = "See Also: Constant Field Values"
  val columnNames = arrayOf("AAA", "BBB")
  val data = arrayOf(
    arrayOf(makeOptionPaneDescription("error"), see),
    arrayOf(makeOptionPaneDescription("information"), see),
    arrayOf(makeOptionPaneDescription("question"), see),
    arrayOf(makeOptionPaneDescription("warning"), see)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun isCellEditable(row: Int, column: Int) = false
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.tableHeader.reorderingAllowed = false
  table.rowSelectionAllowed = true
  table.fillsViewportHeight = true
  table.showVerticalLines = false
  table.intercellSpacing = Dimension(0, 1)
  table.rowHeight = 56
  val renderer = ColumnSpanningCellRenderer()
  for (i in 0 until table.columnModel.columnCount) {
    table.columnModel.getColumn(i).also {
      it.cellRenderer = renderer
      it.minWidth = 50
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeOptionPaneDescription(type: String): OptionPaneDescription {
  val key = type + "Icon"
  val icon = UIManager.getIcon("OptionPane.$key")
  val fmt = "public static final int %s_MESSAGE%nUsed for %s messages."
  val msg = fmt.format(type.toUpperCase(Locale.ENGLISH), type)
  return OptionPaneDescription(key, icon, msg)
}

private class ColumnSpanningCellRenderer : JPanel(BorderLayout()), TableCellRenderer {
  private val textArea = JTextArea(2, 999_999)
  private val label = JLabel()
  private val iconLabel = JLabel()
  private val scroll = JScrollPane(textArea)

  init {
    scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    scroll.border = BorderFactory.createEmptyBorder()
    scroll.viewportBorder = BorderFactory.createEmptyBorder()
    scroll.isOpaque = false
    scroll.viewport.isOpaque = false
    textArea.border = BorderFactory.createEmptyBorder()
    textArea.margin = Insets(0, 0, 0, 0)
    textArea.foreground = Color.RED
    textArea.isEditable = false
    textArea.isFocusable = false
    textArea.isOpaque = false
    iconLabel.border = BorderFactory.createEmptyBorder(0, 4, 0, 4)
    iconLabel.isOpaque = false
    val b1 = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    val b2 = BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY)
    label.border = BorderFactory.createCompoundBorder(b2, b1)
    background = textArea.background
    isOpaque = true
    add(label, BorderLayout.NORTH)
    add(scroll)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val d: OptionPaneDescription
    if (value is OptionPaneDescription) {
      d = value
      add(iconLabel, BorderLayout.WEST)
    } else {
      val title = value?.toString() ?: ""
      val mri = table.convertRowIndexToModel(row)
      val o = table.model.getValueAt(mri, 0)
      d = if (o is OptionPaneDescription) {
        OptionPaneDescription(title, o.icon, o.text)
      } else {
        OptionPaneDescription(title, null, "")
      }
      remove(iconLabel)
    }
    label.text = d.title
    textArea.text = d.text
    iconLabel.icon = d.icon
    val cr = table.getCellRect(row, column, false)
    if (column != TARGET_IDX) {
      cr.x -= iconLabel.preferredSize.width
    }
    scroll.viewport.viewPosition = cr.location
    background = if (isSelected) Color.ORANGE else Color.WHITE
    return this
  }

  companion object {
    private const val TARGET_IDX = 0
  }
}

data class OptionPaneDescription(val title: String, val icon: Icon?, val text: String)

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
