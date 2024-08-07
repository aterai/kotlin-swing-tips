package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.tree.TreePath

fun makeUI() = JPanel(GridLayout(1, 2, 2, 2)).also {
  it.add(JScrollPane(JTree()))
  it.add(JScrollPane(RowSelectionTree()))
  it.preferredSize = Dimension(320, 240)
}

private class RowSelectionTree : JTree() {
  override fun paintComponent(g: Graphics) {
    g.color = background
    g.fillRect(0, 0, width, height)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = SELECTED_COLOR
    selectionRows
      ?.map { getRowBounds(it) }
      ?.forEach { g2.fillRect(0, it.y, width, it.height) }
    super.paintComponent(g)
    if (hasFocus()) {
      leadSelectionPath?.also {
        val r = getRowBounds(getRowForPath(it))
        g2.paint = SELECTED_COLOR.darker()
        g2.drawRect(0, r.y, width - 1, r.height - 1)
      }
    }
    g2.dispose()
  }

  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    val tmp = object : BasicTreeUI() {
      override fun getPathBounds(
        tree: JTree,
        path: TreePath?,
      ) = getPathBounds(path, Rectangle())

      private fun getPathBounds(
        path: TreePath?,
        bounds: Rectangle,
      ): Rectangle? {
        val r = treeState?.getBounds(path, bounds)
        if (r != null) {
          r.width = tree.width
          r.y += tree.insets?.top ?: 0
        }
        return r
      }
    }
    setUI(tmp)
    UIManager.put("Tree.repaintWholeRow", true)
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
