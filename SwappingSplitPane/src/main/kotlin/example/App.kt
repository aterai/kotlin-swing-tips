package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val sp = JSplitPane().also {
    it.leftComponent = JScrollPane(JTree())
    it.rightComponent = JScrollPane(JTable(6, 3))
    it.resizeWeight = .4
  }

  val check = JCheckBox("Keep DividerLocation", true)

  val button = JButton("swap")
  button.isFocusable = false
  button.addActionListener {
    val left = sp.leftComponent
    val right = sp.rightComponent
    val loc = sp.dividerLocation

    sp.remove(left)
    sp.remove(right)

    sp.leftComponent = right
    sp.rightComponent = left

    sp.resizeWeight = 1.0 - sp.resizeWeight
    if (check.isSelected) {
      sp.dividerLocation = loc
    }
  }

  val box = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
    it.add(check)
    it.add(Box.createHorizontalGlue())
    it.add(button)
  }

  return JPanel(BorderLayout()).also {
    it.add(sp)
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
