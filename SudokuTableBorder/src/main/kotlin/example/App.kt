package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

const val BORDERWIDTH1 = 1
const val BORDERWIDTH2 = 2
const val CELLSIZE = 18

fun makeUI(): Component {
  val columnNames = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
  val data = arrayOf(
      arrayOf<Number>(5, 3, 0, 0, 7, 0, 0, 0, 0),
      arrayOf<Number>(6, 0, 0, 1, 9, 5, 0, 0, 0),
      arrayOf<Number>(0, 9, 8, 0, 0, 0, 0, 6, 0),
      arrayOf<Number>(8, 0, 0, 0, 6, 0, 0, 0, 3),
      arrayOf<Number>(4, 0, 0, 8, 0, 3, 0, 0, 1),
      arrayOf<Number>(7, 0, 0, 0, 2, 0, 0, 0, 6),
      arrayOf<Number>(0, 6, 0, 0, 0, 0, 2, 8, 0),
      arrayOf<Number>(0, 0, 0, 4, 1, 9, 0, 0, 5),
      arrayOf<Number>(0, 0, 0, 0, 8, 0, 0, 7, 9))

  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = Number::class.java

    override fun isCellEditable(row: Int, column: Int) = data[row][column] == 0
  }
  val table = object : JTable(model) {
    override fun getPreferredScrollableViewportSize() = super.getPreferredSize()
  }
  for (i in 0 until table.getRowCount()) {
    val a = if ((i + 1) % 3 == 0) BORDERWIDTH2 else BORDERWIDTH1
    table.setRowHeight(i, CELLSIZE + a)
  }

  table.setCellSelectionEnabled(true)
  table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
  table.getTableHeader().setReorderingAllowed(false)
  table.setBorder(BorderFactory.createEmptyBorder())

  table.setShowVerticalLines(false)
  table.setShowHorizontalLines(false)

  table.setIntercellSpacing(Dimension())
  table.setRowMargin(0)
  table.getColumnModel().setColumnMargin(0)

  val editor = JTextField()
  editor.setHorizontalAlignment(SwingConstants.CENTER)
  // editor.setBorder(BorderFactory.createLineBorder(Color.RED));
  table.setDefaultEditor(Number::class.java, object : DefaultCellEditor(editor) {
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

    override fun getCellEditorValue(): Any {
      return if (editor.getText().isEmpty()) 0 else super.getCellEditorValue()
    }
  })
  table.setDefaultRenderer(Number::class.java, SudokuCellRenderer(data))

  val m = table.getColumnModel()
  m.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  for (i in 0 until m.getColumnCount()) {
    val col = m.getColumn(i)
    val a = if ((i + 1) % 3 == 0) BORDERWIDTH2 else BORDERWIDTH1
    col.setPreferredWidth(CELLSIZE + a)
    col.setResizable(false)
  }

  val scroll = JScrollPane(table).also {
    it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
    it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    it.setBorder(BorderFactory.createEmptyBorder())
    it.setViewportBorder(BorderFactory.createMatteBorder(BORDERWIDTH2, BORDERWIDTH2, 0, 0, Color.BLACK))
    it.setColumnHeader(JViewport())
    it.getColumnHeader().setVisible(false)
  }

  return JPanel(GridBagLayout()).also {
    it.add(scroll)
    it.setPreferredSize(Dimension(320, 240))
  }
}

private class SudokuCellRenderer(src: Array<Array<Number>>) : DefaultTableCellRenderer() {
  private val bold: Font
  private val b0 = BorderFactory.createMatteBorder(0, 0, BORDERWIDTH1, BORDERWIDTH1, Color.GRAY)
  private val b1 = BorderFactory.createMatteBorder(0, 0, BORDERWIDTH2, BORDERWIDTH2, Color.BLACK)
  private val b2 = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, BORDERWIDTH2, 0, Color.BLACK),
      BorderFactory.createMatteBorder(0, 0, 0, BORDERWIDTH1, Color.GRAY))
  private val b3 = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, 0, BORDERWIDTH2, Color.BLACK),
      BorderFactory.createMatteBorder(0, 0, BORDERWIDTH1, 0, Color.GRAY))
  private val mask: Array<Array<Number>>

  init {
    this.bold = font.deriveFont(Font.BOLD)
    // // val dest = Array<Array<Int?>>(src.size) { arrayOfNulls(src[0].size) }
    // val dest = Array(src.size, { Array(src[0].size, { 0 }) })
    // for (i in src.indices) {
    //   System.arraycopy(src[i], 0, dest[i], 0, src[0].size)
    // }
    // this.mask = dest
    // this.mask = Array(src.size, { i -> Array(src[i].size, { j -> src[i][j] }) })
    this.mask = Array(src.size) { i -> Array(src[i].size) { j -> src[i][j] } }
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val isEditable = mask[row][column] == 0
    super.getTableCellRendererComponent(table, value, isEditable && isSelected, hasFocus, row, column)
    if (isEditable && value == 0) {
      this.setText(" ")
    }
    setFont(if (isEditable) font else bold)
    setHorizontalAlignment(SwingConstants.CENTER)
    val rf = (row + 1) % 3 == 0
    val cf = (column + 1) % 3 == 0
    setBorder(when {
      rf && cf -> b1
      rf -> b2
      cf -> b3
      else -> b0
    })
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
