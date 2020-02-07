package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

class MainPanel : JPanel(BorderLayout()) {
  private val model = WorkerModel()
  private val table = JTable(model)
  private val sorter = TableRowSorter(model)
  private val deleteRowSet: MutableSet<Int> = TreeSet()
  // private final ExecutorService executor = Executors.newFixedThreadPool(1);
  private val executor: ExecutorService = Executors.newSingleThreadExecutor()

  init {
    table.setRowSorter(sorter)
    model.addProgressValue("Name 1", 100, null)

    val scrollPane = JScrollPane(table)
    scrollPane.getViewport().setBackground(Color.WHITE)
    table.setComponentPopupMenu(TablePopupMenu())
    table.setFillsViewportHeight(true)
    table.setIntercellSpacing(Dimension())
    table.setShowGrid(false)
    table.putClientProperty("terminateEditOnFocusLost", true)

    table.getColumnModel().getColumn(0).also {
      it.setMaxWidth(60)
      it.setMinWidth(60)
      it.setResizable(false)
    }
    table.getColumnModel().getColumn(2).setCellRenderer(ProgressRenderer())

    addHierarchyListener { e ->
      val cf = e.getChangeFlags().toInt() and HierarchyEvent.DISPLAYABILITY_CHANGED
      if (cf != 0 && !e.getComponent().isDisplayable()) {
        executor.shutdownNow()
      }
    }

    val button = JButton("add")
    button.addActionListener { addActionPerformed() }
    add(button, BorderLayout.SOUTH)
    add(scrollPane)
    setPreferredSize(Dimension(320, 240))
  }

  private fun addActionPerformed() {
    val key = model.getRowCount()
    val worker = object : BackgroundTask() {
      override fun process(c: List<Int>) {
        if (isCancelled()) {
          return
        }
        if (!isDisplayable()) {
          println("process: DISPOSE_ON_CLOSE")
          cancel(true)
          executor.shutdown()
          return
        }
        c.forEach { model.setValueAt(it, key, 2) }
      }

      override fun done() {
        if (!isDisplayable()) {
          println("done: DISPOSE_ON_CLOSE")
          cancel(true)
          executor.shutdown()
          return
        }
        var i = -1
        val message = runCatching {
          i = get()
          if (i >= 0) "Done" else "Disposed"
        }.getOrNull() ?: "Interrupted"
        println("$key:$message(${i}ms)")
        // executor.remove(this)
      }
    }
    model.addProgressValue("example", 0, worker)
    executor.execute(worker)
  }

  private fun cancelActionPerformed() {
    for (i in table.getSelectedRows()) {
      val mi = table.convertRowIndexToModel(i)
      model.getSwingWorker(mi)?.takeUnless { it.isDone() }?.cancel(true)
    }
    table.repaint()
  }

  private fun deleteActionPerformed() {
    val selection = table.getSelectedRows()
    if (selection.isEmpty()) {
      return
    }
    for (i in selection) {
      val mi = table.convertRowIndexToModel(i)
      deleteRowSet.add(mi)
      model.getSwingWorker(mi)?.takeUnless { it.isDone() }?.cancel(true)
    }
    sorter.setRowFilter(object : RowFilter<TableModel, Int>() {
      override fun include(entry: Entry<out TableModel, out Int>) = !deleteRowSet.contains(entry.getIdentifier())
    })
    table.clearSelection()
    table.repaint()
  }

  private inner class TablePopupMenu : JPopupMenu() {
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

    override fun show(c: Component, x: Int, y: Int) {
      val table = c as? JTable ?: return
      val flag = table.getSelectedRowCount() > 0
      cancelMenuItem.setEnabled(flag)
      deleteMenuItem.setEnabled(flag)
      super.show(table, x, y)
    }
  }
}

open class BackgroundTask : SwingWorker<Int, Int>() {
  private val sleepDummy = (1..50).random()

  @Throws(InterruptedException::class)
  override fun doInBackground(): Int? {
    val lengthOfTask = 120
    var current = 0
    while (current <= lengthOfTask && !isCancelled()) {
      publish(100 * current / lengthOfTask)
      Thread.sleep(sleepDummy.toLong())
      current++
    }
    return sleepDummy * lengthOfTask
  }
}

open class WorkerModel : DefaultTableModel() {
  private val workerMap = ConcurrentHashMap<Int, SwingWorker<Int, Int>>()
  private var number = 0

  fun addProgressValue(name: String, iv: Int, worker: SwingWorker<Int, Int>?) {
    super.addRow(arrayOf(number, name, iv))
    worker?.also { workerMap[number] = it }
    number++
  }

  fun getSwingWorker(identifier: Int): SwingWorker<Int, Int>? {
    val key = getValueAt(identifier, 0) as? Int ?: -1
    return workerMap[key]
  }

  override fun isCellEditable(row: Int, col: Int) = COLUMN_ARRAY[col].isEditable

  override fun getColumnClass(column: Int) = COLUMN_ARRAY[column].columnClass

  override fun getColumnCount() = COLUMN_ARRAY.size

  override fun getColumnName(column: Int) = COLUMN_ARRAY[column].columnName

  private data class ColumnContext(val columnName: String, val columnClass: Class<*>, val isEditable: Boolean)

  companion object {
    private val COLUMN_ARRAY = arrayOf(
      ColumnContext("No.", Number::class.java, false),
      ColumnContext("Name", String::class.java, false),
      ColumnContext("Progress", Number::class.java, false))
  }
}

internal class ProgressRenderer : DefaultTableCellRenderer() {
  private val progress = JProgressBar()
  private val renderer: JPanel? = JPanel(BorderLayout())

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val i = value as? Int ?: -1
    var text = "Done"
    if (i < 0) {
      text = "Canceled"
    } else if (i < progress.getMaximum() && renderer != null) { // < 100
      progress.setValue(i)
      renderer.add(progress)
      renderer.setOpaque(false)
      renderer.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
      return renderer
    }
    super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column)
    return this
  }

  override fun updateUI() {
    super.updateUI()
    setOpaque(false)
    renderer?.also { SwingUtilities.updateComponentTreeUI(it) }
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
