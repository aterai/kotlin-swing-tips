package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val gl = GridLayout(5, 7, 5, 5)
  val p = JPanel(gl)
  p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

  val log = JTextArea(3, 0)
  val al = ActionListener { e ->
    val idx = p.getComponentZOrder(e.getSource() as? Component)
    val row = idx / gl.getColumns()
    val col = idx % gl.getColumns()
    log.append(String.format("Row: %d, Column: %d%n", row + 1, col + 1))
  }
  List(gl.getRows() * gl.getColumns()) { JButton() }
    .forEach { p.add(it.also { it.addActionListener(al) }) }

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(JScrollPane(log), BorderLayout.SOUTH)
    it.setPreferredSize(Dimension(320, 240))
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
