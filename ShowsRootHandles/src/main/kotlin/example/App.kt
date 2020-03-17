package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tree1 = JTree()
  tree1.showsRootHandles = true

  val tree2 = JTree()
  tree2.showsRootHandles = false

  val check = JCheckBox("setRootVisible", true)
  check.addActionListener { e ->
    val flg = (e.source as? JCheckBox)?.isSelected == true
    tree1.isRootVisible = flg
    tree2.isRootVisible = flg
  }

  val p = JPanel(GridLayout(1, 2))
  p.add(makeTitledPanel("setShowsRootHandles(true)", tree1))
  p.add(makeTitledPanel("setShowsRootHandles(false)", tree2))

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, tree: JTree): Component {
  tree.border = BorderFactory.createEmptyBorder(2, 4, 2, 2)
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(JScrollPane(tree))
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
