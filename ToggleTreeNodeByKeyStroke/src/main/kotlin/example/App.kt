package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.*

fun makeUI(): Component {
  val tree = JTree()
  tree.setBorder(BorderFactory.createTitledBorder("Ctrl+T: toggle TreeNode"))
  val im = tree.getInputMap(JComponent.WHEN_FOCUSED)
  val modifiers1 = InputEvent.CTRL_DOWN_MASK
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, modifiers1), "toggle")
  val modifiers2 = InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, modifiers2), "toggle2")
  val act = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val row = tree.getLeadSelectionRow()
      val path = tree.getPathForRow(row)
      if (tree.isExpanded(path)) {
        tree.collapsePath(path)
      } else {
        tree.expandPath(path)
      }
    }
  }
  tree.actionMap.put("toggle2", act)
  return JPanel(BorderLayout(2, 2)).also {
    it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
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
