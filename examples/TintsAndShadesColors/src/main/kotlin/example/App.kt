package example

import java.awt.*
import javax.swing.*
import kotlin.math.abs
import kotlin.math.roundToInt

fun makeUI(): Component {
  val list1 = makeCellList()
  val list2 = makeCellList()
  val list3 = makeCellList()
  val button = JButton("open JColorChooser")
  button.addActionListener {
    val bgc = list1[0].background
    val color = JColorChooser.showDialog(button.rootPane, "title", bgc)
    if (color != null) {
      makePalette1(list1, color)
      makePalette2(list2, color)
      makePalette3(list3, color)
    }
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
  val p3 = JPanel(GridLayout(0, 1, 1, 1)).also {
    it.border = BorderFactory.createTitledBorder("lumMod/lumOff")
    for (l in list3) {
      it.add(l)
    }
  }
  val color = Color(0x70_AD_47)
  makePalette1(list1, color)
  makePalette2(list2, color)
  makePalette3(list3, color)

  val p = JPanel(GridLayout(1, 0, 2, 2))
  p.add(p1)
  p.add(p2)
  p.add(p3)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(p))
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

const val DARKER = "Darker %d%"
const val LIGHTER = "Lighter %d%"

private fun makePalette1(list: List<JLabel>, color: Color) {
  shade(list[0], color, 1f, "")
  shade(list[1], color, .95f, DARKER.format(5))
  shade(list[2], color, .85f, DARKER.format(15))
  shade(list[3], color, .75f, DARKER.format(25))
  shade(list[4], color, .65f, DARKER.format(35))
  shade(list[5], color, .5f, DARKER.format(50))
}

private fun makePalette2(list: List<JLabel>, color: Color) {
  tint(list[0], color, 0f, "")
  tint(list[1], color, .8f, LIGHTER.format(80))
  tint(list[2], color, .6f, LIGHTER.format(60))
  tint(list[3], color, .4f, LIGHTER.format(40))
  tint(list[4], color, -.25f, DARKER.format(25))
  tint(list[5], color, -.5f, DARKER.format(50))
}

private fun makePalette3(list: List<JLabel>, color: Color) {
  luminance(list[0], color, 1.0, 0.0, "")
  luminance(list[1], color, .2, .8, LIGHTER.format(80))
  luminance(list[2], color, .4, .6, LIGHTER.format(60))
  luminance(list[3], color, .6, .4, LIGHTER.format(40))
  luminance(list[4], color, .75, 0.0, DARKER.format(25))
  luminance(list[5], color, .5, 0.0, DARKER.format(50))
}

private fun tint(l: JLabel, color: Color, tint: Float, txt: String) {
  val c = ColorUtils.getTintColor(color, tint)
  l.background = c
  l.text = txt + getColorCode(c)
}

private fun shade(l: JLabel, color: Color, shade: Float, txt: String) {
  val c = ColorUtils.getShadeColor(color, shade)
  l.background = c
  l.text = txt + getColorCode(c)
}

private fun luminance(
  l: JLabel,
  c: Color,
  lumMod: Double,
  lumOff: Double,
  s: String,
) {
  val bg = ColorUtils.getLuminanceColor(c, lumMod, lumOff)
  l.background = bg
  l.text = s + getColorCode(bg)
}

private fun getColorCode(c: Color) = "#%06X".format(c.rgb and 0xFF_FF_FF)

private fun makeCellList() = listOf(
  makeCell(),
  makeCell(),
  makeCell(),
  makeCell(),
  makeCell(),
  makeCell(),
)

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

  fun getLuminanceColor(c: Color, lumMod: Double, lumOff: Double): Color {
    val r = c.red / 255.0
    val g = c.green / 255.0
    val b = c.blue / 255.0
    val hsl = rgbToHsl(r, g, b)
    val lum = hsl[2] * lumMod + lumOff
    return hslToRgb(hsl[0], hsl[1], lum)
  }

  fun rgbToHsl(r: Double, g: Double, b: Double): DoubleArray {
    val max = listOf(r, g, b).maxOrNull() ?: 0.0
    val min = listOf(r, g, b).minOrNull() ?: 0.0
    val l = (max + min) / 2.0
    val v = DoubleArray(3)
    if (max == min) {
      v[0] = 0.0
      v[1] = 0.0
    } else {
      val d = max - min
      val s = if (l > .5) d / (2.0 - max - min) else d / (max + min)
      val h = if (r > g && r > b) {
        (g - b) / d + if (g < b) 6.0 else 0.0
      } else if (g > b) {
        (b - r) / d + 2.0
      } else {
        (r - g) / d + 4.0
      }
      v[0] = h / 6.0
      v[1] = s
    }
    v[2] = l
    return v
  }

  fun hslToRgb(h: Double, s: Double, l: Double): Color {
    val c: Color
    val achromatic = abs(s) <= 1.0e-6
    if (achromatic) {
      val v = to255(l)
      c = Color(v, v, v)
    } else {
      val q = if (l < .5) l * (1.0 + s) else l + s - l * s
      val p = 2.0 * l - q
      val r = to255(hueToRgb(p, q, h + 1.0 / 3.0))
      val g = to255(hueToRgb(p, q, h))
      val b = to255(hueToRgb(p, q, h - 1.0 / 3.0))
      c = Color(r, g, b)
    }
    return c
  }

  fun to255(v: Double) = (255.0 * v).roundToInt().coerceIn(0..255)

  fun hueToRgb(p: Double, q: Double, t: Double): Double {
    val t1 = if (t < 0.0) t + 1.0 else t
    val tt = if (t1 > 1.0) t1 - 1.0 else t1
    val c = if (tt < 1.0 / 6.0) {
      p + (q - p) * 6.0 * tt
    } else if (tt < 1.0 / 2.0) {
      q
    } else if (tt < 2.0 / 3.0) {
      p + (q - p) * (2.0 / 3.0 - tt) * 6.0
    } else {
      p
    }
    return c
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
