package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val field = JTextField("1f, 1f, 5f, 1f")
  var dashedStroke = makeStroke(field)
  val label = object : JLabel() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val i = insets
      val w = width
      val h = height / 2
      val g2 = g.create() as? Graphics2D ?: return
      g2.stroke = dashedStroke
      g2.drawLine(i.left, h, w - i.right, h)
      g2.dispose()
    }
  }
  label.border = BorderFactory.createEmptyBorder(0, 15, 0, 15)

  val button = JButton("Change")
  button.addActionListener {
    dashedStroke = makeStroke(field)
    label.repaint()
  }
  EventQueue.invokeLater { button.rootPane.defaultButton = button }

  val p = JPanel(BorderLayout(5, 5))
  p.border = BorderFactory.createTitledBorder("Comma Separated Values")
  p.add(field)
  p.add(button, BorderLayout.EAST)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(label)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getDashArray(field: JTextField): FloatArray {
  val strArray = tokenize(field.text.trim())
  val dist = FloatArray(strArray.size)
  var i = 0
  runCatching {
    for (s in strArray) {
      val ss = s.trim()
      if (ss.isNotEmpty()) {
        dist[i++] = ss.toFloat()
      }
    }
  }.onFailure {
    EventQueue.invokeLater {
      UIManager.getLookAndFeel().provideErrorFeedback(field)
      val msg = "Invalid input. ${it.message}"
      JOptionPane.showMessageDialog(
        field.rootPane,
        msg,
        "Error",
        JOptionPane.ERROR_MESSAGE,
      )
    }
    i = 0
  }
  return if (i == 0) floatArrayOf(1f) else dist
}

private fun makeStroke(field: JTextField): BasicStroke {
  val dist = getDashArray(field)
  return BasicStroke(5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dist, 0f)
}

private fun tokenize(text: String) = text
  .split(",")
  .map { it.trim() }
  .filter { it.isNotEmpty() }
  .toTypedArray()

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
