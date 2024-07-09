package example

import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.*

fun makeUI(): Component {
  val field1 = JTextField("01234567890")
  field1.addFocusListener(BackgroundFocusListener(Color(0xE6_E6_FF)))

  val field2 = JTextField()
  field2.addFocusListener(BackgroundFocusListener(Color(0xFF_FF_E6)))

  val field3 = JTextField("123465789735")
  field3.addFocusListener(BackgroundFocusListener(Color(0xFF_E6_E6)))

  val box = Box.createVerticalBox().also {
    it.add(makeTitledPanel("Color(230, 230, 255)", field1))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("Color(255, 255, 230)", field2))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("Color(255, 230, 230)", field3))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private class BackgroundFocusListener(
  private val color: Color,
) : FocusListener {
  override fun focusGained(e: FocusEvent) {
    e.component.background = color
  }

  override fun focusLost(e: FocusEvent) {
    e.component.background = UIManager.getColor("TextField.background")
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
