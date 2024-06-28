package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeSelectionModel

fun makeUI() = JPanel(GridLayout(1, 2)).also {
  val handler = TreeTransferHandler()
  it.add(JScrollPane(makeTree(handler)))
  it.add(JScrollPane(makeTree(handler)))
  it.preferredSize = Dimension(320, 240)
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
          val nodes = arrayOfNulls<DefaultMutableTreeNode>(paths.size)
          for (i in paths.indices) {
            nodes[i] = paths[i].lastPathComponent as? DefaultMutableTreeNode
          }
          nodes
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun getSourceActions(c: JComponent) = MOVE

  override fun canImport(support: TransferSupport) =
    support.isDrop && support.isDataFlavorSupported(FLAVOR) && support.component != source

  override fun importData(support: TransferSupport): Boolean {
    @Suppress("UNCHECKED_CAST")
    val nodes = runCatching {
      support.transferable?.getTransferData(FLAVOR) as? Array<DefaultMutableTreeNode>
    }.getOrNull().orEmpty()
    val dl = support.dropLocation as? JTree.DropLocation
    val target = dl?.path
    val parent = target?.lastPathComponent
    val tree = support.component as? JTree
    val model = tree?.model
    if (dl != null && model is DefaultTreeModel && parent is DefaultMutableTreeNode) {
      val childIndex = dl.childIndex
      val idx = AtomicInteger(if (childIndex < 0) parent.childCount else childIndex)
      nodes.forEach {
        val clone = DefaultMutableTreeNode(it.userObject)
        model.insertNodeInto(
          deepCopyTreeNode(it, clone),
          parent,
          idx.incrementAndGet(),
        )
      }
      return true
    }
    return false
  }

  override fun exportDone(
    src: JComponent,
    data: Transferable,
    action: Int,
  ) {
    val tree = src as? JTree
    val model = tree?.model
    val selectionPaths = tree?.selectionPaths
    if (action == MOVE && model is DefaultTreeModel && selectionPaths != null) {
      for (path in selectionPaths) {
        model.removeNodeFromParent(path.lastPathComponent as? MutableTreeNode)
      }
    }
  }

  private fun deepCopyTreeNode(
    src: MutableTreeNode,
    tgt: DefaultMutableTreeNode,
  ): DefaultMutableTreeNode {
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
