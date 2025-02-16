package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val list1 = listOf(
    makeCell(),
    makeCell(),
    makeCell(),
    makeCell(),
    makeCell(),
    makeCell(),
  )
  val list2 = listOf(
    makeCell(),
    makeCell(),
    makeCell(),
    makeCell(),
    makeCell(),
    makeCell(),
  )
  val button = JButton("open JColorChooser")
  button.addActionListener {
    showColorChooser(list1, list2, button.rootPane)
  }
  val p1 = JPanel(GridLayout(0, 1, 1, 1))
  p1.border = BorderFactory.createTitledBorder("Shade")
  for (l in list1) {
    p1.add(l)
  }
  val p2 = JPanel(GridLayout(0, 1, 1, 1))
  p2.border = BorderFactory.createTitledBorder("Tint")
  for (l in list2) {
    p2.add(l)
  }
  makePalette(list1, list2, Color(0x70_AD_47))
  val p = JPanel(GridLayout(1, 2, 2, 2))
  p.add(p1)
  p.add(p2)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(p))
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun showColorChooser(list1: List<JLabel>, list2: List<JLabel>, p: Component) {
  val title = "JColorChooser"
  val color = JColorChooser.showDialog(p, title, list1[0].background)
  if (color != null) {
    makePalette(list1, list2, color)
  }
}

private fun makePalette(list1: List<JLabel>, list2: List<JLabel>, color: Color) {
  shade(list1[0], color, 1f, "")
  shade(list1[1], color, .95f, "Darker 5%")
  shade(list1[2], color, .85f, "Darker 15%")
  shade(list1[3], color, .75f, "Darker 25%")
  shade(list1[4], color, .65f, "Darker 35%")
  shade(list1[5], color, .5f, "Darker 50%")
  tint(list2[0], color, 0f, "")
  tint(list2[1], color, .8f, "Lighter 80%")
  tint(list2[2], color, .6f, "Lighter 80%")
  tint(list2[3], color, .4f, "Lighter 40%")
  tint(list2[4], color, -.25f, "Darker 25%")
  tint(list2[5], color, -.5f, "Darker 50%")
}

private fun tint(l: JLabel, color: Color, tint: Float, txt: String) {
  val c = ColorUtils.getTintColor(color, tint)
  l.background = c
  l.text = "%s #%06X".format(txt, c.rgb and 0xFF_FF_FF)
}

private fun shade(l: JLabel, color: Color, shade: Float, txt: String) {
  val c = ColorUtils.getShadeColor(color, shade)
  l.background = c
  l.text = "%s #%06X".format(txt, c.rgb and 0xFF_FF_FF)
}

private fun makeCell(): JLabel {
  val label = object : JLabel() {
    override fun setBackground(bg: Color) {
      super.setBackground(bg)
      val fg = if (Color.BLACK == bg) Color.WHITE else Color.BLACK
      foreground = fg
      val c = if (Color.WHITE == bg) Color.LIGHT_GRAY else bg
      border = BorderFactory.createLineBorder(c, 1)
    }
  }
  label.isOpaque = true
  return label
}

private object ColorUtils {
  fun getTintColor(color: Color, tint: Float): Color {
    val c: Color
    val positive = tint > 0f
    if (positive) {
      val v = (tint * 255f + .5f).toInt()
      val t = 1f - tint
      val r = (color.red * t).toInt() + v
      val g = (color.green * t).toInt() + v
      val b = (color.blue * t).toInt() + v
      c = Color(r, g, b)
    } else {
      c = getShadeColor(color, 1f + tint)
    }
    return c
  }

  fun getShadeColor(color: Color, shade: Float): Color {
    val r = color.red * shade
    val g = color.green * shade
    val b = color.blue * shade
    return Color(r.toInt(), g.toInt(), b.toInt())
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
