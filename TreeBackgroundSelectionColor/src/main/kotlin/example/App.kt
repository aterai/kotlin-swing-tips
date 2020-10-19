package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.regex.Pattern
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

fun makeUI(): Component {
  val tree = object : JTree(makeModel()) {
    override fun updateUI() {
      setCellRenderer(null)
      setCellEditor(null)
      super.updateUI()
      setCellRenderer(SelectionColorTreeCellRenderer())
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): DefaultTreeModel {
  val set1 = DefaultMutableTreeNode(Color.ORANGE).also {
    it.add(DefaultMutableTreeNode(Color.RED))
    it.add(DefaultMutableTreeNode(Color.GREEN))
    it.add(DefaultMutableTreeNode(Color.BLUE))
  }
  val set2 = DefaultMutableTreeNode("Set 002").also {
    it.add(DefaultMutableTreeNode("aaa 111111111"))
    it.add(DefaultMutableTreeNode("aa 2222"))
  }
  val set3 = DefaultMutableTreeNode("Set 003").also {
    it.add(DefaultMutableTreeNode("Abc 3333333333333"))
    it.add(DefaultMutableTreeNode("44444444"))
    it.add(DefaultMutableTreeNode("55555555555555555"))
  }
  val root = DefaultMutableTreeNode("Root").also {
    it.add(set1)
    it.add(set2)
  }
  set2.add(set3)
  return DefaultTreeModel(root)
}

private class SelectionColorTreeCellRenderer : DefaultTreeCellRenderer() {
  private val pattern = Pattern.compile("^a.*", Pattern.CASE_INSENSITIVE)
  private var color: Color? = null
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus).also {
    if (selected) {
      setParticularCondition(value)
      it.foreground = getTextSelectionColor()
      it.background = getBackgroundSelectionColor()
      val str = value?.toString() ?: ""
      if (leaf && pattern.matcher(str).matches()) {
        (it as? JComponent)?.isOpaque = true
        it.background = Color.RED
      } else {
        (it as? JComponent)?.isOpaque = false
        it.background = getBackgroundSelectionColor()
      }
    } else {
      it.foreground = getTextNonSelectionColor()
      it.background = getBackgroundNonSelectionColor()
    }
  }

  private fun setParticularCondition(value: Any?) {
    if (value is DefaultMutableTreeNode) {
      val uo = value.userObject
      if (uo is Color) {
        color = uo
        return
      }
    }
    color = null
  }

  override fun getBackgroundSelectionColor(): Color? = color ?: super.getBackgroundSelectionColor()
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
