package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeSelectionModel

class MainPanel : JPanel(GridLayout(1, 2)) {
  init {
    val handler = TreeTransferHandler()
    add(JScrollPane(makeTree(handler)))
    add(JScrollPane(makeTree(handler)))
    preferredSize = Dimension(320, 240)
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
    tree.actionMap.put(TransferHandler.getCutAction().getValue(Action.NAME), object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        /* Dummy action */
      }
    })
    expandTree(tree)
    return tree
  }

  private fun expandTree(tree: JTree) {
    for (i in 0 until tree.rowCount) {
      tree.expandRow(i)
    }
  }
}

class TreeTransferHandler : TransferHandler() {
  private var source: JTree? = null
  override fun createTransferable(c: JComponent): Transferable? {
    source = c as? JTree
    val paths = source?.selectionPaths ?: return null // , "SelectionPaths is null")
    val nodes = arrayOfNulls<DefaultMutableTreeNode>(paths.size)
    for (i in paths.indices) {
      nodes[i] = paths[i].lastPathComponent as DefaultMutableTreeNode
    }
    return object : Transferable {
      override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(FLAVOR)
      }

      override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return FLAVOR == flavor
      }

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

  override fun canImport(support: TransferSupport) =
    support.isDrop && support.isDataFlavorSupported(FLAVOR) && support.component != source

  override fun importData(support: TransferSupport): Boolean {
    @Suppress("UNCHECKED_CAST")
    val nodes = runCatching {
      support.transferable?.getTransferData(FLAVOR) as? Array<DefaultMutableTreeNode>
    }.getOrNull().orEmpty()
    val dl = support.dropLocation
    if (dl is JTree.DropLocation) {
      val childIndex = dl.childIndex
      val dest = dl.path
      val parent = dest.lastPathComponent as DefaultMutableTreeNode
      val tree = support.component as JTree
      val model = tree.model as DefaultTreeModel
      val idx = AtomicInteger(if (childIndex < 0) parent.childCount else childIndex)
      nodes.forEach {
        val clone = DefaultMutableTreeNode(it.userObject)
        model.insertNodeInto(
          deepCopyTreeNode(it, clone),
          parent,
          idx.incrementAndGet()
        )
      }
      return true
    }
    return false
  }

  override fun exportDone(src: JComponent, data: Transferable, action: Int) {
    if (action == MOVE && src is JTree) {
      val model = src.model as DefaultTreeModel
      val selectionPaths = src.selectionPaths
      if (selectionPaths != null) {
        for (path in selectionPaths) {
          model.removeNodeFromParent(path.lastPathComponent as MutableTreeNode)
        }
      }
    }
  }

  companion object {
    private const val NAME = "Array of DefaultMutableTreeNode"
    private val FLAVOR = DataFlavor(Array<DefaultMutableTreeNode>::class.java, NAME)

    private fun deepCopyTreeNode(
      src: DefaultMutableTreeNode,
      tgt: DefaultMutableTreeNode
    ): DefaultMutableTreeNode { // Java 9: Collections.list(src.children()).stream()
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
