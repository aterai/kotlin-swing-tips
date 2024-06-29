package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val key = "Tree.paintLines"
  UIManager.put(key, true)
  val tree = JTree()
  for (i in 0..<tree.rowCount) {
    tree.expandRow(i)
  }

  val check = JCheckBox(key)
  check.addActionListener { e ->
    UIManager.put(key, (e.source as? JCheckBox)?.isSelected == true)
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
