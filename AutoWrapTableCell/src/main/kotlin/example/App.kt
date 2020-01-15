package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  init {
    val columnNames = arrayOf("Default", "AutoWrap")
    val data = arrayOf(
      arrayOf<Any>("123456789012345678901234567890", "123456789012345678901234567890"),
      arrayOf<Any>("1111", "22222222222222222222222222222222222222222222222222222222"),
      arrayOf<Any>("3333333", "----------------------------------------------0"),
      arrayOf<Any>("4444444444444444444", ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>|")
    )
    val model = DefaultTableModel(data, columnNames)
    val table = object : JTable(model) {
      private val evenColor = Color(0xE6_F0_FF)
      override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component {
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
        getColumnModel().getColumn(AUTO_WRAP_COLUMN).cellRenderer = null
        super.updateUI()
        isEnabled = false
        setShowGrid(false)
        getColumnModel().getColumn(AUTO_WRAP_COLUMN).cellRenderer = TextAreaCellRenderer()
      }
    }
    val scroll = JScrollPane(table)
    scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
    scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    add(scroll)
    preferredSize = Dimension(320, 240)
  }

  companion object {
    private const val AUTO_WRAP_COLUMN = 1
  }
}

class TextAreaCellRenderer : TableCellRenderer {
  private val renderer = JTextArea()
  private val rowAndCellHeights: MutableList<MutableList<Int>> = ArrayList()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    renderer.font = table.font
    renderer.text = value?.toString() ?: ""
    adjustRowHeight(table, row, column)
    return renderer
  }

  // Calculate the new preferred height for a given row, and sets the height on the table.
  // http://blog.botunge.dk/post/2009/10/09/JTable-multiline-cell-renderer.aspx
  private fun adjustRowHeight(
    table: JTable,
    row: Int,
    column: Int
  ) {
    // The trick for this to work properly is to set the width of the column to the
    // text area. The reason for this is that getPreferredSize(), without a width tries
    // to place all the text in one line. By setting the size with the width of the column,
    // getPreferredSize() returns the proper height which the row should have in
    // order to make room for the text.
    // int cWidth = table.getTableHeader().getColumnModel().getColumn(column).getWidth()
    // int cWidth = table.getCellRect(row, column, false).width // Ignore IntercellSpacing
    // renderer.setSize(new Dimension(cWidth, 1000))
    renderer.bounds = table.getCellRect(row, column, false)
    // renderer.doLayout()
    val preferredHeight = renderer.preferredSize.height
    while (rowAndCellHeights.size <= row) {
      rowAndCellHeights.add(createMutableList(column))
    }
    val list = rowAndCellHeights[row]
    while (list.size <= column) {
      list.add(0)
    }
    list[column] = preferredHeight
    val max = list.max() ?: 0
    if (table.getRowHeight(row) != max) {
      table.setRowHeight(row, max)
    }
  }

  private fun <E> createMutableList(initialCapacity: Int) = ArrayList<E>(initialCapacity)

  init {
    renderer.lineWrap = true
    renderer.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    renderer.name = "Table.cellRenderer"
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
