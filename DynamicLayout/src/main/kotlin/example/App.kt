package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

fun makeUI(): Component {
  val label = JLabel("", SwingConstants.CENTER)
  val cmpListener = object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
      label.text = label.size.toString()
    }
  }
  label.addComponentListener(cmpListener)
  Toolkit.getDefaultToolkit().setDynamicLayout(true)

  val check = JCheckBox("DynamicLayout", true)
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    Toolkit.getDefaultToolkit().setDynamicLayout(b)
  }

  return JPanel(BorderLayout()).also {
    it.add(label)
    it.add(check, BorderLayout.NORTH)
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
