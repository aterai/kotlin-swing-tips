package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.event.ItemEvent
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileSystemView
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val table = makeTable()
  val sorter = TableRowSorter(table.model)
  table.rowSorter = sorter
  setDefaultComparator(sorter)

  val check1 = JRadioButton("Default", true)
  check1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      setDefaultComparator(sorter)
    }
  }
  val check2 = JRadioButton("Directory < File", false)
  check2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      for (i in 0..<3) {
        sorter.setComparator(i, FileComparator(i))
      }
    }
  }
  val check3 = JRadioButton("Group Sorting", false)
  check3.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      for (i in 0..<3) {
        sorter.setComparator(i, FileGroupComparator(table, i))
      }
    }
  }

  val p = JPanel()
  val bg = ButtonGroup()
  listOf(check1, check2, check3).forEach {
    bg.add(it)
    p.add(it)
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun setDefaultComparator(sorter: TableRowSorter<TableModel>) {
  for (i in 0..<3) {
    sorter.setComparator(i, DefaultFileComparator(i))
  }
}

private fun makeTable(): JTable {
  val columnNames = arrayOf("Name", "Size", "Full Path")
  val model = object : DefaultTableModel(columnNames, 0) {
    override fun getColumnClass(column: Int) = File::class.java
  }
  val table = JTable(model)
  table.putClientProperty("Table.isFileList", true)
  table.cellSelectionEnabled = true
  table.intercellSpacing = Dimension()
  table.componentPopupMenu = TablePopupMenu()
  table.setShowGrid(false)
  table.fillsViewportHeight = true
  // table.setAutoCreateRowSorter(true)
  table.dropMode = DropMode.INSERT_ROWS
  table.transferHandler = FileTransferHandler()
  table.setDefaultRenderer(
    Any::class.java,
    FileIconTableCellRenderer(FileSystemView.getFileSystemView()),
  )
  return table
}

private class FileIconTableCellRenderer(
  private val fileSystemView: FileSystemView,
) : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    if (c is JLabel && value is File) {
      when (table.convertColumnIndexToModel(column)) {
        0 -> {
          c.horizontalAlignment = SwingConstants.LEFT
          c.icon = fileSystemView.getSystemIcon(value)
          c.text = fileSystemView.getSystemDisplayName(value)
        }

        1 -> {
          c.horizontalAlignment = SwingConstants.RIGHT
          c.icon = null
          c.text = if (value.isDirectory) "" else value.length().toString()
        }

        2 -> {
          c.horizontalAlignment = SwingConstants.LEFT
          c.icon = null
          c.text = value.absolutePath
        }

        else -> {
          c.horizontalAlignment = SwingConstants.LEFT
          c.icon = null
          c.text = value.absolutePath
          error("Should never happened.")
        }
      }
    }
    return c
  }
}

private class FileTransferHandler : TransferHandler() {
  override fun importData(support: TransferSupport): Boolean {
    val list = runCatching {
      support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
    }.getOrNull().orEmpty()
    val model = (support.component as? JTable)?.model as? DefaultTableModel ?: return false
    list
      .filterIsInstance<File>()
      .map { file -> (0..2).map { file }.toTypedArray() }
      .forEach { model.addRow(it) }
    return list.isNotEmpty()
  }

  override fun canImport(ts: TransferSupport) =
    ts.isDataFlavorSupported(DataFlavor.javaFileListFlavor)

  override fun getSourceActions(component: JComponent) = COPY
}

private open class DefaultFileComparator(
  protected val column: Int,
) : Comparator<File> {
  override fun compare(
    a: File,
    b: File,
  ) = when (column) {
    0 -> a.name.compareTo(b.name, ignoreCase = true)
    1 -> a.length().compareTo(b.length())
    else -> a.absolutePath.compareTo(b.absolutePath, ignoreCase = true)
  }
}

private class FileComparator(
  column: Int,
) : DefaultFileComparator(column) {
  override fun compare(
    a: File,
    b: File,
  ) = when {
    a.isDirectory && !b.isDirectory -> -1
    !a.isDirectory && b.isDirectory -> 1
    else -> super.compare(a, b)
  }
}

// > dir /O:GN
// > ls --group-directories-first
private class FileGroupComparator(
  private val table: JTable,
  column: Int,
) : DefaultFileComparator(column) {
  override fun compare(
    a: File,
    b: File,
  ): Int {
    val key = table.rowSorter.sortKeys.firstOrNull()
    val flag = key
      ?.takeIf {
        it.column == column && it.sortOrder == SortOrder.DESCENDING
      }?.let { -1 } ?: 1
    return when {
      a.isDirectory && !b.isDirectory -> -1 * flag
      !a.isDirectory && b.isDirectory -> 1 * flag
      else -> super.compare(a, b)
    }
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete = add("delete")

  init {
    delete.addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
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
    if (c is JTable) {
      delete.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
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
