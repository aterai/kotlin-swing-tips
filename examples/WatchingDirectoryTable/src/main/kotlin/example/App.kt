package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService
import javax.swing.*
import javax.swing.table.DefaultTableModel

private const val FULL_PATH_COLUMN = 2

private val logArea = JTextArea()
private val model = FileTableModel()

fun createUI(): Component {
  val table = JTable(model)
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()

  val tk = Toolkit.getDefaultToolkit()
  val loop = tk.getSystemEventQueue().createSecondaryLoop()
  val worker = Thread(DirectoryWatcher(loop))
  worker.start()
  if (!loop.enter()) {
    appendLog("Error")
  }

  val button = JButton("createTempFile")
  button.addActionListener {
    runCatching {
      Files.createTempFile("_", ".tmp").toFile().deleteOnExit()
    }.onFailure {
      appendLog(it.message)
    }
  }

  val buttonPanel = JPanel()
  buttonPanel.add(button)

  val centerPanel = JPanel(GridLayout(2, 1))
  centerPanel.add(JScrollPane(table))
  centerPanel.add(JScrollPane(logArea))

  return JPanel(BorderLayout()).also {
    it.addHierarchyListener { e ->
      val flags = e.changeFlags.toInt() and HierarchyEvent.DISPLAYABILITY_CHANGED
      if (flags != 0 && !e.component.isDisplayable) {
        worker.interrupt()
      }
    }
    it.add(buttonPanel, BorderLayout.NORTH)
    it.add(centerPanel)
    it.preferredSize = Dimension(320, 240)
  }
}

fun appendLog(str: String?) {
  logArea.append(str + "\n")
}

private fun rowIndexOf(path: Path): Int {
  val fullPath = path.toString()
  return (0..<model.rowCount).firstOrNull {
    val obj = model.getValueAt(it, FULL_PATH_COLUMN)
    fullPath == obj.toString()
  } ?: -1
}

private class DirectoryWatcher(
  private val loop: SecondaryLoop,
) : Runnable {
  override fun run() {
    runCatching {
      FileSystems.getDefault().newWatchService().use { watchService ->
        val dir = Paths.get(System.getProperty("java.io.tmpdir"))
        dir.register(
          watchService,
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_DELETE,
        )
        appendLog("register: $dir")
        processEvents(dir, watchService)
        loop.exit()
      }
    }.onFailure {
      appendLog(it.message)
    }
    loop.exit()
  }

  fun processEvents(dir: Path, watchService: WatchService) {
    while (true) {
      val key = runCatching {
        watchService.take()
      }.onFailure {
        EventQueue.invokeLater { appendLog("Interrupted") }
        Thread.currentThread().interrupt()
      }.getOrNull() ?: return

      for (event in key.pollEvents()) {
        val kind: WatchEvent.Kind<*>? = event.kind()
        if (kind === StandardWatchEventKinds.OVERFLOW) {
          continue
        }
        val filename = event.context() as Path
        val child = dir.resolve(filename)
        EventQueue.invokeLater {
          appendLog(String.format("%s: %s", kind, child))
          updateTable(kind, child)
        }
      }
      val valid = key.reset()
      if (!valid) {
        break
      }
    }
  }

  fun updateTable(kind: WatchEvent.Kind<*>?, child: Path) {
    if (kind === StandardWatchEventKinds.ENTRY_CREATE) {
      model.addPath(child)
    } else if (kind === StandardWatchEventKinds.ENTRY_DELETE) {
      val modelRow = rowIndexOf(child)
      if (modelRow >= 0) {
        model.removeRow(modelRow)
      }
    }
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val deleteMenuItem = add("delete")

  init {
    deleteMenuItem.addActionListener { deleteActionPerformed() }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTable) {
      deleteMenuItem.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
    }
  }

  fun deleteActionPerformed() {
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

private class FileTableModel : DefaultTableModel() {
  private var rowNumber = 0

  fun addPath(path: Path) {
    val obj = arrayOf(rowNumber, path.fileName, path.toAbsolutePath())
    super.addRow(obj)
    rowNumber++
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
      ColumnContext("No.", Number::class.java, false),
      ColumnContext("Name", String::class.java, false),
      ColumnContext("Full Path", String::class.java, false),
    )
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
