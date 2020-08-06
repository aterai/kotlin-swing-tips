package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val gl = GridLayout(5, 7, 5, 5)
  val p = JPanel(gl)
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val log = JTextArea(3, 0)
  val al = ActionListener { e ->
    val idx = p.getComponentZOrder(e.source as? Component)
    val row = idx / gl.columns
    val col = idx % gl.columns
    log.append("Row: ${row + 1}, Column: ${col + 1}\n")
  }
  List(gl.rows * gl.columns) { JButton() }
    .forEach { p.add(it.also { it.addActionListener(al) }) }

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(JScrollPane(log), BorderLayout.SOUTH)
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
