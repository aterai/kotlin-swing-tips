package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.InsetsUIResource

private val log = JTextArea()

fun makeUI(): Component {
  val field1 = JTextField("1111111111")
  val m = field1.margin
  log.append("$m\n")
  val margin = Insets(m.top, m.left + 10, m.bottom, m.right)
  field1.margin = margin

  val field2 = JTextField("2222222222222")
  val b1 = BorderFactory.createEmptyBorder(0, 20, 0, 0)
  val b2 = BorderFactory.createCompoundBorder(field2.border, b1)
  field2.border = b2

  val box = Box.createVerticalBox()
  box.add(makePanel(JTextField("000000000000000000")))
  box.add(Box.createVerticalStrut(5))
  box.add(makePanel(field1))
  box.add(Box.createVerticalStrut(5))
  box.add(makePanel(field2))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getLeftMargin(c: JTextField): Int {
  log.append("----\n")
  val l1 = c.margin.left
  log.append("margin.left: $l1\n")
  val l2 = c.insets.left
  log.append("insets.left: $l2\n")
  val l3 = c.border.getBorderInsets(c).left
  log.append("border.getBorderInsets(c).left: $l3\n")
  return l2
}

private fun makePanel(field: JTextField): Component {
  val p = JPanel(BorderLayout())
  val title = "left margin = " + getLeftMargin(field)
  p.border = BorderFactory.createTitledBorder(title)
  p.add(field)
  return p
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      val key = "TextField.margin"
      val m = UIManager.getInsets(key)
      val ins = InsetsUIResource(m.top, m.left + 5, m.bottom, m.right)
      UIManager.put(key, ins)
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
