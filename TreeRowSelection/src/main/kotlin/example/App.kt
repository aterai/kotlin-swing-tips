package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.tree.TreePath

class MainPanel : JPanel(GridLayout(1, 2, 2, 2)) {
  init {
    add(JScrollPane(JTree()))
    add(JScrollPane(RowSelectionTree()))
    setPreferredSize(Dimension(320, 240))
  }
}

class RowSelectionTree : JTree() {
  override fun paintComponent(g: Graphics) {
    g.setColor(getBackground())
    g.fillRect(0, 0, getWidth(), getHeight())
    val g2 = g.create() as? Graphics2D ?: return
    g2.setPaint(SELECTED_COLOR)
    getSelectionRows()
      ?.map { getRowBounds(it) }
      ?.forEach { g2.fillRect(0, it.y, getWidth(), it.height) }
    super.paintComponent(g)
    if (hasFocus()) {
      getLeadSelectionPath()?.also {
        val r = getRowBounds(getRowForPath(it))
        g2.setPaint(SELECTED_COLOR.darker())
        g2.drawRect(0, r.y, getWidth() - 1, r.height - 1)
      }
    }
    g2.dispose()
  }

  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    setUI(object : BasicTreeUI() {
      override fun getPathBounds(tree: JTree?, path: TreePath?): Rectangle? {
        return if (tree != null && treeState != null) {
          getPathBounds(path, tree.getInsets(), Rectangle())
        } else null
      }

      private fun getPathBounds(path: TreePath?, insets: Insets, bounds: Rectangle) =
        treeState.getBounds(path, bounds)?.also {
          it.width = tree.getWidth()
          it.y += insets.top
        }
    })
    UIManager.put("Tree.repaintWholeRow", true)
    val renderer = getCellRenderer()
    setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
      val c = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
      c.setBackground(if (selected) SELECTED_COLOR else tree.getBackground())
      (c as? JLabel)?.setOpaque(true)
      return@setCellRenderer c
    }
    setOpaque(false)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
