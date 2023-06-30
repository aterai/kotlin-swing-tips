package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.nio.file.Path
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val model = FileModel()
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()
  table.columnModel.getColumn(0).also {
    it.minWidth = 60
    it.maxWidth = 60
    it.resizable = false
  }

  val dtl = object : DropTargetAdapter() {
    override fun dragOver(e: DropTargetDragEvent) {
      if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        e.acceptDrag(DnDConstants.ACTION_COPY)
      } else {
        e.rejectDrag()
      }
    }

    override fun drop(e: DropTargetDropEvent) {
      runCatching {
        if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
          e.acceptDrop(DnDConstants.ACTION_COPY)
          val list = e.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
          list?.filterIsInstance<File>()?.forEach { model.addPath(it.toPath()) }
          e.dropComplete(true)
        } else {
          e.rejectDrop()
        }
      }.onFailure {
        e.rejectDrop()
      }
    }
  }
  DropTarget(table, DnDConstants.ACTION_COPY, dtl, true)

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class FileModel : DefaultTableModel() {
  private var number = 0
  fun addPath(path: Path) {
    super.addRow(arrayOf(number++, path.fileName, path.toAbsolutePath()))
  }

  override fun isCellEditable(row: Int, col: Int) = COLUMN_ARRAY[col].isEditable

  override fun getColumnClass(column: Int) = COLUMN_ARRAY[column].columnClass

  override fun getColumnCount() = COLUMN_ARRAY.size

  override fun getColumnName(column: Int) = COLUMN_ARRAY[column].columnName

  private data class ColumnContext(
    val columnName: String,
    val columnClass: Class<*>,
    val isEditable: Boolean
  )

  companion object {
    private val COLUMN_ARRAY = arrayOf(
      ColumnContext("No.", Number::class.java, false),
      ColumnContext("Name", String::class.java, true),
      ColumnContext("Full Path", String::class.java, true)
    )
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete = add("Remove only from JTable")

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

  override fun show(c: Component?, x: Int, y: Int) {
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
