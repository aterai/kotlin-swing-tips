package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

private const val AUTO_WRAP_COLUMN = 1

fun createUI(): Component {
  val columnNames = arrayOf("Default", "AutoWrap")
  val data = arrayOf(
    arrayOf("123456789012345678901234567890", "123456789012345678901234567890"),
    arrayOf("1111", "22222222222222222222222222222222222222222222222222222222"),
    arrayOf("3333333", "----------------------------------------------0"),
    arrayOf("4444444444444444444", ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>|"),
  )
  val model = DefaultTableModel(data, columnNames)
  val table = object : JTable(model) {
    private val evenColor = Color(0xE6_F0_FF)

    override fun prepareRenderer(
      tcr: TableCellRenderer,
      row: Int,
      column: Int,
    ): Component {
      val c = super.prepareRenderer(tcr, row, column)
      if (isRowSelected(row)) {
        c.foreground = getSelectionForeground()
        c.background = getSelectionBackground()
      } else {
        c.foreground = foreground
        c.background = if (row % 2 == 0) evenColor else background
      }
      return c
    }

    override fun updateUI() {
      columnModel.getColumn(AUTO_WRAP_COLUMN).cellRenderer = null
      super.updateUI()
      isEnabled = false
      setShowGrid(false)
      columnModel.getColumn(AUTO_WRAP_COLUMN).cellRenderer = TextAreaCellRenderer()
    }
  }
  val scroll = JScrollPane(table)
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TextAreaCellRenderer : TableCellRenderer {
  private val renderer = JTextArea()

  // Caches the preferred height of each cell: cellHeights[row][column]
  private val cellHeights = mutableListOf<MutableList<Int>>()

  init {
    renderer.lineWrap = true
    renderer.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    renderer.name = "Table.cellRenderer"
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    renderer.font = table.font
    renderer.text = value?.toString() ?: ""
    // The trick for this to work properly is to set the width of the column to the
    // text area. The reason for this is that getPreferredSize(), without a width tries
    // to place all the text in one line. By setting the size with the width of the column,
    // getPreferredSize() returns the proper height which the row should have in
    // order to make room for the text.
    // http://blog.botunge.dk/post/2009/10/09/JTable-multiline-cell-renderer.aspx
    renderer.bounds = table.getCellRect(row, column, false)
    val maxHeight = updateCellHeight(row, column, renderer.preferredSize.height)
    if (table.getRowHeight(row) != maxHeight) {
      table.setRowHeight(row, maxHeight)
    }
    return renderer
  }

  // Stores the preferred height of the given cell and returns the maximum
  // cell height of the row, i.e. the height that the row should have.
  private fun updateCellHeight(
    row: Int,
    column: Int,
    height: Int,
  ): Int {
    while (cellHeights.size <= row) {
      cellHeights.add(mutableListOf())
    }
    val colHeights = cellHeights[row]
    while (colHeights.size <= column) {
      colHeights.add(0)
    }
    colHeights[column] = height
    return colHeights.max()
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
