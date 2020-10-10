package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tree = JTree()
  tree.componentPopupMenu = TreePopupMenu()

  val button = JButton("Clear node selection")
  button.addActionListener { tree.clearSelection() }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TreePopupMenu : JPopupMenu() {
  init {
    add("path").addActionListener {
      (invoker as? JTree)?.also {
        JOptionPane.showMessageDialog(it, it.selectionPaths, "path", JOptionPane.INFORMATION_MESSAGE)
      }
    }
    add("dummy")
  }

  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTree)?.also {
      val path = it.getPathForLocation(x, y)
      if (it.selectionPaths?.contains(path) == true) {
        super.show(c, x, y)
      }
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
