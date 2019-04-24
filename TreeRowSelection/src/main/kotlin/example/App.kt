package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreePath

class MainPanel : JPanel(BorderLayout()) {
  init {
    add(JPanel(GridLayout(1, 2)).also {
      it.add(JScrollPane(JTree()))
      it.add(JScrollPane(RowSelectionTree()))
    })
    setPreferredSize(Dimension(320, 240))
  }
}

internal class RowSelectionTree : JTree() {
  protected override fun paintComponent(g: Graphics) {
    g.setColor(getBackground())
    g.fillRect(0, 0, getWidth(), getHeight())
    val g2 = g.create() as Graphics2D
    g2.setPaint(SELC)
    getSelectionRows()?.forEach {
      val r = getRowBounds(it)
      g2.fillRect(0, r.y, getWidth(), r.height)
    }
    super.paintComponent(g)
    if (hasFocus()) {
      getLeadSelectionPath()?.also {
        val r = getRowBounds(getRowForPath(it))
        g2.setPaint(SELC.darker())
        g2.drawRect(0, r.y, getWidth() - 1, r.height - 1)
      }
    }
    g2.dispose()
  }

  override fun updateUI() {
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
    setCellRenderer(Handler())
    setOpaque(false)
  }

  private class Handler : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
      tree: JTree,
      value: Any?,
      selected: Boolean,
      expanded: Boolean,
      leaf: Boolean,
      row: Int,
      hasFocus: Boolean
    ): Component {
      val c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
      val l = c as? JLabel ?: return c
      l.setBackground(if (selected) SELC else tree.getBackground())
      l.setOpaque(true)
      return l
    }
  }

  companion object {
    val SELC = Color(0x64_96_C8)
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
