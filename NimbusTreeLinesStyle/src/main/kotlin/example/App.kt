package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val d = UIDefaults()
  d["Tree.drawVerticalLines"] = true
  d["Tree.drawHorizontalLines"] = true
  d["Tree.linesStyle"] = "dashed"

  val tree = JTree()
  tree.putClientProperty("Nimbus.Overrides", d)

  return JPanel(GridLayout()).also {
    it.add(makeTitledPanel("Default", JScrollPane(JTree())))
    it.add(makeTitledPanel("linesStyle: dashed", JScrollPane(tree)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
