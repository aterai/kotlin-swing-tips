package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.Timer
import javax.swing.table.DefaultTableModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

private val columnNames = arrayOf("String", "Integer", "Boolean")
private val data = arrayOf<Array<Any>>(
  arrayOf("aaa", 12, true),
  arrayOf("bbb", 5, false),
  arrayOf("CCC", 92, true),
  arrayOf("DDD", 0, false),
)
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
}
private val listModel = DefaultListModel<LocalDateTime>()
private val timer = Timer(100, null)
private var hierarchyListener: HierarchyListener? = null

fun makeUI(): Component {
  val t = JTabbedPane()

  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  t.addTab("JTable", JScrollPane(table))

  val list = JList(listModel)
  t.addTab("JList", JScrollPane(list))

  val tree = JTree()
  t.addTab("JTree", JScrollPane(tree))

  timer.addActionListener {
    val date = LocalDateTime.now(ZoneId.systemDefault())
    // JTable
    model.addRow(arrayOf<Any>(date.toString(), model.rowCount, false))
    val i = table.convertRowIndexToView(model.rowCount - 1)
    val r = table.getCellRect(i, 0, true)
    table.scrollRectToVisible(r)
    // JList
    listModel.addElement(date)
    val index = listModel.size - 1
    list.ensureIndexIsVisible(index)
    // JTree
    (tree.model as? DefaultTreeModel)?.also { treeModel ->
      (treeModel.root as? DefaultMutableTreeNode)?.also { parent ->
        val newChild = DefaultMutableTreeNode(date)
        treeModel.insertNodeInto(newChild, parent, parent.childCount)
        tree.scrollPathToVisible(TreePath(newChild.path))
      }
    }
  }
  timer.start()

  val p = object : JPanel(BorderLayout()) {
    override fun updateUI() {
      removeHierarchyListener(hierarchyListener)
      super.updateUI()
      hierarchyListener = HierarchyListener { e ->
        val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
        if (b && !e.component.isDisplayable) {
          // println("case DISPOSE_ON_CLOSE: hierarchyChanged")
          timer.stop()
        }
      }
      addHierarchyListener(hierarchyListener)
    }
  }
  p.add(t)
  p.preferredSize = Dimension(320, 240)
  return p
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
