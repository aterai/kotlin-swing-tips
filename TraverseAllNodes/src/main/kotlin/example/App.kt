package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode

fun makeUI(): Component {
  val tree = JTree()
  val textArea = JTextArea()
  val root = tree.model.root as DefaultMutableTreeNode

  val depthFirst = JButton("<html>depthFirst<br>postorder")
  depthFirst.addActionListener {
    textArea.text = ""
    root.depthFirstEnumeration()
      .toList().forEach { textArea.append("$it\n") }
  }

  val breadthFirst = JButton("breadthFirst")
  breadthFirst.addActionListener {
    textArea.text = ""
    root.breadthFirstEnumeration()
      .toList().forEach { textArea.append("$it\n") }
  }

  val preorder = JButton("preorder")
  preorder.addActionListener {
    textArea.text = ""
    root.preorderEnumeration()
      .toList().forEach { textArea.append("$it\n") }
  }

  val p = JPanel(GridLayout(0, 1, 5, 5))
  p.add(depthFirst)
  p.add(breadthFirst)
  p.add(preorder)

  val panel = JPanel(BorderLayout())
  panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  panel.add(p, BorderLayout.NORTH)

  val sp = JSplitPane()
  sp.leftComponent = JScrollPane(tree)
  sp.rightComponent = JScrollPane(textArea)

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.add(panel, BorderLayout.EAST)
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
