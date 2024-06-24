package example

import java.awt.*
import java.util.Locale
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val see = "See Also: Constant Field Values"
  val columnNames = arrayOf("AAA", "BBB")
  val data = arrayOf(
    arrayOf(makeOptionPaneDescription("error"), see),
    arrayOf(makeOptionPaneDescription("information"), see),
    arrayOf(makeOptionPaneDescription("question"), see),
    arrayOf(makeOptionPaneDescription("warning"), see),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = false
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      setColumnCellRenderer(null)
      super.updateUI()
      val r = ColumnSpanningCellRenderer()
      setColumnCellRenderer(r)
    }

    private fun setColumnCellRenderer(renderer: TableCellRenderer?) {
      val cm = getColumnModel()
      for (i in 0..<cm.columnCount) {
        val c = cm.getColumn(i)
        c.cellRenderer = renderer
        c.minWidth = 50
      }
    }
  }

  table.autoCreateRowSorter = true
  table.tableHeader.reorderingAllowed = false
  table.rowSelectionAllowed = true
  table.fillsViewportHeight = true
  table.showVerticalLines = false
  table.intercellSpacing = Dimension(0, 1)
  table.rowHeight = 56
  val renderer = ColumnSpanningCellRenderer()
  for (i in 0..<table.columnModel.columnCount) {
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
  val msg = fmt.format(type.uppercase(Locale.ENGLISH), type)
  return OptionPaneDescription(key, icon, msg)
}

private class ColumnSpanningCellRenderer : TableCellRenderer {
  private val textArea = JTextArea(2, 999_999)
  private val label = JLabel()
  private val iconLabel = JLabel()
  private val scroll = JScrollPane(textArea)
  private val renderer = JPanel(BorderLayout())

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
    renderer.background = textArea.background
    renderer.isOpaque = true
    renderer.add(label, BorderLayout.NORTH)
    renderer.add(scroll)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val d: OptionPaneDescription
    if (value is OptionPaneDescription) {
      d = value
      renderer.add(iconLabel, BorderLayout.WEST)
    } else {
      val title = value?.toString() ?: ""
      val mri = table.convertRowIndexToModel(row)
      val o = table.model.getValueAt(mri, 0)
      d = if (o is OptionPaneDescription) {
        OptionPaneDescription(title, o.icon, o.text)
      } else {
        OptionPaneDescription(title, null, "")
      }
      renderer.remove(iconLabel)
    }
    label.text = d.title
    textArea.text = d.text
    iconLabel.icon = d.icon
    val cr = table.getCellRect(row, column, false)
    if (column != TARGET_IDX) {
      cr.x -= iconLabel.preferredSize.width
    }
    scroll.viewport.viewPosition = cr.location
    val bgc = if (isSelected) Color.ORANGE else Color.WHITE
    renderer.background = bgc
    textArea.background = bgc
    return renderer
  }

  companion object {
    private const val TARGET_IDX = 0
  }
}

private data class OptionPaneDescription(val title: String, val icon: Icon?, val text: String)

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
