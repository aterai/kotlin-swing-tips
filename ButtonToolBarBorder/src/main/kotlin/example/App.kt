package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    UIManager.put("Button.disabledToolBarBorderBackground", Color.RED)
    UIManager.put("Button.toolBarBorderBackground", Color.GREEN)

    // TEST: JPanel toolbar = new JPanel();
    val toolbar = JToolBar()

    val tg1 = JToggleButton("Tg1")
    tg1.setEnabled(false)
    val tg2 = JToggleButton("Tg2")
    val tg3 = JToggleButton("Tg3")
    tg3.setBorder(BorderFactory.createLineBorder(Color.BLUE))
    val tg4 = JToggleButton("Tg4", true)
    val tg5 = JToggleButton("Tg5")

    val dim = Dimension(5, 5)
    val bg = ButtonGroup()
    listOf(tg1, tg2, tg3, tg4, tg5).forEach {
      it.setFocusPainted(false)
      toolbar.add(it)
      toolbar.add(Box.createRigidArea(dim))
      bg.add(it)
    }

    toolbar.add(JButton("Button"))

    add(toolbar, BorderLayout.NORTH)
    add(JScrollPane(JTree()))
    setPreferredSize(Dimension(320, 240))
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
