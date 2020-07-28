package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  UIManager.put("Button.disabledToolBarBorderBackground", Color.RED)
  UIManager.put("Button.toolBarBorderBackground", Color.GREEN)

  // TEST: JPanel toolbar = new JPanel();
  val toolbar = JToolBar()

  val tg1 = JToggleButton("Tg1")
  tg1.isEnabled = false
  val tg2 = JToggleButton("Tg2")
  val tg3 = JToggleButton("Tg3")
  tg3.border = BorderFactory.createLineBorder(Color.BLUE)
  val tg4 = JToggleButton("Tg4", true)
  val tg5 = JToggleButton("Tg5")

  val dim = Dimension(5, 5)
  val bg = ButtonGroup()
  listOf(tg1, tg2, tg3, tg4, tg5).forEach {
    it.isFocusPainted = false
    toolbar.add(it)
    toolbar.add(Box.createRigidArea(dim))
    bg.add(it)
  }

  toolbar.add(JButton("Button"))

  return JPanel(BorderLayout()).also {
    it.add(toolbar, BorderLayout.NORTH)
    it.add(JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
  }
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
