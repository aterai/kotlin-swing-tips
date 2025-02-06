package example

import java.awt.*
import java.util.TreeSet
import javax.swing.*
import javax.swing.plaf.basic.BasicProgressBarUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

private val columnNames = arrayOf("No.", "Name", "Progress", "")
private val model = DefaultTableModel(columnNames, 0)
private val table = object : JTable(model) {
  override fun updateUI() {
    super.updateUI()
    removeColumn(getColumnModel().getColumn(3))
    val progress = JProgressBar()
    val renderer = DefaultTableCellRenderer()
    val tc = getColumnModel().getColumn(2)
    tc.cellRenderer = TableCellRenderer { tbl, value, isSelected, hasFocus, row, column ->
      if (value is JProgressBar) {
        value
      } else if (value is Int) {
        progress.value = value
        progress
      } else {
        val txt = value?.toString() ?: ""
        renderer.getTableCellRendererComponent(
          tbl,
          txt,
          isSelected,
          hasFocus,
          row,
          column,
        )
      }
    }
  }
}
private val deletedRowSet: MutableSet<Int> = TreeSet()
private var number = 0

fun makeUI(): Component {
  val scrollPane = JScrollPane(table)
  scrollPane.viewport.background = Color.WHITE

  table.rowSorter = TableRowSorter(model)
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

private fun addProgressValue(
  name: String,
  iv: Int,
  worker: SwingWorker<*, *>?,
) {
  val obj = arrayOf(number, name, iv, worker)
  model.addRow(obj)
  number++
}

private fun addActionPerformed() {
  val key = model.rowCount
  val worker = object : BackgroundTask() {
    override fun process(c: List<Any>) {
      if (table.isDisplayable && !isCancelled) {
        c.forEach { model.setValueAt(it, key, 2) }
      } else {
        cancel(true)
      }
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
  addProgressValue("example", 0, worker)
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

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
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

private class IndeterminateProgressBarUI : BasicProgressBarUI() {
  public override fun incrementAnimationIndex() {
    super.incrementAnimationIndex()
  }
}

private open class BackgroundTask : SwingWorker<Int, Any>() {
  private val randomSleep = (1..50).random()

  @Throws(InterruptedException::class)
  override fun doInBackground(): Int {
    val lengthOfTask = calculateTaskSize()
    var current = 0
    var total = 0
    while (current <= lengthOfTask && !isCancelled) {
      publish(100 * current / lengthOfTask)
      total += doSomething()
      current++
    }
    return total
  }

  @Throws(InterruptedException::class)
  private fun calculateTaskSize(): Int {
    var total = 0
    val ui = IndeterminateProgressBarUI()
    val indeterminate = JProgressBar()
    indeterminate.setUI(ui)
    indeterminate.isIndeterminate = true
    for (i in 0..199) {
      val iv = randomSleep.toInt()
      Thread.sleep(iv.toLong())
      ui.incrementAnimationIndex()
      publish(indeterminate)
      total += iv
    }
    return 1 + total / 100
  }

  @Throws(InterruptedException::class)
  private fun doSomething(): Int {
    val iv = randomSleep.toInt()
    Thread.sleep(iv.toLong())
    return iv
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
