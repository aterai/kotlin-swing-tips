package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val bg = ToggleButtonGroup()
    val p = JPanel()
    listOf("A", "B", "C").map(::JToggleButton).forEach {
      it.setActionCommand(it.getText())
      p.add(it)
      bg.add(it)
    }

    val label = JLabel()
    val button = JButton("check")
    button.addActionListener {
      label.setText(bg.getSelection()?.let {
        """"${it.getActionCommand()}" isSelected."""
      } ?: "Please select one of the option above.")
    }

    val box = Box.createHorizontalBox()
    box.add(label)
    box.add(Box.createHorizontalGlue())
    box.add(button, BorderLayout.WEST)
    box.add(Box.createHorizontalStrut(5))

    add(p)
    add(box, BorderLayout.SOUTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }
}

internal class ToggleButtonGroup : ButtonGroup() {
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
    prevModel = getSelection()
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
