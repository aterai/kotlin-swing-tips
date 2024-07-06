package example

import java.awt.*
import javax.swing.*

private val joinCombo = JComboBox(JoinStyle.entries.toTypedArray())
private val endcapCombo = JComboBox(EndCapStyle.entries.toTypedArray())
private val field = JTextField("10, 20")
private val label = JLabel()
private val button = JButton("Change")
private val DEFAULT_DASH_ARRAY = floatArrayOf(1f)

fun makeUI(): Component {
  button.addActionListener {
    val ecs = endcapCombo.getItemAt(endcapCombo.selectedIndex).style
    val js = joinCombo.getItemAt(joinCombo.selectedIndex).style
    val dashedStroke = BasicStroke(5f, ecs, js, 5f, getDashArray(), 0f)
    label.border = BorderFactory.createStrokeBorder(dashedStroke, Color.RED)
  }

  val p = JPanel(BorderLayout(2, 2))
  p.add(field)
  p.add(button, BorderLayout.EAST)
  p.border = BorderFactory.createTitledBorder("Comma Separated Values")

  val p1 = JPanel(GridLayout(2, 1))
  p1.add(endcapCombo)
  p1.add(joinCombo)
  p.add(p1, BorderLayout.NORTH)

  val p2 = JPanel(BorderLayout())
  p2.add(label)
  p2.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(p2)
    it.preferredSize = Dimension(320, 240)
    button.doClick()
    EventQueue.invokeLater { it.rootPane.defaultButton = button }
  }
}

private fun getDashArray(): FloatArray {
  val l = field.text
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }
  if (l.isEmpty()) {
    return DEFAULT_DASH_ARRAY
  }
  val list = FloatArray(l.size)
  var i = 0
  runCatching {
    for (s in l) {
      list[i++] = s.toFloat()
    }
  }.getOrDefault(DEFAULT_DASH_ARRAY)
  return if (i == 0) DEFAULT_DASH_ARRAY else list
}

private enum class JoinStyle(val style: Int) {
  JOIN_BEVEL(BasicStroke.JOIN_BEVEL),
  JOIN_MITER(BasicStroke.JOIN_MITER),
  JOIN_ROUND(BasicStroke.JOIN_ROUND),
}

private enum class EndCapStyle(val style: Int) {
  CAP_BUTT(BasicStroke.CAP_BUTT),
  CAP_ROUND(BasicStroke.CAP_ROUND),
  CAP_SQUARE(BasicStroke.CAP_SQUARE),
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
