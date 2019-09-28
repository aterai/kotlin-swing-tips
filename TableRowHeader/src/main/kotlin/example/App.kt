package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

class MainPanel : JPanel(BorderLayout()) {
  init {

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
    table.setCellSelectionEnabled(true)

    val header = table.getTableHeader()
    header.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        if (table.isEditing()) {
          table.getCellEditor().stopCellEditing()
        }
        val col = header.columnAtPoint(e.getPoint())
        table.changeSelection(0, col, false, false)
        table.changeSelection(table.getRowCount() - 1, col, false, true)
      }
    })

    val rowHeader = RowHeaderList<String>(listModel, table)
    rowHeader.setFixedCellWidth(50)

    val scroll = JScrollPane(table)
    scroll.setRowHeaderView(rowHeader)
    scroll.getRowHeader().addChangeListener { e ->
      val vport = e.getSource() as? JViewport ?: return@addChangeListener
      scroll.getVerticalScrollBar().setValue(vport.getViewPosition().y)
    }
    scroll.setComponentPopupMenu(TablePopupMenu())
    table.setInheritsPopupMenu(true)

    rowHeader.setBackground(Color.BLUE)
    scroll.setBackground(Color.RED)
    scroll.getViewport().setBackground(Color.GREEN)

    add(scroll)
    setPreferredSize(Dimension(320, 240))
  }
}

class RowHeaderList<E>(model: ListModel<E>, private val table: JTable) : JList<E>(model) {
  private val tableSelection: ListSelectionModel
  private val listSelection: ListSelectionModel
  private var rollOverRowIndex = -1
  private var pressedRowIndex = -1

  init {
    setFixedCellHeight(table.getRowHeight())
    setCellRenderer(RowHeaderRenderer<E>(table.getTableHeader()))
    // setSelectionModel(table.getSelectionModel())
    val rol = RollOverListener()
    addMouseListener(rol)
    addMouseMotionListener(rol)
    // setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY.brighter()))

    tableSelection = table.getSelectionModel()
    listSelection = getSelectionModel()
  }

  inner class RowHeaderRenderer<E2>(private val header: JTableHeader) : JLabel(), ListCellRenderer<E2> {
    init {
      this.setOpaque(true)
      // this.setBorder(UIManager.getBorder("TableHeader.cellBorder"))
      this.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 2, Color.GRAY.brighter()))
      this.setHorizontalAlignment(SwingConstants.CENTER)
      this.setForeground(header.getForeground())
      this.setBackground(header.getBackground())
      this.setFont(header.getFont())
    }

    override fun getListCellRendererComponent(
      list: JList<out E2>,
      value: E2?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      when {
        index == pressedRowIndex -> setBackground(Color.GRAY)
        index == rollOverRowIndex -> setBackground(Color.WHITE)
        isSelected -> setBackground(Color.GRAY.brighter())
        else -> {
          setForeground(header.getForeground())
          setBackground(header.getBackground())
        }
      }
      setText(value?.toString() ?: "")
      return this
    }
  }

  private inner class RollOverListener : MouseAdapter() {
    override fun mouseExited(e: MouseEvent) {
      if (pressedRowIndex < 0) {
        rollOverRowIndex = -1
        repaint()
      }
    }

    override fun mouseMoved(e: MouseEvent) {
      val row = locationToIndex(e.getPoint())
      if (row != rollOverRowIndex) {
        rollOverRowIndex = row
        repaint()
      }
    }

    override fun mouseDragged(e: MouseEvent) {
      if (pressedRowIndex >= 0) {
        val row = locationToIndex(e.getPoint())
        val start = minOf(row, pressedRowIndex)
        val end = maxOf(row, pressedRowIndex)
        tableSelection.clearSelection()
        listSelection.clearSelection()
        tableSelection.addSelectionInterval(start, end)
        listSelection.addSelectionInterval(start, end)
        repaint()
      }
    }

    override fun mousePressed(e: MouseEvent) {
      val row = locationToIndex(e.getPoint())
      if (row == pressedRowIndex) {
        return
      }
      listSelection.clearSelection()
      table.changeSelection(row, 0, false, false)
      table.changeSelection(row, table.getColumnModel().getColumnCount() - 1, false, true)
      pressedRowIndex = row
    }

    override fun mouseReleased(e: MouseEvent) {
      listSelection.clearSelection()
      pressedRowIndex = -1
      rollOverRowIndex = -1
      repaint()
    }
  }
}

class RowDataModel(private val rowListModel: DefaultListModel<String>) : DefaultTableModel() {
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

  override fun isCellEditable(row: Int, col: Int) = COLUMN_ARRAY[col].isEditable

  override fun getColumnClass(column: Int) = COLUMN_ARRAY[column].columnClass

  override fun getColumnCount() = COLUMN_ARRAY.size

  override fun getColumnName(column: Int) = COLUMN_ARRAY[column].columnName

  private data class ColumnContext(val columnName: String, val columnClass: Class<*>, val isEditable: Boolean)

  companion object {
    private val COLUMN_ARRAY = arrayOf(
      ColumnContext("Name", String::class.java, false),
      ColumnContext("Comment", String::class.java, false))
  }
}

data class RowData(val name: String, val comment: String)

class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = getInvoker() as? JTable ?: return@addActionListener
      val model = table.getModel() as? RowDataModel ?: return@addActionListener
      model.addRowData(RowData("New row", ""))
      table.scrollRectToVisible(table.getCellRect(model.getRowCount() - 1, 0, true))
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = getInvoker() as? JTable ?: return@addActionListener
      val model = table.getModel() as? DefaultTableModel ?: return@addActionListener
      val selection = table.getSelectedRows()
      for (i in selection.indices.reversed()) {
        model.removeRow(table.convertRowIndexToModel(selection[i]))
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTable)?.also {
      delete.setEnabled(it.getSelectedRowCount() > 0)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
