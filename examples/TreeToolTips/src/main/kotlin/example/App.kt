package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
  val tree1 = object : JTree() {
    override fun getToolTipText(e: MouseEvent) =
      getPathForLocation(e.x, e.y)?.lastPathComponent?.let { "getToolTipText: $it" }
  }
  ToolTipManager.sharedInstance().registerComponent(tree1)

  val tree2 = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
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
          )?.also {
            (it as? JComponent)?.toolTipText = value?.let { v -> "TreeCellRenderer: $v" }
          }
      }
    }
  }
  ToolTipManager.sharedInstance().registerComponent(tree2)

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("Override getToolTipText", JScrollPane(tree1)))
    it.add(makeTitledPanel("Use TreeCellRenderer", JScrollPane(tree2)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
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
