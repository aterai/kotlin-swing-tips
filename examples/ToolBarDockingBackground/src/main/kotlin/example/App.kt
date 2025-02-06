package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicToolBarUI

fun makeUI(): Component {
  UIManager.put("ToolBar.dockingBackground", Color(0x64_FF_00_00, true))
  UIManager.put("ToolBar.floatingBackground", Color(0x64_00_00_FF, true))
  UIManager.put("ToolBar.dockingForeground", Color.BLUE)
  UIManager.put("ToolBar.floatingForeground", Color.RED)

  val check = JCheckBox("translucent", true)
  val toolBar = JToolBar()
  toolBar.add(check)
  toolBar.add(Box.createRigidArea(Dimension(5, 5)))
  toolBar.add(JButton("Button"))
  toolBar.add(Box.createRigidArea(Dimension(5, 5)))
  toolBar.add(JRadioButton("RadioButton"))
  toolBar.add(Box.createGlue())

  check.addActionListener { e ->
    (toolBar.ui as? BasicToolBarUI)?.also {
      if ((e.source as? JCheckBox)?.isSelected == true) {
        it.dockingColor = Color(0x64_FF_00_00, true)
        it.floatingColor = Color(0x64_00_00_FF, true)
      } else {
        it.dockingColor = Color.RED
        it.floatingColor = Color.BLUE
      }
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(toolBar, BorderLayout.NORTH)
    it.add(JScrollPane(JTree()))
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
