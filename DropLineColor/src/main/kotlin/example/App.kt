package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.event.ActionEvent
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeSelectionModel

fun makeUI() = JTabbedPane().also {
  it.add("JList", makeListPanel())
  it.add("JTable", makeTablePanel())
  it.add("JTree", makeTreePanel())
  // Default drop line color: UIManager.put("List.dropLineColor", null)
  // Hide drop lines: UIManager.put("List.dropLineColor", new Color(0x0, true))
  it.preferredSize = Dimension(320, 240)
}

private fun makeColorChooserButton(key: String): JButton {
  val button = JButton(key)
  button.addActionListener {
    val c = JColorChooser.showDialog(button.rootPane, key, UIManager.getColor(key))
    UIManager.put(key, c)
  }
  return button
}

private fun makeListPanel(): Component {
  val model = DefaultListModel<String>()
  model.addElement("1111")
  model.addElement("22222222")
  model.addElement("333333333333")
  model.addElement("****")
  val list = JList(model)
  list.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
  list.transferHandler = ListItemTransferHandler()
  list.dropMode = DropMode.INSERT
  list.dragEnabled = true

  // Disable row Cut, Copy, Paste
  val map = list.actionMap
  val dummy = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent?) {
      /* Dummy action */
    }
  }
  map.put(TransferHandler.getCutAction().getValue(Action.NAME), dummy)
  map.put(TransferHandler.getCopyAction().getValue(Action.NAME), dummy)
  map.put(TransferHandler.getPasteAction().getValue(Action.NAME), dummy)
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(makeColorChooserButton("List.dropLineColor"))
  val p = JPanel(BorderLayout())
  p.add(JScrollPane(list))
  p.add(box, BorderLayout.SOUTH)
  return p
}

private fun makeTablePanel(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("AAA", 12, true),
    arrayOf("aaa", 1, false),
    arrayOf("BBB", 13, true),
    arrayOf("bbb", 2, false),
    arrayOf("CCC", 15, true),
    arrayOf("ccc", 3, false),
    arrayOf("DDD", 17, true),
    arrayOf("ddd", 4, false),
    arrayOf("EEE", 18, true),
    arrayOf("eee", 5, false),
    arrayOf("FFF", 19, true),
    arrayOf("fff", 6, false),
    arrayOf("GGG", 92, true),
    arrayOf("ggg", 0, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int): Class<*> {
      return when (column) {
        0 -> String::class.java
        1 -> Number::class.java
        2 -> java.lang.Boolean::class.java
        else -> super.getColumnClass(column)
      }
    }
  }
  val table = JTable(model)
  table.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
  table.transferHandler = TableRowTransferHandler()
  table.dropMode = DropMode.INSERT_ROWS
  table.dragEnabled = true
  table.fillsViewportHeight = true

  // Disable row Cut, Copy, Paste
  val map = table.actionMap
  val dummy = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      /* Dummy action */
    }
  }
  map.put(TransferHandler.getCutAction().getValue(Action.NAME), dummy)
  map.put(TransferHandler.getCopyAction().getValue(Action.NAME), dummy)
  map.put(TransferHandler.getPasteAction().getValue(Action.NAME), dummy)
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(makeColorChooserButton("Table.dropLineColor"))
  box.add(makeColorChooserButton("Table.dropLineShortColor"))
  val p = JPanel(BorderLayout())
  p.add(JScrollPane(table))
  p.add(box, BorderLayout.SOUTH)
  return p
}

private fun makeTree(handler: TransferHandler): JTree {
  val tree = JTree()
  tree.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  tree.isRootVisible = false
  tree.dragEnabled = true
  tree.transferHandler = handler
  tree.dropMode = DropMode.INSERT
  tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

  // Disable node Cut action
  val dummy = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent?) {
      /* Dummy action */
    }
  }
  tree.actionMap.put(TransferHandler.getCutAction().getValue(Action.NAME), dummy)

  for (i in 0 until tree.rowCount) {
    tree.expandRow(i)
  }
  return tree
}

private fun makeTreePanel(): Component {
  val handler = TreeTransferHandler()
  val p = JPanel(GridLayout(1, 2))
  p.add(JScrollPane(makeTree(handler)))
  p.add(JScrollPane(makeTree(handler)))
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(makeColorChooserButton("Tree.dropLineColor"))
  val panel = JPanel(BorderLayout())
  panel.add(p)
  panel.add(box, BorderLayout.SOUTH)
  return panel
}

private class ListItemTransferHandler : TransferHandler() {
  private val localObjectFlavor = DataFlavor(List::class.java, "List of items")
  private var source: JList<*>? = null
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable? {
    val src = c as? JList<*> ?: return null
    source = src
    src.selectedIndices.forEach { selectedIndices.add(it) }
    val transferObjects = src.selectedValuesList
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(localObjectFlavor)

      override fun isDataFlavorSupported(flavor: DataFlavor) = localObjectFlavor == flavor

      @Throws(UnsupportedFlavorException::class, IOException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor)) {
          transferObjects
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(info: TransferSupport) = info.isDrop &&
    info.isDataFlavorSupported(localObjectFlavor) &&
    info.dropLocation is JList.DropLocation

  override fun getSourceActions(c: JComponent) = MOVE // COPY_OR_MOVE

  override fun importData(info: TransferSupport): Boolean {
    val dl = info.dropLocation
    val target = info.component
    if (dl !is JList.DropLocation || target !is JList<*>) {
      return false
    }
    @Suppress("UNCHECKED_CAST")
    val listModel = target.model as DefaultListModel<Any>
    val max = listModel.size
    // var index = minOf(maxOf(0, dl.getIndex()), max)
    // var index = dl.index.coerceIn(0, max)
    var index = dl.index.takeIf { it in 0 until max } ?: max // -1 -> max
    addIndex = index
    val values = runCatching {
      info.transferable.getTransferData(localObjectFlavor) as? List<*>
    }.getOrNull().orEmpty()
    for (o in values) {
      val i = index++
      listModel.add(i, o)
      target.addSelectionInterval(i, i)
    }
    addCount = if (target == source) values.size else 0
    return values.isNotEmpty()
  }

  override fun exportDone(c: JComponent, data: Transferable, action: Int) {
    cleanup(c, action == MOVE)
  }

  private fun cleanup(c: JComponent, remove: Boolean) {
    if (remove && selectedIndices.isNotEmpty()) {
      // If we are moving items around in the same list, we
      // need to adjust the indices accordingly, since those
      // after the insertion point have moved.
      val selectedList = if (addCount > 0) {
        selectedIndices.map { if (it >= addIndex) it + addCount else it }
      } else {
        selectedIndices.toList()
      }
      ((c as? JList<*>)?.model as? DefaultListModel<*>)?.also { model ->
        for (i in selectedList.indices.reversed()) {
          model.remove(selectedList[i])
        }
      }
    }
    selectedIndices.clear()
    addCount = 0
    addIndex = -1
  }
}

private class TableRowTransferHandler : TransferHandler() {
  // private var indices: IntArray? = null
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable {
    c.rootPane.glassPane.isVisible = true
    val table = c as JTable
    val model = table.model as DefaultTableModel
    table.selectedRows.forEach { selectedIndices.add(it) }
    val transferredObjects = table.selectedRows.map { model.dataVector[it] }
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor)) {
          transferredObjects
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(info: TransferSupport): Boolean {
    val c = info.component as? JComponent ?: return false
    val glassPane = c.rootPane.glassPane
    val canDrop = info.isDrop && info.isDataFlavorSupported(FLAVOR)
    glassPane.cursor = if (canDrop) DragSource.DefaultMoveDrop else DragSource.DefaultMoveNoDrop
    return canDrop
  }

  override fun getSourceActions(c: JComponent?) = MOVE

  override fun importData(info: TransferSupport): Boolean {
    val tdl = info.dropLocation
    val target = info.component
    if (tdl !is JTable.DropLocation || target !is JTable) {
      return false
    }
    val model = target.model as DefaultTableModel
    val max = model.rowCount
    var index = tdl.row
    index = if (index in 0 until max) index else max
    addIndex = index
    val values = runCatching {
      info.transferable.getTransferData(FLAVOR) as? List<*>
    }.getOrNull().orEmpty()
    addCount = values.size
    for (o in values) {
      val row = index++
      val list = o as? List<*> ?: continue
      val array = arrayOfNulls<Any?>(list.size)
      for ((i, v) in list.withIndex()) {
        array[i] = v
      }
      model.insertRow(row, array)
      target.selectionModel.addSelectionInterval(row, row)
    }
    return values.isNotEmpty()
  }

  override fun exportDone(
    c: JComponent,
    data: Transferable?,
    action: Int
  ) {
    cleanup(c, action == MOVE)
  }

  private fun cleanup(c: JComponent, remove: Boolean) {
    c.rootPane.glassPane.isVisible = false
    if (remove && selectedIndices.isNotEmpty()) {
      val selectedList = if (addCount > 0) {
        selectedIndices.map { if (it >= addIndex) it + addCount else it }
      } else {
        selectedIndices.toList()
      }
      ((c as? JTable)?.model as? DefaultTableModel)?.also { model ->
        for (i in selectedList.indices.reversed()) {
          model.removeRow(selectedList[i])
        }
      }
    }
    selectedIndices.clear()
    addCount = 0
    addIndex = -1
  }

  companion object {
    private val FLAVOR = DataFlavor(List::class.java, "List of items")
  }
}

private class TreeTransferHandler : TransferHandler() {
  private var source: JTree? = null
  override fun createTransferable(c: JComponent): Transferable? {
    if (c !is JTree || c.selectionModel == null) {
      return null
    }
    source = c
    val paths = c.selectionPaths
    val nodes = arrayOfNulls<DefaultMutableTreeNode?>(paths?.size ?: 0)
    paths?.indices?.forEach {
      nodes[it] = paths[it].lastPathComponent as? DefaultMutableTreeNode
    }
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor)) {
          nodes
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun getSourceActions(c: JComponent) = MOVE

  override fun canImport(support: TransferSupport) = support.isDrop &&
    support.isDataFlavorSupported(FLAVOR) &&
    support.component != source

  override fun importData(support: TransferSupport): Boolean {
    val nodes = runCatching {
      support.transferable.getTransferData(FLAVOR) as? Array<*>
    }.getOrNull()?.filterIsInstance<DefaultMutableTreeNode>() ?: return false // .orEmpty()

    return (support.dropLocation as? JTree.DropLocation)?.let { dl ->
      val childIndex = dl.childIndex
      val dest = dl.path
      val parent = dest.lastPathComponent as DefaultMutableTreeNode
      val tree = support.component as JTree
      val model = tree.model as DefaultTreeModel
      val idx = AtomicInteger(if (childIndex < 0) parent.childCount else childIndex)
      nodes.forEach {
        val clone = DefaultMutableTreeNode(it.userObject)
        model.insertNodeInto(deepCopyTreeNode(it, clone), parent, idx.incrementAndGet())
      }
      true
    } ?: false
  }

  override fun exportDone(src: JComponent?, data: Transferable?, action: Int) {
    if (action == MOVE && src is JTree) {
      val model = src.model as? DefaultTreeModel
      val selectionPaths = src.selectionPaths
      if (model != null && selectionPaths != null) {
        for (path in selectionPaths) {
          model.removeNodeFromParent(path.lastPathComponent as? MutableTreeNode)
        }
      }
    }
  }

  private fun deepCopyTreeNode(src: DefaultMutableTreeNode, tgt: DefaultMutableTreeNode): DefaultMutableTreeNode {
    src.children().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .forEach {
        val clone = DefaultMutableTreeNode(it.userObject)
        tgt.add(clone)
        if (!it.isLeaf) {
          deepCopyTreeNode(it, clone)
        }
      }
    return tgt
  }

  companion object {
    private const val NAME = "Array of DefaultMutableTreeNode"
    private val FLAVOR = DataFlavor(Array<DefaultMutableTreeNode>::class.java, NAME)
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
