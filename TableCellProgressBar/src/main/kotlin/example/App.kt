package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.TreeSet
import java.util.concurrent.ExecutionException
import java.util.concurrent.ConcurrentHashMap
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

class MainPanel : JPanel(BorderLayout()) {
  protected val model = WorkerModel()
  protected val table = JTable(model)
  protected val sorter = TableRowSorter<WorkerModel>(model)
  protected val deleteRowSet: MutableSet<Int> = TreeSet<Int>()

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

    val button = JButton("add")
    button.addActionListener { addActionPerformed() }
    add(button, BorderLayout.SOUTH)
    add(scrollPane)
    setPreferredSize(Dimension(320, 240))
  }

  protected fun addActionPerformed() {
    val key = model.getRowCount()
    val worker = object : BackgroundTask() {
      protected override fun process(c: List<Int>) {
        if (isCancelled()) {
          return
        }
        if (!isDisplayable()) {
          println("process: DISPOSE_ON_CLOSE")
          cancel(true)
          return
        }
        c.forEach { model.setValueAt(it, key, 2) }
      }

      protected override fun done() {
        if (!isDisplayable()) {
          println("done: DISPOSE_ON_CLOSE")
          cancel(true)
          return
        }
        var i = -1
        val text = if (isCancelled()) "Cancelled" else try {
          i = get()
          if (i >= 0) "Done" else "Disposed"
        } catch (ex: InterruptedException) {
          "Interrupted"
        } catch (ex: ExecutionException) {
          ex.message
        }
        System.out.format("%s:%s(%dms)%n", key, text, i)
        // executor.remove(this);
      }
    }
    model.addProgressValue("example", 0, worker)
    worker.execute()
  }

  protected fun cancelActionPerformed() {
    for (i in table.getSelectedRows()) {
      val midx = table.convertRowIndexToModel(i)
      model.getSwingWorker(midx)?.takeUnless { it.isDone() }?.cancel(true)
    }
    table.repaint()
  }

  protected fun deleteActionPerformed() {
    val selection = table.getSelectedRows()
    if (selection.size == 0) {
      return
    }
    for (i in selection) {
      val midx = table.convertRowIndexToModel(i)
      deleteRowSet.add(midx)
      model.getSwingWorker(midx)?.takeUnless { it.isDone() }?.cancel(true)
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
  private val sleepDummy = (1..100).random() // Random().nextInt(100) + 1

  protected override fun doInBackground(): Int? {
    val lengthOfTask = 120
    var current = 0
    while (current <= lengthOfTask && !isCancelled()) {
      publish(100 * current / lengthOfTask)
      try {
        Thread.sleep(sleepDummy.toLong())
      } catch (ex: InterruptedException) {
        break
      }
      current++
    }
    return sleepDummy * lengthOfTask
  }
}

open class WorkerModel : DefaultTableModel() {
  private val swmap = ConcurrentHashMap<Int, SwingWorker<Int, Int>>()
  private var number = 0

  fun addProgressValue(name: String, iv: Int, worker: SwingWorker<Int, Int>?) {
    super.addRow(arrayOf<Any>(number, name, iv))
    worker?.also { swmap.put(number, it) }
    number++
  }

  fun getSwingWorker(identifier: Int): SwingWorker<Int, Int>? {
    val key = getValueAt(identifier, 0) as Int
    return swmap.get(key)
  }

  override fun isCellEditable(row: Int, col: Int) = COLUMN_ARRAY[col].isEditable

  override fun getColumnClass(column: Int) = COLUMN_ARRAY[column].columnClass

  override fun getColumnCount() = COLUMN_ARRAY.size

  override fun getColumnName(column: Int) = COLUMN_ARRAY[column].columnName

  private class ColumnContext(val columnName: String, val columnClass: Class<*>, val isEditable: Boolean)

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
    val i = value as Int
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
