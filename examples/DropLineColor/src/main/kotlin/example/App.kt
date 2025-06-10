package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.event.ActionEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
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
  // Hide drop lines: UIManager.put("List.dropLineColor", Color(0x0, true))
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
  val data = arrayOf<Array<Any>>(
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
    arrayOf("ggg", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> String::class.java
      1 -> Number::class.java
      2 -> Boolean::class.javaObjectType
      else -> super.getColumnClass(column)
    }
  }
  val table = JTable(model)
  table.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
  table.transferHandler = TableRowTransferHandler()
  table.dropMode = DropMode.INSERT_ROWS
  table.dragEnabled = true
  table.fillsViewportHeight = true

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
  val empty = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      // do nothing action
    }
  }
  tree.actionMap.put(TransferHandler.getCutAction().getValue(Action.NAME), empty)

  for (i in 0..<tree.rowCount) {
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
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable {
    val source = (c as? JList<*>)?.also { s ->
      s.selectedIndices.forEach { selectedIndices.add(it) }
    }
    val selectedValues = source?.selectedValuesList
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(
        flavor: DataFlavor,
      ) = if (isDataFlavorSupported(flavor) && selectedValues != null) {
        selectedValues
      } else {
        throw UnsupportedFlavorException(flavor)
      }
    }
  }

  override fun canImport(info: TransferSupport) = info.isDataFlavorSupported(FLAVOR)

  override fun getSourceActions(c: JComponent) = COPY_OR_MOVE

  override fun importData(info: TransferSupport): Boolean {
    val target = info.component as? JList<*> ?: return false
    var index = getIndex(info)
    addIndex = index
    val values = runCatching {
      info.transferable.getTransferData(FLAVOR) as? List<*>
    }.getOrNull().orEmpty()

    @Suppress("UNCHECKED_CAST")
    (target.model as? DefaultListModel<Any>)?.also {
      for (o in values) {
        val i = index++
        it.add(i, o)
        target.addSelectionInterval(i, i)
      }
    }
    addCount = if (info.isDrop) values.size else 0
    // target.requestFocusInWindow()
    return values.isNotEmpty()
  }

  override fun importData(
    comp: JComponent,
    t: Transferable,
  ) = importData(TransferSupport(comp, t))

  override fun exportDone(
    c: JComponent,
    data: Transferable,
    action: Int,
  ) {
    cleanup(c, action == MOVE)
  }

  private fun cleanup(
    c: JComponent,
    remove: Boolean,
  ) {
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

  private fun getIndex(info: TransferSupport): Int {
    val target = info.component as? JList<*> ?: return -1
    var index = if (info.isDrop) { // Mouse Drag & Drop
      val tdl = info.dropLocation
      if (tdl is JList.DropLocation) {
        tdl.index
      } else {
        target.selectedIndex
      }
    } else { // Keyboard Copy & Paste
      target.selectedIndex
    }
    val max = (target.model as? DefaultListModel<*>)?.size ?: -1
    index = if (index < 0) max else index
    index = index.coerceAtMost(max)
    return index
  }

  companion object {
    private val FLAVOR = DataFlavor(List::class.java, "List of items")
  }
}

private class TableRowTransferHandler : TransferHandler() {
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable {
    c.rootPane.glassPane.isVisible = true
    val table = c as? JTable
    val model = table?.model as? DefaultTableModel
    val transferredRows = mutableListOf<Any>()
    selectedIndices.clear()
    if (model != null) {
      table.selectedRows.forEach {
        selectedIndices.add(it)
        transferredRows.add(model.dataVector[it])
      }
    }
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(
        flavor: DataFlavor,
      ) = if (isDataFlavorSupported(flavor)) {
        transferredRows
      } else {
        throw UnsupportedFlavorException(flavor)
      }
    }
  }

  override fun canImport(info: TransferSupport): Boolean {
    val canDrop = info.isDrop && info.isDataFlavorSupported(FLAVOR)
    (info.component as? JComponent)?.rootPane?.glassPane?.also {
      it.cursor = if (canDrop) {
        DragSource.DefaultMoveDrop
      } else {
        DragSource.DefaultMoveNoDrop
      }
    }
    return canDrop
  }

  override fun getSourceActions(c: JComponent) = COPY_OR_MOVE

  override fun importData(info: TransferSupport): Boolean {
    val target = info.component as? JTable
    val model = target?.model as? DefaultTableModel ?: return false
    val max = model.rowCount
    var index = if (info.isDrop) {
      (info.dropLocation as? JTable.DropLocation)?.row ?: -1
    } else {
      target.selectedRow
    }
    index = if (index in 0..<max) index else max
    addIndex = index
    val values = runCatching {
      info.transferable.getTransferData(FLAVOR) as? List<*>
    }.getOrNull().orEmpty()
    values.filterIsInstance<List<*>>().forEach {
      val i = index++
      model.insertRow(i, it.toTypedArray())
      target.selectionModel.addSelectionInterval(i, i)
      target.requestFocusInWindow()
    }
    addCount = if (info.isDrop) values.size else 0
    return values.isNotEmpty()
  }

  override fun exportDone(
    c: JComponent,
    data: Transferable?,
    action: Int,
  ) {
    cleanup(c, action == MOVE)
  }

  private fun cleanup(
    c: JComponent,
    remove: Boolean,
  ) {
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

  override fun createTransferable(c: JComponent): Transferable {
    val src = c as? JTree
    source = src
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        val paths = src?.selectionPaths
        return if (isDataFlavorSupported(flavor) && paths != null) {
          val nodes = arrayOfNulls<DefaultMutableTreeNode?>(paths.size)
          paths.indices.forEach {
            nodes[it] = paths[it].lastPathComponent as? DefaultMutableTreeNode
          }
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
    }.getOrNull()?.filterIsInstance<DefaultMutableTreeNode>() ?: return false
    val dl = support.dropLocation as? JTree.DropLocation
    val target = dl?.path
    val parent = target?.lastPathComponent as? DefaultMutableTreeNode
    val tree = support.component as? JTree
    val model = tree?.model as? DefaultTreeModel
    return if (dl != null && model != null && parent != null) {
      val childIndex = dl.childIndex
      val idx = AtomicInteger(if (childIndex < 0) parent.childCount else childIndex)
      nodes.forEach {
        val clone = DefaultMutableTreeNode(it.userObject)
        model.insertNodeInto(deepCopyTreeNode(it, clone), parent, idx.incrementAndGet())
      }
      true
    } else {
      false
    }
  }

  override fun exportDone(
    src: JComponent?,
    data: Transferable?,
    action: Int,
  ) {
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

  private fun deepCopyTreeNode(
    src: MutableTreeNode,
    tgt: DefaultMutableTreeNode,
  ): DefaultMutableTreeNode {
    src
      .children()
      .toList()
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
