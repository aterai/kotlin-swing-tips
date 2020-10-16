package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val def = UIDefaults()
  val tree = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      val selectionBackground = Color(0x39_69_8A)
      val renderer = getCellRenderer()
      setCellRenderer { tree, value, selected, expanded, isLeaf, row, focused ->
        renderer.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused).also {
          if (selected) {
            it.background = selectionBackground
          }
          (it as? JComponent)?.isOpaque = selected
        }
      }
    }
  }
  tree.putClientProperty("Nimbus.Overrides", def)
  tree.putClientProperty("Nimbus.Overrides.InheritDefaults", false)
  tree.background = Color.WHITE

  val split = JSplitPane()
  split.resizeWeight = .5
  split.leftComponent = JScrollPane(JTree())
  split.rightComponent = JScrollPane(tree)

  return JPanel(BorderLayout()).also {
    it.add(split)
    it.preferredSize = Dimension(320, 240)
  }
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
