package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

private const val PROGRESS_COLUMN = 2
private const val WORKER_COLUMN = 3

private val columnNames = arrayOf("No.", "Name", "Progress", "")
private val model = DefaultTableModel(columnNames, 0)
private val table = object : JTable(model) {
  override fun updateUI() {
    super.updateUI()
    removeColumn(getColumnModel().getColumn(WORKER_COLUMN))
    val progressColumn = getColumnModel().getColumn(PROGRESS_COLUMN)
    progressColumn.setCellRenderer(ProgressRenderer())
  }
}
private var rowNumber = 0

fun createUI(): Component {
  table.rowSorter = TableRowSorter(model)
  addProgressRow("Name 1", 100, null)

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

private fun addProgressRow(name: String, progress: Int, worker: SwingWorker<*, *>?) {
  val rowData = arrayOf(rowNumber, name, progress, worker)
  model.addRow(rowData)
  rowNumber++
}

private fun addActionPerformed() {
  val worker = ProgressWorker()
  addProgressRow("example", 0, worker)
  worker.execute()
}

private fun rowIndexOf(worker: SwingWorker<*, *>?) = (0..<model.rowCount)
  .firstOrNull { model.getValueAt(it, WORKER_COLUMN) == worker }
  ?: -1

private fun setProgressValue(worker: SwingWorker<*, *>, value: Any) {
  val modelRow = rowIndexOf(worker)
  if (modelRow >= 0) {
    model.setValueAt(value, modelRow, PROGRESS_COLUMN)
  }
}

private class ProgressWorker : BackgroundTask() {
  override fun process(chunks: MutableList<Int>) {
    if (table.isDisplayable && !isCancelled) {
      chunks.forEach { setProgressValue(this, it) }
    } else {
      cancel(true)
    }
  }

  override fun done() {
    val text = if (isCancelled) "Cancelled" else this.resultMessage
    setProgressValue(this, text)
  }
}

private class ProgressRenderer : DefaultTableCellRenderer() {
  private val progressBar = JProgressBar()

  init {
    progressBar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component? = if (value is Int) {
    if (0 <= value && value < progressBar.maximum) {
      progressBar.setValue(value)
      progressBar
    } else {
      val text = if (value < 0) "Canceled" else "Done(0ms)"
      super.getTableCellRendererComponent(
        table,
        text,
        isSelected,
        hasFocus,
        row,
        column,
      )
    }
  } else {
    super.getTableCellRendererComponent(
      table,
      value.toString(),
      isSelected,
      hasFocus,
      row,
      column,
    )
  }
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
      val hasSelection = c.selectedRowCount > 0
      cancelMenuItem.setEnabled(hasSelection)
      deleteMenuItem.setEnabled(hasSelection)
      super.show(c, x, y)
    }
  }

  fun cancelWorker(modelRow: Int) {
    val worker = model.getValueAt(modelRow, WORKER_COLUMN) as? SwingWorker<*, *>
    if (worker?.isDone != true) {
      worker?.cancel(true)
    }
  }

  fun deleteActionPerformed() {
    val modelRows = table.selectedRows
      .map { viewRowIndex -> table.convertRowIndexToModel(viewRowIndex) }
      .sorted()
    for (i in modelRows.indices.reversed()) {
      cancelWorker(modelRows[i])
      model.removeRow(modelRows[i])
    }
    table.clearSelection()
  }

  fun cancelActionPerformed() {
    val selection = table.selectedRows
    for (viewRow in selection) {
      cancelWorker(table.convertRowIndexToModel(viewRow))
    }
    table.repaint()
  }
}

private open class BackgroundTask : SwingWorker<Int, Int>() {
  private val rnd = (1..50).random()
  protected val resultMessage: String
    get() {
      val message = runCatching {
        val total = get()
        if (total >= 0) "Done" else "Disposed"
      }.getOrNull() ?: "Interrupted"
      return message
    }

  @Throws(InterruptedException::class)
  override fun doInBackground(): Int {
    val lengthOfTask = 120
    var current = 0
    var total = 0
    while (current <= lengthOfTask && !isCancelled) {
      publish(100 * current / lengthOfTask)
      total += sleepRandomly()
      current++
    }
    return total
  }

  @Throws(InterruptedException::class)
  private fun sleepRandomly(): Int {
    val sleepTime = rnd
    Thread.sleep(sleepTime.toLong())
    return sleepTime
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
