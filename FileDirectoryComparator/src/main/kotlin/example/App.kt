package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ItemEvent
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.util.Comparator
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.filechooser.FileSystemView
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

class MainPanel : JPanel(BorderLayout()) {
  init {
    val columnNames = arrayOf("Name", "Size", "Full Path")
    val model = object : DefaultTableModel(null, columnNames) {
      override fun getColumnClass(column: Int) = File::class.java
    }
    val table = JTable(model)
    table.putClientProperty("Table.isFileList", java.lang.Boolean.TRUE)
    table.setCellSelectionEnabled(true)
    table.setIntercellSpacing(Dimension())
    table.setComponentPopupMenu(TablePopupMenu())
    table.setShowGrid(false)
    table.setFillsViewportHeight(true)
    table.setAutoCreateRowSorter(true)
    table.setDropMode(DropMode.INSERT_ROWS)
    table.setTransferHandler(FileTransferHandler())
    table.setDefaultRenderer(Any::class.java, FileIconTableCellRenderer(FileSystemView.getFileSystemView()))

    val sorter = table.getRowSorter() as TableRowSorter<out TableModel>
    // IntStream.range(0, 3).forEach { i -> sorter.setComparator(i, DefaultFileComparator(i)) }
    for (i in 0 until 3) {
      sorter.setComparator(i, DefaultFileComparator(i))
    }

    val check1 = JRadioButton("Default", true)
    check1.addItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
        // IntStream.range(0, 3).forEach { i -> sorter.setComparator(i, DefaultFileComparator(i)) }
        for (i in 0 until 3) {
          sorter.setComparator(i, DefaultFileComparator(i))
        }
      }
    }
    val check2 = JRadioButton("Directory < File", false)
    check2.addItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
        // IntStream.range(0, 3).forEach { i -> sorter.setComparator(i, FileComparator(i)) }
        for (i in 0 until 3) {
          sorter.setComparator(i, FileComparator(i))
        }
      }
    }
    val check3 = JRadioButton("Group Sorting", false)
    check3.addItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
        // IntStream.range(0, 3).forEach { i -> sorter.setComparator(i, FileGroupComparator(table, i)) }
        for (i in 0 until 3) {
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
    add(p, BorderLayout.NORTH)
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }
}

internal class FileIconTableCellRenderer(private val fileSystemView: FileSystemView) : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ) = (super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel).also {
    it.setHorizontalAlignment(SwingConstants.LEFT)
    it.setIcon(null)
    val file = value as? File ?: return it
    when (table.convertColumnIndexToModel(column)) {
      0 -> {
        it.setIcon(fileSystemView.getSystemIcon(file))
        it.setText(fileSystemView.getSystemDisplayName(file))
      }
      1 -> {
        it.setHorizontalAlignment(SwingConstants.RIGHT)
        it.setText(if (file.isDirectory()) null else file.length().toString())
      }
      2 -> it.setText(file.getAbsolutePath())
      // else -> {
      //   assert(false) { "Should never happened." }
      // }
    } // it.setText(file.getName());
  }
}

internal class FileTransferHandler : TransferHandler() {
  override fun importData(support: TransferHandler.TransferSupport) = try {
    if (canImport(support)) {
      val model = (support.getComponent() as JTable).getModel() as DefaultTableModel
      for (o in support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor) as List<*>) {
        (o as? File)?.also { file -> model.addRow((0..2).map { file }.toTypedArray()) }
      }
      true
    } else {
      false
    }
  } catch (ex: UnsupportedFlavorException) {
    ex.printStackTrace()
    false
  } catch (ex: IOException) {
    ex.printStackTrace()
    false
  }

  override fun canImport(support: TransferSupport) = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)

  override fun getSourceActions(component: JComponent) = TransferHandler.COPY
}

open class DefaultFileComparator(protected val column: Int) : Comparator<File>, Serializable {
  override fun compare(a: File, b: File): Int = when (column) {
    0 -> a.getName().compareTo(b.getName(), ignoreCase = true)
    1 -> java.lang.Long.compare(a.length(), b.length())
    else -> a.getAbsolutePath().compareTo(b.getAbsolutePath(), ignoreCase = true)
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}

internal class FileComparator(column: Int) : DefaultFileComparator(column) {
  override fun compare(a: File, b: File) = if (a.isDirectory() && !b.isDirectory()) -1
    else if (!a.isDirectory() && b.isDirectory()) 1
    else super.compare(a, b)

  companion object {
    private const val serialVersionUID = 1L
  }
}

// > dir /O:GN
// > ls --group-directories-first
internal class FileGroupComparator(private val table: JTable, column: Int) : DefaultFileComparator(column) {
  override fun compare(a: File, b: File): Int {
    // var flag = 1
    // val keys = table.getRowSorter().getSortKeys()
    // if (!keys.isEmpty()) {
    //   val sortKey = keys.get(0)
    //   if (sortKey.getColumn() == column && sortKey.getSortOrder() == SortOrder.DESCENDING) {
    //     flag = -1
    //   }
    // }
    val flag = table.getRowSorter().getSortKeys().firstOrNull()
      ?.takeIf { it.getColumn() == column && it.getSortOrder() == SortOrder.DESCENDING }
      ?.let { -1 } ?: 1
    return if (a.isDirectory() && !b.isDirectory()) -1 * flag
      else if (!a.isDirectory() && b.isDirectory()) 1 * flag
      else super.compare(a, b)
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}

internal class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    delete = add("delete")
    delete.addActionListener {
      val table = getInvoker() as JTable
      val model = table.getModel() as DefaultTableModel
      val selection = table.getSelectedRows()
      for (i in selection.indices.reversed()) {
        model.removeRow(table.convertRowIndexToModel(selection[i]))
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    val table = c as? JTable ?: return
    delete.setEnabled(table.getSelectedRowCount() > 0)
    super.show(table, x, y)
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
