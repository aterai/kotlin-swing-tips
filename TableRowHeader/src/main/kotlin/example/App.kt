package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

fun makeUI(): Component {
  val listModel = DefaultListModel<String>()
  val model = RowDataModel(listModel)
  model.addRowData(RowData("Name 1", "comment"))
  model.addRowData(RowData("Name 2", "test"))
  model.addRowData(RowData("Name d", "ee"))
  model.addRowData(RowData("Name c", "test cc"))
  model.addRowData(RowData("Name b", "test bb"))
  model.addRowData(RowData("Name a", "ff"))
  model.addRowData(RowData("Name 0", "test aa"))
  model.addRowData(RowData("Name 0", "gg"))

  val table = JTable(model)
  table.cellSelectionEnabled = true
  table.rowHeight = 20

  val header = table.tableHeader
  val ml = object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (table.isEditing) {
        table.cellEditor.stopCellEditing()
      }
      val col = header.columnAtPoint(e.point)
      table.changeSelection(0, col, false, false)
      table.changeSelection(table.rowCount - 1, col, false, true)
    }
  }
  header.addMouseListener(ml)

  val rowHeader = RowHeaderList<String>(listModel, table)
  rowHeader.fixedCellWidth = 50

  val scroll = JScrollPane(table)
  scroll.setRowHeaderView(rowHeader)
  scroll.rowHeader.addChangeListener { e ->
    (e.source as? JViewport)?.also {
      scroll.verticalScrollBar.value = it.viewPosition.y
    }
  }
  scroll.componentPopupMenu = TablePopupMenu()
  table.inheritsPopupMenu = true

  rowHeader.background = Color.BLUE
  scroll.background = Color.RED
  scroll.viewport.background = Color.GREEN

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class RowHeaderList<E>(
  model: ListModel<E>,
  private val table: JTable,
) : JList<E>(model) {
  private val tableSelection: ListSelectionModel
  private val listSelection: ListSelectionModel
  private var rollOverRowIndex = -1
  private var pressedRowIndex = -1

  init {
    fixedCellHeight = table.rowHeight
    cellRenderer = RowHeaderRenderer<E>(table.tableHeader)
    // setSelectionModel(table.getSelectionModel())
    val rol = RollOverListener()
    addMouseListener(rol)
    addMouseMotionListener(rol)
    // setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY.brighter()))

    tableSelection = table.selectionModel
    listSelection = selectionModel
  }

  inner class RowHeaderRenderer<F>(
    private val header: JTableHeader,
  ) : ListCellRenderer<F> {
    private val renderer = JLabel()

    init {
      renderer.isOpaque = true
      // renderer.setBorder(UIManager.getBorder("TableHeader.cellBorder"))
      renderer.border = BorderFactory.createMatteBorder(0, 0, 1, 2, Color.GRAY.brighter())
      renderer.horizontalAlignment = SwingConstants.CENTER
      // renderer.foreground = header.foreground
      // renderer.background = header.background
      // renderer.font = header.font
    }

    override fun getListCellRendererComponent(
      list: JList<out F>,
      value: F?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean,
    ): Component {
      renderer.font = header.font
      renderer.text = value?.toString() ?: ""
      when {
        index == pressedRowIndex -> {
          renderer.foreground = Color.WHITE
          renderer.background = Color.GRAY
        }
        index == rollOverRowIndex -> {
          renderer.foreground = Color.BLACK
          renderer.background = Color.WHITE
        }
        isSelected -> {
          renderer.foreground = header.foreground.brighter()
          renderer.background = Color.GRAY.brighter()
        }
        else -> {
          renderer.foreground = header.foreground
          renderer.background = header.background
        }
      }
      return renderer
    }
  }

  private inner class RollOverListener : MouseAdapter() {
    override fun mouseExited(e: MouseEvent) {
      if (pressedRowIndex < 0) {
        rollOverRowIndex = -1
        e.component.repaint()
      }
    }

    override fun mouseMoved(e: MouseEvent) {
      val row = locationToIndex(e.point)
      if (row != rollOverRowIndex) {
        rollOverRowIndex = row
        e.component.repaint()
      }
    }

    override fun mouseDragged(e: MouseEvent) {
      if (pressedRowIndex >= 0) {
        val row = locationToIndex(e.point)
        val start = minOf(row, pressedRowIndex)
        val end = maxOf(row, pressedRowIndex)
        tableSelection.clearSelection()
        listSelection.clearSelection()
        tableSelection.addSelectionInterval(start, end)
        listSelection.addSelectionInterval(start, end)
        e.component.repaint()
      }
    }

    override fun mousePressed(e: MouseEvent) {
      val row = locationToIndex(e.point)
      if (row == pressedRowIndex) {
        return
      }
      listSelection.clearSelection()
      table.changeSelection(row, 0, false, false)
      table.changeSelection(row, table.columnModel.columnCount - 1, false, true)
      pressedRowIndex = row
    }

    override fun mouseReleased(e: MouseEvent) {
      listSelection.clearSelection()
      pressedRowIndex = -1
      rollOverRowIndex = -1
      e.component.repaint()
    }
  }
}

private class RowDataModel(
  private val rowListModel: DefaultListModel<String>,
) : DefaultTableModel() {
  private var number = 0

  fun addRowData(t: RowData) {
    val obj = arrayOf(t.name, t.comment)
    super.addRow(obj)
    rowListModel.addElement("row$number")
    number++
  }

  override fun removeRow(index: Int) {
    super.removeRow(index)
    rowListModel.remove(index)
  }

  override fun isCellEditable(
    row: Int,
    col: Int,
  ) = COLUMN_ARRAY[col].isEditable

  override fun getColumnClass(column: Int) = COLUMN_ARRAY[column].columnClass

  override fun getColumnCount() = COLUMN_ARRAY.size

  override fun getColumnName(column: Int) = COLUMN_ARRAY[column].columnName

  private data class ColumnContext(
    val columnName: String,
    val columnClass: Class<*>,
    val isEditable: Boolean,
  )

  companion object {
    private val COLUMN_ARRAY = arrayOf(
      ColumnContext("Name", String::class.java, false),
      ColumnContext("Comment", String::class.java, false),
    )
  }
}

private data class RowData(val name: String, val comment: String)

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = invoker as? JTable
      val model = table?.model as? RowDataModel
      if (model != null) {
        model.addRowData(RowData("New row", ""))
        table.scrollRectToVisible(table.getCellRect(model.rowCount - 1, 0, true))
      }
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as? JTable
      val model = table?.model as? DefaultTableModel
      if (model != null) {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    (c as? JTable)?.also {
      delete.isEnabled = it.selectedRowCount > 0
      super.show(it, x, y)
    }
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
