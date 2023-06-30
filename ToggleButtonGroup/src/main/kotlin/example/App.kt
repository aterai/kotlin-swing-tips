package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Default ButtonGroup", ButtonGroup()))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Custom ButtonGroup(clears the selection)", ToggleButtonGroup()))
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, bg: ButtonGroup): Component {
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder(title)
  listOf("aaa", "bbb", "ccc")
    .map { JToggleButton(it) }
    .forEach {
      p.add(it)
      bg.add(it)
    }
  return p
}

private class ToggleButtonGroup : ButtonGroup() {
  private var prevModel: ButtonModel? = null
  private var isAdjusting = false
  override fun setSelected(m: ButtonModel, b: Boolean) {
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
      // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
