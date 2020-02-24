package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val ta = JTextArea("JTextArea")
  ta.isEditable = false

  val field = JTextField()
  val nb = JButton("NORTH")
  val sb = JButton("SOUTH")
  val wb = JButton("WEST")
  val eb = JButton("EAST")

  val p = JPanel(BorderLayout(5, 5))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(nb, BorderLayout.NORTH)
  p.add(sb, BorderLayout.SOUTH)
  p.add(wb, BorderLayout.WEST)
  p.add(eb, BorderLayout.EAST)
  p.add(field)
  EventQueue.invokeLater {
    field.requestFocusInWindow()
    p.rootPane.defaultButton = eb
  }
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(ta))
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
