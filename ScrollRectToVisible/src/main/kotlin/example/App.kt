package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer
import javax.swing.table.DefaultTableModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

private val columnNames = arrayOf("String", "Integer", "Boolean")
private val data = arrayOf(
  arrayOf("aaa", 12, true),
  arrayOf("bbb", 5, false),
  arrayOf("CCC", 92, true),
  arrayOf("DDD", 0, false)
)
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
}
private val listModel = DefaultListModel<LocalDateTime>()
private val timer = Timer(100, null)
@Transient
private var hierarchyListener: HierarchyListener? = null

fun makeUI(): Component {
  val t = JTabbedPane()

  val table = JTable(model)
  table.setAutoCreateRowSorter(true)
  table.setFillsViewportHeight(true)
  t.addTab("JTable", JScrollPane(table))

  val list = JList(listModel)
  t.addTab("JList", JScrollPane(list))

  val tree = JTree()
  t.addTab("JTree", JScrollPane(tree))

  timer.addActionListener {
    val date = LocalDateTime.now(ZoneId.systemDefault())
    // JTable
    model.addRow(arrayOf(date.toString(), model.getRowCount(), false))
    val i = table.convertRowIndexToView(model.getRowCount() - 1)
    val r = table.getCellRect(i, 0, true)
    table.scrollRectToVisible(r)
    // JList
    listModel.addElement(date)
    val index = listModel.getSize() - 1
    list.ensureIndexIsVisible(index)
    // JTree
    (tree.getModel() as? DefaultTreeModel)?.also { treeModel ->
      (treeModel.getRoot() as? DefaultMutableTreeNode)?.also { parent ->
        val newChild = DefaultMutableTreeNode(date)
        treeModel.insertNodeInto(newChild, parent, parent.getChildCount())
        tree.scrollPathToVisible(TreePath(newChild.getPath()))
      }
    }
  }
  timer.start()

  val p = object : JPanel(BorderLayout()) {
    override fun updateUI() {
      removeHierarchyListener(hierarchyListener)
      super.updateUI()
      hierarchyListener = HierarchyListener { e ->
        val isDisplayableChanged = e.getChangeFlags() and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
        if (isDisplayableChanged && !e.getComponent().isDisplayable()) {
          println("case DISPOSE_ON_CLOSE: hierarchyChanged")
          timer.stop()
        }
      }
      addHierarchyListener(hierarchyListener)
    }
  }
  p.add(t)
  p.setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
