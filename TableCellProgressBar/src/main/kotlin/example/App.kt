package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.TreeSet
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

class MainPanel : JPanel(BorderLayout()) {
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
        val i = value as? Int ?: -1
        var text = "Done"
        if (i < 0) {
          text = "Canceled"
        } else if (i < progress.getMaximum()) { // < 100
          progress.setValue(i)
          progress.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
          return@setCellRenderer progress
        }
        renderer.getTableCellRendererComponent(tbl, text, isSelected, hasFocus, row, column)
      }
    }
  }
  private val deletedRowSet: MutableSet<Int> = TreeSet()
  private var number = 0

  private fun addProgressValue(name: String, iv: Int, worker: SwingWorker<*, *>?) {
    val obj = arrayOf(number, name, iv, worker)
    model.addRow(obj)
    number++
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
          return
        }
        c.forEach { model.setValueAt(it, key, 2) }
      }

      override fun done() {
        if (!isDisplayable()) {
          println("done: DISPOSE_ON_CLOSE")
          cancel(true)
          return
        }
        var i = -1
        val message = runCatching {
          i = get()
          if (i >= 0) "Done" else "Disposed"
        }.getOrNull() ?: "Interrupted"
        println("$key:$message(${i}ms)")
      }
    }
    addProgressValue("example", 0, worker)
    worker.execute()
  }

  private inner class TablePopupMenu : JPopupMenu() {
    private val cancelMenuItem: JMenuItem
    private val deleteMenuItem: JMenuItem

    override fun show(c: Component, x: Int, y: Int) {
      val table = c as? JTable ?: return
      val flag = table.getSelectedRowCount() > 0
      cancelMenuItem.setEnabled(flag)
      deleteMenuItem.setEnabled(flag)
      super.show(table, x, y)
    }

    private fun getSwingWorker(identifier: Int) = model.getValueAt(identifier, 3) as? SwingWorker<*, *>

    private fun deleteActionPerformed() {
      val selection = table.getSelectedRows()
      if (selection.isEmpty()) {
        return
      }
      for (i in selection) {
        val mi = table.convertRowIndexToModel(i)
        deletedRowSet.add(mi)
        getSwingWorker(mi)?.takeUnless { it.isDone() }?.cancel(true)
      }
      val sorter = table.rowSorter
      (sorter as? TableRowSorter<out TableModel>)?.setRowFilter(object : RowFilter<TableModel, Int>() {
        override fun include(entry: Entry<out TableModel, out Int>) = !deletedRowSet.contains(entry.getIdentifier())
      })
      table.clearSelection()
      table.repaint()
    }

    private fun cancelActionPerformed() {
      for (i in table.getSelectedRows()) {
        val mi = table.convertRowIndexToModel(i)
        getSwingWorker(mi)?.takeUnless { it.isDone() }?.cancel(true)
      }
      table.repaint()
    }

    init {
      add("add").addActionListener { addActionPerformed() }
      addSeparator()
      cancelMenuItem = add("cancel")
      cancelMenuItem.addActionListener { cancelActionPerformed() }
      deleteMenuItem = add("delete")
      deleteMenuItem.addActionListener { deleteActionPerformed() }
    }
  }

  init {
    table.setRowSorter(TableRowSorter(model))
    addProgressValue("Name 1", 100, null)

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

    val button = JButton("add")
    button.addActionListener { addActionPerformed() }
    add(button, BorderLayout.SOUTH)
    add(scrollPane)
    setPreferredSize(Dimension(320, 240))
  }
}

open class BackgroundTask : SwingWorker<Int, Int>() {
  private val sleepDummy = (1..50).random()
  @Throws(InterruptedException::class)
  override fun doInBackground(): Int {
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
