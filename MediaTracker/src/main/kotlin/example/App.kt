package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

private val imageId = AtomicInteger(0)
private val model = FileModel()
private val table = JTable(model)
private val scroll = JScrollPane(table)
private var tracker: MediaTracker? = null

fun makeUI(): Component {
  table.autoCreateRowSorter = true
  table.inheritsPopupMenu = true
  scroll.componentPopupMenu = TablePopupMenu()

  val dtl = ImageDropTargetListener()
  DropTarget(table, DnDConstants.ACTION_COPY, dtl, true)
  DropTarget(scroll.viewport, DnDConstants.ACTION_COPY, dtl, true)

  table.columnModel.getColumn(0).also {
    it.minWidth = 60
    it.maxWidth = 60
    it.resizable = false
  }

  val cl = Thread.currentThread().contextClassLoader
  val uri = cl.getResource("example/test.png")?.toURI()
  if (uri != null) {
    runCatching { Paths.get(uri) }.onSuccess { addImage(it) }
  }

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ImageDropTargetListener : DropTargetAdapter() {
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
        (e.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)?.also {
          for (o in it) {
            if (o is File) {
              addImage(o.toPath())
            }
          }
          e.dropComplete(true)
        } ?: e.rejectDrop()
      } else {
        e.rejectDrop()
      }
    }.onFailure {
      e.rejectDrop()
    }
  }
}

fun addImage(path: Path) {
  val id = imageId.getAndIncrement()
  val img = Toolkit.getDefaultToolkit().createImage(path.toAbsolutePath().toString())
  val mediaTracker = tracker ?: MediaTracker(table)
  tracker = mediaTracker
  mediaTracker.addImage(img, id)
  object : SwingWorker<Void?, Void?>() {
    @Throws(InterruptedException::class)
    override fun doInBackground(): Void? {
      mediaTracker.waitForID(id)
      return null
    }

    override fun done() {
      if (!table.isDisplayable) {
        cancel(true)
        return
      }
      if (!mediaTracker.isErrorID(id)) {
        model.addRowData(RowData(id, path, img.getWidth(table), img.getHeight(table)))
      }
      mediaTracker.removeImage(img)
    }
  }.execute()
}

private class FileModel : DefaultTableModel() {
  private val columnList = listOf(
    ColumnContext("No.", Number::class.java, false),
    ColumnContext("Name", String::class.java, false),
    ColumnContext("Full Path", String::class.java, false),
    ColumnContext("Width", Number::class.java, false),
    ColumnContext("Height", Number::class.java, false)
  )

  override fun isCellEditable(row: Int, col: Int) = columnList[col].isEditable

  override fun getColumnClass(column: Int) = columnList[column].columnClass

  override fun getColumnCount() = columnList.size

  override fun getColumnName(column: Int) = columnList[column].columnName

  fun addRowData(t: RowData) {
    super.addRow(arrayOf(t.id, t.name, t.absolutePath, t.width, t.height))
  }
}

private data class ColumnContext(
  val columnName: String,
  val columnClass: Class<*>,
  val isEditable: Boolean
)

private data class RowData(val id: Int, val path: Path, val width: Int, val height: Int) {
  val name get() = path.fileName.toString()
  val absolutePath get() = path.toAbsolutePath().toString()
}

private class TablePopupMenu : JPopupMenu() {
  private val delete = add("Remove from list")

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
