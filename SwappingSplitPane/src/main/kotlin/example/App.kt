package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val s1 = JScrollPane(JTree())
  val s2 = JScrollPane(JTable(6, 3))
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, s1, s2).also {
    it.resizeWeight = .4
  }

  val check = JCheckBox("Keep DividerLocation", true)

  val button = JButton("swap")
  button.isFocusable = false
  button.addActionListener {
    val left = split.leftComponent
    val right = split.rightComponent
    val loc = split.dividerLocation

    split.remove(left)
    split.remove(right)

    split.leftComponent = right
    split.rightComponent = left

    split.resizeWeight = 1.0 - split.resizeWeight
    if (check.isSelected) {
      split.dividerLocation = loc
    }
  }

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
