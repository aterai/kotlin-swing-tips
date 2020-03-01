package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val label = JLabel("", SwingConstants.CENTER)
  label.addComponentListener(object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
      label.text = label.size.toString()
    }
  })
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
