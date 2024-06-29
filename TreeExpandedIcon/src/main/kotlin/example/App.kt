package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.IconUIResource

fun makeUI(): Component {
  val expandedKey = "Tree.expandedIcon"
  val collapsedKey = "Tree.collapsedIcon"
  val emptyIcon = EmptyIcon()
  UIManager.put(expandedKey, IconUIResource(emptyIcon))
  UIManager.put(collapsedKey, IconUIResource(emptyIcon))
  val tree = JTree()
  for (i in 0..<tree.rowCount) {
    tree.expandRow(i)
  }

  val check = JCheckBox("JTree: paint expanded, collapsed Icon")
  check.addActionListener { e ->
    if ((e.source as? JCheckBox)?.isSelected == true) {
      val def = UIManager.getLookAndFeelDefaults()
      UIManager.put(expandedKey, IconUIResource(def.getIcon(expandedKey)))
      UIManager.put(collapsedKey, IconUIResource(def.getIcon(collapsedKey)))
    } else {
      UIManager.put(expandedKey, IconUIResource(emptyIcon))
      UIManager.put(collapsedKey, IconUIResource(emptyIcon))
    }
    SwingUtilities.updateComponentTreeUI(tree)
  }

  val p = JPanel(GridLayout(1, 2))
  p.add(JScrollPane(tree))
  p.add(JScrollPane(JTree()))

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private class EmptyIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    // Empty icon
  }

  override fun getIconWidth() = 0

  override fun getIconHeight() = 0
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
