package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val key = "JTree.lineStyle"

  val tree0 = JTree()
  tree0.putClientProperty(key, "Angled")

  val tree1 = JTree()
  tree1.putClientProperty(key, "Horizontal")

  val tree2 = JTree()
  tree2.putClientProperty(key, "None")

  return JPanel(GridLayout(1, 3)).also {
    it.add(makeTitledPanel("Angled(default)", JScrollPane(tree0)))
    it.add(makeTitledPanel("Horizontal", JScrollPane(tree1)))
    it.add(makeTitledPanel("None", JScrollPane(tree2)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
