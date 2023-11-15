package example

import java.awt.*
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.plaf.IconUIResource
import javax.swing.tree.ExpandVetoException

fun makeUI() = JPanel(GridLayout(1, 2, 5, 5)).also {
  it.add(JScrollPane(JTree()))
  it.add(JScrollPane(makeTree()))
  it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  it.preferredSize = Dimension(320, 240)
}

private fun makeTree(): JTree {
  val emptyIcon = EmptyIcon()
  UIManager.put("Tree.expandedIcon", IconUIResource(emptyIcon))
  UIManager.put("Tree.collapsedIcon", IconUIResource(emptyIcon))

  val tree = JTree()
  tree.isEditable = true
  tree.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
  var row = 0
  while (row < tree.rowCount) {
    tree.expandRow(row++)
  }
  val handler = object : TreeWillExpandListener {
    // @Throws(ExpandVetoException::class)
    override fun treeWillExpand(e: TreeExpansionEvent) {
      // throw ExpandVetoException(e, "Tree expansion cancelled")
    }

    @Throws(ExpandVetoException::class)
    override fun treeWillCollapse(e: TreeExpansionEvent) {
      throw ExpandVetoException(e, "Tree collapse cancelled")
    }
  }
  tree.addTreeWillExpandListener(handler)
  return tree
}

private class EmptyIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    // Empty icon
  }

  override fun getIconWidth() = 0

  override fun getIconHeight() = 0
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
