package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

private val logger = JTextArea()
private val model = FileModel()

@Transient
private val sorter = TableRowSorter(model)
val deleteRowSet = mutableSetOf<Int>()

fun makeUI(): Component {
  val table = JTable(model)
  table.rowSorter = sorter
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()

  val col = table.columnModel.getColumn(0)
  col.minWidth = 30
  col.maxWidth = 30
  col.resizable = false

  val dir = Paths.get(System.getProperty("java.io.tmpdir"))
  val loop = Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop()
  val worker = object : Thread() {
    override fun run() {
      runCatching {
        FileSystems.getDefault().newWatchService()
      }.onSuccess {
        dir.register(it, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)
        append("register: $dir")
        processEvents(dir, it)
      }.onFailure {
        append(it.message)
      }
      loop.exit()
    }
  }
  worker.start()
  if (!loop.enter()) {
    append("Error")
  }

  val button = JButton("createTempFile")
  button.addActionListener {
    runCatching {
      Files.createTempFile("_", ".tmp").toFile().deleteOnExit()
    }.onFailure {
      append(it.message)
    }
  }

  val p = JPanel()
  p.add(button)

  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.topComponent = JScrollPane(table)
  sp.bottomComponent = JScrollPane(logger)
  sp.resizeWeight = .5

  return JPanel(BorderLayout()).also {
    it.addHierarchyListener { e ->
      val b = e.changeFlags.toInt() and HierarchyEvent.DISPLAYABILITY_CHANGED != 0
      if (b && !e.component.isDisplayable) {
        worker.interrupt()
      }
    }
    it.add(p, BorderLayout.NORTH)
    it.add(sp)
    it.preferredSize = Dimension(320, 240)
  }
}

// Watching a Directory for Changes (The Java™ Tutorials > Essential Classes > Basic I/O)
// https://docs.oracle.com/javase/tutorial/essential/io/notification.html
// Process all events for keys queued to the watcher
private fun processEvents(dir: Path, watcher: WatchService) {
  while (true) {
    // wait for key to be signaled
    val key = runCatching {
      watcher.take()
    }.onFailure {
      EventQueue.invokeLater { append("Interrupted") }
    }.getOrNull() ?: return

    for (event in key.pollEvents()) {
      val kind = event.kind()

      // This key is registered only for ENTRY_CREATE events,
      // but an OVERFLOW event can occur regardless if events
      // are lost or discarded.
      if (kind === StandardWatchEventKinds.OVERFLOW) {
        continue
      }

      (event.context() as? Path)?.also {
        val child = dir.resolve(it)
        EventQueue.invokeLater {
          append("$kind: $child")
          updateTable(kind, child)
        }
      }
    }

    // Reset the key -- this step is critical if you want to
    // receive further watch events.  If the key is no longer valid,
    // the directory is inaccessible so exit the loop.
    val valid = key.reset()
    if (!valid) {
      break
    }
  }
}

private fun updateTable(kind: WatchEvent.Kind<*>, child: Path) {
  if (kind === StandardWatchEventKinds.ENTRY_CREATE) {
    model.addPath(child)
  } else if (kind === StandardWatchEventKinds.ENTRY_DELETE) {
    for (i in 0 until model.rowCount) {
      val path = model.getValueAt(i, 2)?.toString() ?: ""
      if (path == child.toString()) {
        deleteRowSet.add(i)
        // model.removeRow(i);
        break
      }
    }
    sorter.rowFilter = object : RowFilter<TableModel, Int>() {
      override fun include(entry: Entry<out TableModel, out Int>) =
        !deleteRowSet.contains(entry.identifier)
    }
  }
}

fun append(str: String?) {
  logger.append(str + "\n")
}

private class FileModel : DefaultTableModel() {
  private var number = 0

  fun addPath(path: Path) {
    val obj = arrayOf(number, path.fileName, path.toAbsolutePath())
    super.addRow(obj)
    number++
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
      ColumnContext("Name", String::class.java, false),
      ColumnContext("Full Path", String::class.java, false)
    )
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
          val idx = table.convertRowIndexToModel(selection[i])
          model.getValueAt(idx, 2)?.toString()?.also {
            runCatching {
              Files.delete(Paths.get(it))
            }.onFailure {
              Toolkit.getDefaultToolkit().beep()
            }
          }
        }
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
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
