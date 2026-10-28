package example

import java.awt.*
import javax.swing.*

fun createUI(): Component {
  val left = JScrollPane(JTree())
  val right = JScrollPane(JTable(6, 3))
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right).also {
    it.resizeWeight = .4
  }

  val check = JCheckBox("Keep DividerLocation", true)

  val button = JButton("swap")
  button.isFocusable = false
  button.addActionListener { swap(split, check.isSelected) }

  val box = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
    it.add(check)
    it.add(Box.createHorizontalGlue())
    it.add(button)
  }

  return JPanel(BorderLayout()).also {
    it.add(split)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun swap(split: JSplitPane, keepLocation: Boolean) {
  val left = split.leftComponent
  val right = split.rightComponent

  split.remove(left)
  split.remove(right)

  split.leftComponent = right
  split.rightComponent = left

  // Swap the distribution ratio of extra space as well
  split.resizeWeight = 1.0 - split.resizeWeight
  // Re-set the current location explicitly, otherwise the divider location
  // is recalculated after the child components are replaced
  if (keepLocation) {
    split.dividerLocation = split.dividerLocation
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
