package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val def = UIDefaults()
  val tree = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      val selectionBgc = Color(0x39_69_8A)
      val renderer = getCellRenderer()
      setCellRenderer { tree, value, selected, expanded, isLeaf, row, focused ->
        renderer
          .getTreeCellRendererComponent(
            tree,
            value,
            selected,
            expanded,
            isLeaf,
            row,
            focused,
          ).also {
            if (selected) {
              it.background = selectionBgc
            }
            (it as? JComponent)?.isOpaque = selected
          }
      }
    }
  }
  tree.putClientProperty("Nimbus.Overrides", def)
  tree.putClientProperty("Nimbus.Overrides.InheritDefaults", false)
  tree.background = Color.WHITE

  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JScrollPane(JTree()), JScrollPane(tree))
  split.resizeWeight = .5

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
