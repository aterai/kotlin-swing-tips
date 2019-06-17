package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val gl = GridLayout(5, 7, 5, 5)
    val p = JPanel(gl)
    p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

    val log = JTextArea(3, 0)
    val al = ActionListener { e ->
      val c = e.getSource() as Component
      val idx = p.getComponentZOrder(c)
      val row = idx / gl.getColumns()
      val col = idx % gl.getColumns()
      log.append(String.format("Row: %d, Column: %d%n", row + 1, col + 1))
    }
    for (i in 0 until gl.getRows() * gl.getColumns()) {
      val b = JButton()
      b.addActionListener(al)
      p.add(b)
    }

    add(p)
    add(JScrollPane(log), BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
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
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
