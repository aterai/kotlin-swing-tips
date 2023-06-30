package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val toolBar = JToolBar()
  // println(toolBar.isRollover)
  // EventQueue.invokeLater { println(toolBar.isRollover) }

  val tg1 = JToggleButton("Tg1")
  val tg2 = JToggleButton("Tg2")
  val tg3 = JToggleButton("Tg3")
  val button = JButton("Button")
  val radio = JRadioButton("RadioButton")
  val d = Dimension(2, 2)
  val bg = ButtonGroup()
  listOf(tg1, tg2, tg3, button, radio).forEach {
    it.isFocusPainted = false
    toolBar.add(it)
    toolBar.add(Box.createRigidArea(d))
    bg.add(it)
  }

  val check = JCheckBox("setRollover")
  check.addActionListener { e ->
    toolBar.isRollover = (e.source as? AbstractButton)?.isSelected == true
  }
  toolBar.add(Box.createGlue())
  toolBar.add(check)

  val box = Box.createHorizontalBox()
  box.add(JLabel("setRolloverEnabled(false)"))
  listOf(JToggleButton("ToggleButton"), JButton("Button")).forEach {
    it.isRolloverEnabled = false
    box.add(it)
    box.add(Box.createRigidArea(d))
  }

  return JPanel(BorderLayout()).also {
    it.add(toolBar, BorderLayout.NORTH)
    it.add(JScrollPane(JTree()))
    it.add(box, BorderLayout.SOUTH)
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
