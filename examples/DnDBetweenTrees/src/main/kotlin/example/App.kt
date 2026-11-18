package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

fun createUI() = JPanel(GridLayout(1, 2)).also {
  val handler = TreeTransferHandler()
  it.add(JScrollPane(createTree(handler)))
  it.add(JScrollPane(createTree(handler)))
  it.preferredSize = Dimension(320, 240)
}

private fun createTree(handler: TransferHandler): JTree {
  val tree = JTree()
  tree.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  tree.isRootVisible = false
  tree.dragEnabled = true
  tree.transferHandler = handler
  tree.dropMode = DropMode.INSERT
  tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
  // Disable node Cut action
  val doNothing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      // do nothing action
    }
  }
  tree.actionMap.put(TransferHandler.getCutAction().getValue(Action.NAME), doNothing)
  for (i in 0..<tree.rowCount) {
    tree.expandRow(i)
  }
  return tree
}

private class TreeTransferHandler : TransferHandler() {
  private var source: JTree? = null

  override fun createTransferable(c: JComponent): Transferable {
    val src = c as? JTree
    source = src
    // An empty array is transferred when nothing is selected, and importData(...) rejects it
    val nodes = src
      ?.selectionPaths
      ?.mapNotNull { it.lastPathComponent as? DefaultMutableTreeNode }
      .orEmpty()
      .toTypedArray()
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) {
          throw UnsupportedFlavorException(flavor)
        }
        return nodes
      }
    }
  }

  override fun getSourceActions(c: JComponent) = MOVE

  override fun canImport(support: TransferSupport): Boolean {
    val isSource = support.component == source
    return !isSource && support.isDrop && support.isDataFlavorSupported(FLAVOR)
  }

  override fun importData(support: TransferSupport): Boolean {
    val tree = support.component as? JTree
    val dl = support.dropLocation as? JTree.DropLocation
    // Returning true lets exportDone(...) remove the nodes from the source tree,
    // so report success only when the nodes were actually inserted
    val nodes = getTransferData(support.transferable)
    return tree != null && dl != null && insertNodes(tree, dl, nodes)
  }

  private fun insertNodes(
    tree: JTree,
    dl: JTree.DropLocation,
    nodes: List<DefaultMutableTreeNode>,
  ): Boolean {
    val parent = dl.path?.lastPathComponent as? DefaultMutableTreeNode
    val model = tree.model as? DefaultTreeModel
    if (parent == null || model == null || nodes.isEmpty()) {
      return false
    }
    val childIndex = dl.childIndex
    var index = if (childIndex < 0) parent.childCount else childIndex
    for (node in nodes) {
      // Insert a copy without a parent: inserting the original node would detach it
      // from the source model, and exportDone(...) could no longer remove it
      model.insertNodeInto(deepCopy(node), parent, index)
      index++
    }
    return true
  }

  private fun getTransferData(transferable: Transferable?) =
    runCatching {
      (transferable?.getTransferData(FLAVOR) as? Array<*>)
        ?.filterIsInstance<DefaultMutableTreeNode>()
    }.getOrNull().orEmpty()

  private fun deepCopy(src: DefaultMutableTreeNode): DefaultMutableTreeNode {
    val copy = DefaultMutableTreeNode(src.userObject)
    src
      .children()
      .toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .forEach { copy.add(deepCopy(it)) }
    return copy
  }

  override fun exportDone(
    src: JComponent,
    data: Transferable?,
    action: Int,
  ) {
    val model = (src as? JTree)?.model
    if (action == MOVE && model is DefaultTreeModel) {
      // Remove the transferred nodes rather than the current selection,
      // which may have changed since the drag started
      getTransferData(data)
        .filter { it.parent != null }
        .forEach { model.removeNodeFromParent(it) }
    }
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
