package example

import java.awt.*
import javax.swing.*
import javax.swing.border.LineBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

const val BORDER_WIDTH1 = 1
const val BORDER_WIDTH2 = 2
const val CELL_SIZE = 18

fun createUI(): Component {
  val columnNames = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
  val data = arrayOf<Array<Number>>(
    arrayOf(5, 3, 0, 0, 7, 0, 0, 0, 0),
    arrayOf(6, 0, 0, 1, 9, 5, 0, 0, 0),
    arrayOf(0, 9, 8, 0, 0, 0, 0, 6, 0),
    arrayOf(8, 0, 0, 0, 6, 0, 0, 0, 3),
    arrayOf(4, 0, 0, 8, 0, 3, 0, 0, 1),
    arrayOf(7, 0, 0, 0, 2, 0, 0, 0, 6),
    arrayOf(0, 6, 0, 0, 0, 0, 2, 8, 0),
    arrayOf(0, 0, 0, 4, 1, 9, 0, 0, 5),
    arrayOf(0, 0, 0, 0, 8, 0, 0, 7, 9),
  )

  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = Number::class.java

    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = data[row][column] == 0
  }
  val table = object : JTable(model) {
    override fun getPreferredScrollableViewportSize() = super.getPreferredSize()
  }
  for (i in 0..<table.rowCount) {
    table.setRowHeight(i, getCellSize(i))
  }

  table.cellSelectionEnabled = true
  table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF
  table.tableHeader.reorderingAllowed = false
  table.border = BorderFactory.createEmptyBorder()
  table.setShowGrid(false)
  // also sets the row margin and the column margin to 0
  table.intercellSpacing = Dimension()

  val editor = JTextField()
  editor.horizontalAlignment = SwingConstants.CENTER
  table.setDefaultEditor(Number::class.java, SudokuCellEditor(editor))
  table.setDefaultRenderer(Number::class.java, SudokuCellRenderer(data))

  val columnModel = table.columnModel
  columnModel.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
  for (i in 0..<columnModel.columnCount) {
    val col = columnModel.getColumn(i)
    col.preferredWidth = getCellSize(i)
    col.resizable = false
  }

  val scroll = JScrollPane(table).also {
    it.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    it.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    it.border = BorderFactory.createEmptyBorder()
    it.viewportBorder = BorderFactory.createMatteBorder(
      BORDER_WIDTH2,
      BORDER_WIDTH2,
      0,
      0,
      Color.BLACK,
    )
    it.columnHeader = JViewport()
    it.columnHeader.isVisible = false
  }

  return JPanel(GridBagLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

// The cell size including the right or bottom border width,
// every third row or column has a thick block border.
private fun getCellSize(index: Int): Int {
  val isBlockEdge = (index + 1) % 3 == 0
  return CELL_SIZE + if (isBlockEdge) BORDER_WIDTH2 else BORDER_WIDTH1
}

private class SudokuCellEditor(
  private val textField: JTextField,
) : DefaultCellEditor(textField) {
  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    textField.border = LineBorder(Color.BLACK)
    val v = if (value == 0) "" else value
    return super.getTableCellEditorComponent(table, v, isSelected, row, column)
  }

  // Accept only an empty string or a single digit from 1 to 9,
  // otherwise keep editing and show a red border like JTable.GenericEditor.
  override fun stopCellEditing(): Boolean {
    val text = textField.text.trim()
    val isValid = text.isEmpty() || text.matches("[1-9]".toRegex())
    val stopped = isValid && super.stopCellEditing()
    if (!isValid) {
      textField.border = LineBorder(Color.RED)
    }
    return stopped
  }

  // Store an Int instead of the String returned by DefaultCellEditor,
  // an empty cell is represented by 0.
  override fun getCellEditorValue(): Any {
    val text = textField.text.trim()
    return if (text.isEmpty()) 0 else text.toInt()
  }
}

private class SudokuCellRenderer(
  src: Array<Array<Number>>,
) : DefaultTableCellRenderer() {
  private val cellBorder = BorderFactory.createMatteBorder(
    0,
    0,
    BORDER_WIDTH1,
    BORDER_WIDTH1,
    Color.GRAY,
  )
  private val blockCornerBorder = BorderFactory.createMatteBorder(
    0,
    0,
    BORDER_WIDTH2,
    BORDER_WIDTH2,
    Color.BLACK,
  )
  private val blockBottomBorder = BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(0, 0, BORDER_WIDTH2, 0, Color.BLACK),
    BorderFactory.createMatteBorder(0, 0, 0, BORDER_WIDTH1, Color.GRAY),
  )
  private val blockRightBorder = BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(0, 0, 0, BORDER_WIDTH2, Color.BLACK),
    BorderFactory.createMatteBorder(0, 0, BORDER_WIDTH1, 0, Color.GRAY),
  )
  private val puzzle = Array(src.size) { i -> Array(src[i].size) { j -> src[i][j] } }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val isEditable = puzzle[row][column] == 0
    val b = isEditable && isSelected
    val c = super.getTableCellRendererComponent(
      table,
      value,
      b,
      hasFocus,
      row,
      column,
    )
    if (!isEditable) {
      c.font = c.font.deriveFont(Font.BOLD)
    }
    if (c is JLabel) {
      if (isEditable && value == 0) {
        c.text = " "
      }
      c.horizontalAlignment = CENTER
      val isBlockBottom = (row + 1) % 3 == 0
      val isBlockRight = (column + 1) % 3 == 0
      c.border = when {
        isBlockBottom && isBlockRight -> blockCornerBorder
        isBlockBottom -> blockBottomBorder
        isBlockRight -> blockRightBorder
        else -> cellBorder
      }
    }
    return c
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
