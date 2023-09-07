package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val bg = ToggleButtonGroup()
  val p = JPanel()
  listOf("A", "B", "C").map(::JToggleButton).forEach {
    it.actionCommand = it.text
    p.add(it)
    bg.add(it)
  }

  val label = JLabel()
  val button = JButton("check")
  button.addActionListener {
    label.text = bg.selection?.let {
      """"${it.actionCommand}" isSelected."""
    } ?: "Please select one of the option above."
  }

  val box = Box.createHorizontalBox()
  box.add(label)
  box.add(Box.createHorizontalGlue())
  box.add(button, BorderLayout.WEST)
  box.add(Box.createHorizontalStrut(5))

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ToggleButtonGroup : ButtonGroup() {
  private var prevModel: ButtonModel? = null
  private var isAdjusting = false

  override fun setSelected(m: ButtonModel?, b: Boolean) {
    if (isAdjusting) {
      return
    }
    if (m == prevModel) {
      isAdjusting = true
      clearSelection()
      isAdjusting = false
    } else {
      super.setSelected(m, b)
    }
    prevModel = selection
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
