package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

const val BW1 = 1
const val BW2 = 2
const val CELL_SIZE = 18

fun makeUI(): Component {
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

    override fun isCellEditable(row: Int, column: Int) = data[row][column] == 0
  }
  val table = object : JTable(model) {
    override fun getPreferredScrollableViewportSize() = super.getPreferredSize()
  }
  for (i in 0 until table.rowCount) {
    val a = if ((i + 1) % 3 == 0) BW2 else BW1
    table.setRowHeight(i, CELL_SIZE + a)
  }

  table.cellSelectionEnabled = true
  table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF
  table.tableHeader.reorderingAllowed = false
  table.border = BorderFactory.createEmptyBorder()

  table.showVerticalLines = false
  table.showHorizontalLines = false

  table.intercellSpacing = Dimension()
  table.rowMargin = 0
  table.columnModel.columnMargin = 0

  val editor = JTextField()
  editor.horizontalAlignment = SwingConstants.CENTER
  // editor.setBorder(BorderFactory.createLineBorder(Color.RED))
  val cellEditor = object : DefaultCellEditor(editor) {
    override fun getTableCellEditorComponent(
      table: JTable,
      value: Any?,
      isSelected: Boolean,
      row: Int,
      column: Int
    ): Component {
      val v = if (value == 0) "" else value
      return super.getTableCellEditorComponent(table, v, isSelected, row, column)
    }

    override fun getCellEditorValue() = if (editor.text.isEmpty()) {
      0
    } else {
      super.getCellEditorValue()
    }
  }
  table.setDefaultEditor(Number::class.java, cellEditor)
  table.setDefaultRenderer(Number::class.java, SudokuCellRenderer(data))

  val m = table.columnModel
  m.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
  for (i in 0 until m.columnCount) {
    val col = m.getColumn(i)
    val a = if ((i + 1) % 3 == 0) BW2 else BW1
    col.preferredWidth = CELL_SIZE + a
    col.resizable = false
  }

  val scroll = JScrollPane(table).also {
    it.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    it.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    it.border = BorderFactory.createEmptyBorder()
    it.viewportBorder = BorderFactory.createMatteBorder(BW2, BW2, 0, 0, Color.BLACK)
    it.columnHeader = JViewport()
    it.columnHeader.isVisible = false
  }

  return JPanel(GridBagLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class SudokuCellRenderer(src: Array<Array<Number>>) : DefaultTableCellRenderer() {
  private val b0 = BorderFactory.createMatteBorder(0, 0, BW1, BW1, Color.GRAY)
  private val b1 = BorderFactory.createMatteBorder(0, 0, BW2, BW2, Color.BLACK)
  private val b2 = BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(0, 0, BW2, 0, Color.BLACK),
    BorderFactory.createMatteBorder(0, 0, 0, BW1, Color.GRAY),
  )
  private val b3 = BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(0, 0, 0, BW2, Color.BLACK),
    BorderFactory.createMatteBorder(0, 0, BW1, 0, Color.GRAY),
  )
  private val mask = Array(src.size) { i -> Array(src[i].size) { j -> src[i][j] } }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val isEditable = mask[row][column] == 0
    val b = isEditable && isSelected
    val c = super.getTableCellRendererComponent(table, value, b, hasFocus, row, column)
    if (!isEditable) {
      c.font = c.font.deriveFont(Font.BOLD)
    }
    if (c is JLabel) {
      if (isEditable && value == 0) {
        c.text = " "
      }
      c.horizontalAlignment = SwingConstants.CENTER
      val rf = (row + 1) % 3 == 0
      val cf = (column + 1) % 3 == 0
      c.border = when {
        rf && cf -> b1
        rf -> b2
        cf -> b3
        else -> b0
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
