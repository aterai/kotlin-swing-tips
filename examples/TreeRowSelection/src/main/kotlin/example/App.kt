package example

import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.tree.TreePath

fun createUI() = JPanel(GridLayout(1, 2, 2, 2)).also {
  it.add(JScrollPane(JTree()))
  it.add(JScrollPane(RowSelectionTree()))
  it.preferredSize = Dimension(320, 240)
}

private class RowSelectionTree : JTree() {
  override fun paintComponent(g: Graphics) {
    // The tree is not opaque, so the background must always be filled here,
    // even when there is no selection.
    g.color = background
    g.fillRect(0, 0, width, height)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = SELECTED_COLOR
    selectionRows
      ?.asList()
      ?.mapNotNull { getRowBounds(it) }
      ?.forEach { g2.fillRect(0, it.y, width, it.height) }
    super.paintComponent(g)
    if (hasFocus()) {
      leadSelectionPath?.let { getPathBounds(it) }?.also { r ->
        g2.paint = SELECTED_COLOR.darker()
        g2.drawRect(0, r.y, width - 1, r.height - 1)
      }
    }
    g2.dispose()
  }

  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    setUI(RowSelectionTreeUI())
    val renderer = getCellRenderer()
    setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
      renderer
        .getTreeCellRendererComponent(
          tree,
          value,
          selected,
          expanded,
          leaf,
          row,
          hasFocus,
        ).also {
          it.background = if (selected) SELECTED_COLOR else tree.background
          (it as? JComponent)?.isOpaque = true
        }
    }
    isOpaque = false
  }

  companion object {
    private val SELECTED_COLOR = Color(0x64_96_C8)
  }
}

private class RowSelectionTreeUI : BasicTreeUI() {
  // Extend the path bounds to the right edge of the tree so that the node
  // can be selected by clicking anywhere to the right of the node.
  override fun getPathBounds(
    tree: JTree?,
    path: TreePath?,
  ): Rectangle? {
    if (tree == null) {
      return null
    }
    return treeState?.getBounds(path, Rectangle())?.also {
      it.width = tree.width
      it.y += tree.insets.top
    }
  }

  // The BasicTreeUI repaints only the area from the node x position when the
  // focus or the lead selection changes, which would leave the left part of
  // the lead row border. Repaint the whole row instead of using the global
  // UIManager.put("Tree.repaintWholeRow", true) setting.
  override fun createFocusListener(): FocusListener {
    val listener = super.createFocusListener()
    return object : FocusListener {
      override fun focusGained(e: FocusEvent) {
        listener.focusGained(e)
        repaintWholeRow(tree.leadSelectionPath)
      }

      override fun focusLost(e: FocusEvent) {
        listener.focusLost(e)
        repaintWholeRow(tree.leadSelectionPath)
      }
    }
  }

  override fun createPropertyChangeListener(): PropertyChangeListener {
    val listener = super.createPropertyChangeListener()
    return PropertyChangeListener { e ->
      listener.propertyChange(e)
      if (JTree.LEAD_SELECTION_PATH_PROPERTY == e.propertyName) {
        repaintWholeRow(e.oldValue as? TreePath)
        repaintWholeRow(e.newValue as? TreePath)
      }
    }
  }

  private fun repaintWholeRow(path: TreePath?) {
    path?.let { getPathBounds(tree, it) }?.also {
      tree.repaint(0, it.y, tree.width, it.height)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
