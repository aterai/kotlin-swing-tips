package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val button0 = object : JButton("ToolTip1") {
    override fun createToolTip() = JToolTip().also {
      val b = BorderFactory.createTitledBorder("TitledBorder ToolTip")
      it.border = BorderFactory.createCompoundBorder(it.border, b)
      it.component = this
    }
  }
  button0.toolTipText = "Test - ToolTipText0"

  val button1 = object : JButton("ToolTip2") {
    override fun createToolTip() = JToolTip().also {
      val b = BorderFactory.createMatteBorder(0, 10, 0, 0, Color.GREEN)
      it.border = BorderFactory.createCompoundBorder(it.border, b)
      it.component = this
    }
  }
  button1.toolTipText = "Test - ToolTipText1"

  val p = JPanel(BorderLayout(5, 5))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(makeTitledPanel("TitledBorder", button0), BorderLayout.NORTH)
  p.add(makeTitledPanel("MatteBorder", button1), BorderLayout.SOUTH)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
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
