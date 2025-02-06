package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

private fun makeComboBoxModel(
  model: DefaultComboBoxModel<TreeNode>,
  node: TreeNode,
) {
  if (node is DefaultMutableTreeNode && !node.isRoot) {
    model.addElement(node)
  }
  if (!node.isLeaf) {
    node
      .children()
      .toList()
      .filterIsInstance<TreeNode>()
      .forEach { makeComboBoxModel(model, it) }
  }
}

private fun makeModel() = JTree().model

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

fun makeUI(): Component {
  val model1 = DefaultComboBoxModel<TreeNode>()
  val model2 = DefaultComboBoxModel<TreeNode>()
  (makeModel().root as? DefaultMutableTreeNode)?.also { root ->
    makeComboBoxModel(model1, root)
    makeComboBoxModel(model2, root)
  }

  val combo = TreeComboBox<TreeNode>()
  combo.model = model2
  combo.selectedIndex = -1

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("default:", JComboBox(model1)))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Tree ComboBoxModel:", combo))
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TreeComboBox<E : TreeNode> : JComboBox<E>() {
  private var notSelectable = false
  private val up = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val si = selectedIndex
      for (i in si - 1 downTo 0) {
        if (getItemAt(i)?.isLeaf == true) {
          selectedIndex = i
          break
        }
      }
    }
  }
  private val down = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val si = selectedIndex
      for (i in si + 1..<itemCount) {
        if (getItemAt(i)?.isLeaf == true) {
          selectedIndex = i
          break
        }
      }
    }
  }

  override fun updateUI() {
    super.updateUI()
    val r = getRenderer()
    setRenderer { list, value, index, isSelected, cellHasFocus ->
      r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
        if (value != null && !value.isLeaf) {
          it.foreground = Color.WHITE
          it.background = Color.GRAY.darker()
        }
        val indent = if (index >= 0 && value is DefaultMutableTreeNode) {
          0.coerceAtLeast(value.level - 1) * 16
        } else {
          0
        }
        (it as? JComponent)?.border = BorderFactory.createEmptyBorder(1, indent + 1, 1, 1)
      }
    }
    EventQueue.invokeLater {
      val prevKey = "selectPrevious3"
      val nextKey = "selectNext3"
      val am = actionMap
      am.put(prevKey, up)
      am.put(nextKey, down)
      val im = inputMap
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), prevKey)
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0), prevKey)
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), nextKey)
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0), nextKey)
    }
  }

  override fun setPopupVisible(v: Boolean) {
    if (!v && notSelectable) {
      notSelectable = false
    } else {
      super.setPopupVisible(v)
    }
  }

  override fun setSelectedIndex(index: Int) {
    val node = getItemAt(index)
    if (node != null && node.isLeaf) {
      super.setSelectedIndex(index)
    } else {
      notSelectable = true
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
