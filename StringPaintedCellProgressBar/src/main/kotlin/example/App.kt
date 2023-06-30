package example

import java.awt.*
import java.util.TreeSet
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

private val columnNames = arrayOf("No.", "Name", "Progress", "")
private val model = DefaultTableModel(null, columnNames)
private val table = object : JTable(model) {
  override fun updateUI() {
    super.updateUI()
    removeColumn(getColumnModel().getColumn(3))
    val progress = JProgressBar()
    val renderer = DefaultTableCellRenderer()
    val tc = getColumnModel().getColumn(2)
    tc.setCellRenderer { tbl, value, isSelected, hasFocus, row, column ->
      if (value is ProgressValue) {
        val current = value.progress
        val lengthOfTask = value.lengthOfTask
        when {
          current < 0 -> renderer.getTableCellRendererComponent(
            tbl,
            "Canceled",
            isSelected,
            hasFocus,
            row,
            column
          )

          current < lengthOfTask -> progress.also {
            it.value = current * 100 / lengthOfTask
            it.isStringPainted = true
            it.string = "$current/$lengthOfTask"
          }

          else -> renderer.getTableCellRendererComponent(
            tbl,
            "Done",
            isSelected,
            hasFocus,
            row,
            column
          )
        }
      } else {
        renderer.getTableCellRendererComponent(
          tbl,
          value?.toString() ?: "",
          isSelected,
          hasFocus,
          row,
          column
        )
      }
    }
  }
}
private val deletedRowSet: MutableSet<Int> = TreeSet()
private var number = 0

private fun addProgressValue(name: String, value: ProgressValue, worker: SwingWorker<*, *>) {
  val obj = arrayOf(number, name, value, worker)
  model.addRow(obj)
  number++
}

fun addActionPerformed() {
  val key = model.rowCount
  val lengthOfTask = (100..200).random()
  val worker = object : BackgroundTask(lengthOfTask) {
    override fun process(c: List<ProgressValue>) {
      if (isCancelled) {
        return
      }
      if (!table.isDisplayable) {
        cancel(true)
        // executor.shutdown()
        return
      }
      c.forEach { model.setValueAt(it, key, 2) }
    }

    override fun done() {
      var i = -1
      val message = runCatching {
        i = get()
        if (i >= 0) "Done" else "Disposed"
      }.getOrNull() ?: "Interrupted"
      model.setValueAt("$message(${i}ms)", key, 2)
    }
  }
  val value = ProgressValue(lengthOfTask, 0)
  addProgressValue("example(max: $lengthOfTask)", value, worker)
  worker.execute()
}

private class TablePopupMenu : JPopupMenu() {
  private val cancelMenuItem: JMenuItem
  private val deleteMenuItem: JMenuItem

  init {
    add("add").addActionListener { addActionPerformed() }
    addSeparator()
    cancelMenuItem = add("cancel")
    cancelMenuItem.addActionListener { cancelActionPerformed() }
    deleteMenuItem = add("delete")
    deleteMenuItem.addActionListener { deleteActionPerformed() }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTable) {
      val flag = c.selectedRowCount > 0
      cancelMenuItem.isEnabled = flag
      deleteMenuItem.isEnabled = flag
      super.show(c, x, y)
    }
  }

  private fun getSwingWorker(identifier: Int) =
    model.getValueAt(identifier, 3) as? SwingWorker<*, *>

  private fun deleteActionPerformed() {
    val selection = table.selectedRows
    if (selection.isEmpty()) {
      return
    }
    for (i in selection) {
      val mi = table.convertRowIndexToModel(i)
      deletedRowSet.add(mi)
      getSwingWorker(mi)?.takeUnless { it.isDone }?.cancel(true)
    }
    val filter = object : RowFilter<TableModel, Int>() {
      override fun include(entry: Entry<out TableModel, out Int>) =
        !deletedRowSet.contains(entry.identifier)
    }
    (table.rowSorter as? TableRowSorter<out TableModel>)?.rowFilter = filter
    table.clearSelection()
    table.repaint()
  }

  private fun cancelActionPerformed() {
    for (i in table.selectedRows) {
      val mi = table.convertRowIndexToModel(i)
      getSwingWorker(mi)?.takeUnless { it.isDone }?.cancel(true)
    }
    table.repaint()
  }
}

fun makeUI(): Component {
  table.rowSorter = TableRowSorter(model)

  val scrollPane = JScrollPane(table)
  scrollPane.viewport.background = Color.WHITE
  table.componentPopupMenu = TablePopupMenu()
  table.fillsViewportHeight = true
  table.intercellSpacing = Dimension()
  table.setShowGrid(false)
  table.putClientProperty("terminateEditOnFocusLost", true)

  table.columnModel.getColumn(0).also {
    it.maxWidth = 60
    it.minWidth = 60
    it.resizable = false
  }

  val button = JButton("add")
  button.addActionListener { addActionPerformed() }
  return JPanel(BorderLayout()).also {
    it.add(button, BorderLayout.SOUTH)
    it.add(scrollPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class BackgroundTask(
  private val lengthOfTask: Int
) : SwingWorker<Int, ProgressValue>() {
  private val randomSleep = (1..49).random()

  @Throws(InterruptedException::class)
  override fun doInBackground(): Int {
    var current = 0
    while (current <= lengthOfTask && !isCancelled) {
      publish(ProgressValue(lengthOfTask, current))
      Thread.sleep(randomSleep.toLong())
      current++
    }
    return randomSleep * lengthOfTask
  }
}

private data class ProgressValue(val lengthOfTask: Int, val progress: Int)

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
