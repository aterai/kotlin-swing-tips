package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.Objects
import java.util.TreeSet
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

class MainPanel : JPanel(BorderLayout()) {
  private val logger = JTextArea()
  private val model = FileModel()
  @Transient
  private val sorter = TableRowSorter<FileModel>(model)
  val deleteRowSet: MutableSet<Int> = TreeSet<Int>()

  init {

    val table = JTable(model)
    table.setRowSorter(sorter)
    table.setFillsViewportHeight(true)
    table.setComponentPopupMenu(TablePopupMenu())

    val col = table.getColumnModel().getColumn(0)
    col.setMinWidth(30)
    col.setMaxWidth(30)
    col.setResizable(false)

    val dir = Paths.get(System.getProperty("java.io.tmpdir"))
    val loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop()
    val worker = object : Thread() {
      override fun run() {
        var watcher: WatchService?
        try {
          watcher = FileSystems.getDefault().newWatchService()
          dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)
          append("register: $dir")
        } catch (ex: IOException) {
          throw UncheckedIOException(ex)
        }

        processEvents(dir, watcher!!)
        loop.exit()
      }
    }
    worker.start()
    if (!loop.enter()) {
      append("Error")
    }

    addHierarchyListener { e ->
      val isDisplayableChanged = e.getChangeFlags().toInt() and HierarchyEvent.DISPLAYABILITY_CHANGED != 0
      if (isDisplayableChanged && !e.getComponent().isDisplayable()) {
        worker.interrupt()
      }
    }

    val button = JButton("createTempFile")
    button.addActionListener({
      try {
        val path = Files.createTempFile("_", ".tmp")
        path.toFile().deleteOnExit()
      } catch (ex: IOException) {
        append(ex.message)
      }
    })

    val p = JPanel()
    p.add(button)

    val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    sp.setTopComponent(JScrollPane(table))
    sp.setBottomComponent(JScrollPane(logger))
    sp.setResizeWeight(.5)

    add(p, BorderLayout.NORTH)
    add(sp)
    setPreferredSize(Dimension(320, 240))
  }

  // Watching a Directory for Changes (The Java™ Tutorials > Essential Classes > Basic I/O)
  // https://docs.oracle.com/javase/tutorial/essential/io/notification.html
  // Process all events for keys queued to the watcher
  fun processEvents(dir: Path, watcher: WatchService) {
    while (true) {
      // wait for key to be signaled
      val key: WatchKey
      try {
        key = watcher.take()
      } catch (ex: InterruptedException) {
        EventQueue.invokeLater({ append("Interrupted") })
        return
      }

      for (event in key.pollEvents()) {
        val kind = event.kind()

        // This key is registered only for ENTRY_CREATE events,
        // but an OVERFLOW event can occur regardless if events
        // are lost or discarded.
        if (kind === StandardWatchEventKinds.OVERFLOW) {
          continue
        }

        // The filename is the context of the event.
        @Suppress("UNCHECKED_CAST")
        val ev = event as WatchEvent<Path>
        val filename = ev.context()

        val child = dir.resolve(filename)
        EventQueue.invokeLater({
          append(String.format("%s: %s", kind, child))
          updateTable(kind, child)
        })
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

  fun updateTable(kind: WatchEvent.Kind<*>, child: Path) {
    if (kind === StandardWatchEventKinds.ENTRY_CREATE) {
      model.addPath(child)
    } else if (kind === StandardWatchEventKinds.ENTRY_DELETE) {
      for (i in 0 until model.getRowCount()) {
        val value = model.getValueAt(i, 2)
        val path = Objects.toString(value, "")
        if (path == child.toString()) {
          deleteRowSet.add(i)
          // model.removeRow(i);
          break
        }
      }
      sorter.setRowFilter(object : RowFilter<TableModel, Int>() {
        override fun include(entry: RowFilter.Entry<out TableModel, out Int>): Boolean {
          return !deleteRowSet.contains(entry.getIdentifier())
        }
      })
    }
  }

  fun append(str: String?) {
    logger.append(str + "\n")
  }
}

internal class FileModel : DefaultTableModel() {
  private var number: Int = 0

  fun addPath(path: Path) {
    val obj = arrayOf<Any>(number, path.getFileName(), path.toAbsolutePath())
    super.addRow(obj)
    number++
  }

  override fun isCellEditable(row: Int, col: Int): Boolean {
    return COLUMN_ARRAY[col].isEditable
  }

  override fun getColumnClass(column: Int): Class<*> = COLUMN_ARRAY[column].columnClass

  override fun getColumnCount() = COLUMN_ARRAY.size

  override fun getColumnName(column: Int) = COLUMN_ARRAY[column].columnName

  private class ColumnContext(val columnName: String, val columnClass: Class<*>, val isEditable: Boolean)

  companion object {
    private val COLUMN_ARRAY = arrayOf(
        ColumnContext("No.", Int::class.java, false),
        ColumnContext("Name", String::class.java, false),
        ColumnContext("Full Path", String::class.java, false))
  }
}

internal class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    delete = add("delete")
    delete.addActionListener({
      val table = getInvoker() as JTable
      val model = table.getModel() as DefaultTableModel
      val selection = table.getSelectedRows()
      for (i in selection.indices.reversed()) {
        val midx = table.convertRowIndexToModel(selection[i])
        val path = Paths.get(Objects.toString(model.getValueAt(midx, 2)))
        try {
          Files.delete(path)
        } catch (ex: IOException) {
          Toolkit.getDefaultToolkit().beep()
        }
      }
    })
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTable) {
      delete.setEnabled(c.getSelectedRowCount() > 0)
      super.show(c, x, y)
    }
  }
}

fun main() {
  EventQueue.invokeLater({
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
  })
}
