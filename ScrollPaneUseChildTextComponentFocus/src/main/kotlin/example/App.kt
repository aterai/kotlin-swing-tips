package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val LF = "\n"

fun makeUI(): Component {
  val buf = StringBuilder()
  for (i in 0..100) {
    buf.append(i).append(LF)
  }
  val str = buf.toString()
  val scroll = JScrollPane(JTextArea(str))

  val key = "ScrollPane.useChildTextComponentFocus"
  val check = JCheckBox(key, UIManager.getBoolean(key))
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    UIManager.put(key, b)
    SwingUtilities.updateComponentTreeUI(scroll)
  }

  val p = JPanel(GridLayout(1, 2))
  p.add(JScrollPane(JTextArea(str)))
  p.add(scroll)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(check, BorderLayout.NORTH)
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
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
