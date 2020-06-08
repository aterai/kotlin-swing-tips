package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tree = JTree()
  val ml = object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      (e.component as? JTree)?.also { tree ->
        if (tree.getRowForLocation(e.x, e.y) < 0) {
          tree.clearSelection()
        }
      }
    }
  }

  val check = JCheckBox("JTree#clearSelection: when user clicks empty surface")
  check.addActionListener { e ->
    if ((e.source as? JCheckBox)?.isSelected == true) {
      tree.addMouseListener(ml)
    } else {
      tree.removeMouseListener(ml)
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
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
