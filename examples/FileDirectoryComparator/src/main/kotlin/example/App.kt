package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileSystemView
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

private val defaultRadio = JRadioButton("Default", true)
private val dirFirstRadio = JRadioButton("Directory < File", false)
private val groupSortingRadio = JRadioButton("Group Sorting", false)
private val table = createTable()

fun createUI(): Component {
  val sorter = TableRowSorter(table.model)
  table.rowSorter = sorter
  setFileComparators(sorter)
  val listener = ItemListener { e ->
    if (e.getStateChange() == ItemEvent.SELECTED) {
      setFileComparators(sorter)
    }
  }
  val p = JPanel()
  val group = ButtonGroup()
  listOf(defaultRadio, dirFirstRadio, groupSortingRadio).forEach {
    it.addItemListener(listener)
    group.add(it)
    p.add(it)
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

// Set Comparator in TableRowSorter at once depending on the selected radio button
private fun setFileComparators(sorter: TableRowSorter<out TableModel>) {
  for (i in 0..<sorter.model.columnCount) {
    sorter.setComparator(i, getFileComparator(i))
  }
}

// Get the underlying Comparator for each column
private fun getFileComparator(index: Int): Comparator<File> {
  val baseComp = getBaseFileComparator(index)
  return when {
    dirFirstRadio.isSelected -> {
      // Directory < File: directories are compared as smaller than files.
      // Since the RowSorter reverses the comparator result in descending order,
      // directories are placed at the top in ascending order
      // and at the bottom in descending order.
      Comparator
        .comparing<File, Boolean>({ it.isDirectory() }, Comparator.reverseOrder())
        .thenComparing(baseComp)
    }

    groupSortingRadio.isSelected -> {
      // Group Sorting: directories are always placed at the top
      Comparator { a, b ->
        val v = java.lang.Boolean.compare(b.isDirectory(), a.isDirectory())
        // Multiplying the directory-first result by the current sort direction
        // cancels out the reversal by the RowSorter in descending order,
        // so only the files and directories within each group are reversed.
        if (v == 0) baseComp.compare(a, b) else v * getSortOrderDirection(index)
      }
    }

    // Default
    else -> {
      baseComp
    }
  }
}

// Returns a basic File Comparator according to column index
private fun getBaseFileComparator(column: Int): Comparator<File> = when (column) {
  0 -> Comparator.comparing(
    { it.getName() },
    String.CASE_INSENSITIVE_ORDER,
  )

  1 -> Comparator.comparingLong { it.length() }

  else -> Comparator.comparing(
    { it.absolutePath },
    String.CASE_INSENSITIVE_ORDER,
  )
}

// Get the current sort direction of the specified column
// (ascending or unsorted: 1, descending: -1)
// Search the sort key of the specified column, not just the primary key,
// because the comparator is also used for the secondary sort keys.
private fun getSortOrderDirection(column: Int) = table.rowSorter
  ?.sortKeys
  ?.firstOrNull { it.column == column }
  ?.takeIf { it.sortOrder == SortOrder.DESCENDING }
  ?.let { -1 }
  ?: 1

private fun createTable(): JTable {
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
    FileIconCellRenderer(FileSystemView.getFileSystemView()),
  )
  return table
}

private class FileIconCellRenderer(
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
          c.horizontalAlignment = LEFT
          c.icon = fileSystemView.getSystemIcon(value)
          c.text = fileSystemView.getSystemDisplayName(value)
        }

        1 -> {
          c.horizontalAlignment = RIGHT
          c.icon = null
          c.text = if (value.isDirectory) "" else value.length().toString()
        }

        2 -> {
          c.horizontalAlignment = LEFT
          c.icon = null
          c.text = value.absolutePath
        }

        else -> {
          c.horizontalAlignment = LEFT
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
    val table = support.component as? JTable
    val model = table?.model as? DefaultTableModel ?: return false
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

private class TablePopupMenu : JPopupMenu() {
  private val delete = add("delete")

  init {
    delete.addActionListener { deleteSelectedRows() }
  }

  private fun deleteSelectedRows() {
    val table = getInvoker() as? JTable
    val model = table?.model
    if (model is DefaultTableModel) {
      val selection = table.selectedRows
      for (i in selection.indices.reversed()) {
        model.removeRow(table.convertRowIndexToModel(selection[i]))
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
