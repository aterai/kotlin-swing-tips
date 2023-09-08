package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import javax.swing.*
import javax.swing.event.ChangeListener
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val outer = SpinnerNumberModel(40, 10, 1000, 1)
private val inner = SpinnerNumberModel(30, 10, 1000, 1)
private val vcModel = SpinnerNumberModel(20, 3, 100, 1)
private val styleField = JTextField("stroke:none; fill:pink")
private val check = JCheckBox("Antialias", true)
private val label = JLabel()
private val textArea = JTextArea()

fun makeUI(): Component {
  initStar()
  val cl = ChangeListener { initStar() }
  outer.addChangeListener(cl)
  inner.addChangeListener(cl)
  vcModel.addChangeListener(cl)
  check.addChangeListener(cl)
  label.verticalAlignment = SwingConstants.CENTER
  label.horizontalAlignment = SwingConstants.CENTER
  check.horizontalAlignment = SwingConstants.RIGHT
  val tab = JTabbedPane()
  tab.addTab("Preview", makePreviewPanel())
  tab.addTab("SVG", makeSvgPanel())
  val panel = JPanel(BorderLayout())
  panel.add(makePreferencesPanel(), BorderLayout.NORTH)
  panel.add(tab)
  return JPanel(BorderLayout()).also {
    it.add(panel)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePreviewPanel(): Component {
  val p = JPanel(BorderLayout())
  p.add(check, BorderLayout.SOUTH)
  p.add(JScrollPane(label))
  return p
}

private fun makePreferencesPanel(): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder("Preferences")
  val c = GridBagConstraints()
  c.gridx = 0
  c.insets = Insets(5, 5, 5, 0)
  c.anchor = GridBagConstraints.LINE_END
  p.add(JLabel("Addendum Circle Radius:"), c)
  p.add(JLabel("Dedendum Circle Radius:"), c)
  p.add(JLabel("Count of Teeth:"), c)
  c.gridx = 1
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  p.add(JSpinner(outer), c)
  p.add(JSpinner(inner), c)
  p.add(JSpinner(vcModel), c)
  return p
}

private fun makeSvgPanel(): Component {
  val button = JButton("set")
  button.addActionListener { initStar() }

  val sp = JPanel(BorderLayout(2, 2))
  sp.add(JLabel("style:"), BorderLayout.WEST)
  sp.add(styleField)
  sp.add(button, BorderLayout.EAST)

  val p = JPanel(BorderLayout(5, 5))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(sp, BorderLayout.SOUTH)
  p.add(JScrollPane(textArea))
  return p
}

private fun initStar() {
  val r1 = outer.number.toInt()
  val r2 = inner.number.toInt()
  val vc = vcModel.number.toInt()
  val antialias = check.isSelected
  val star = SvgUtils.makeStar(r1, r2, vc)
  label.icon = StarIcon(star, antialias)
  val style = styleField.text.trim()
  val min = r1.coerceAtMost(r2)
  val max = r1.coerceAtLeast(r2)
  val fmt = """
    addendum_circle_radius="%d" dedendum_circle_radius ="%d" number_of_teeth="%dT"
  """.trimIndent()
  val desc = fmt.format(max, min, vc)
  textArea.text = SvgUtils.makeStarburstSvg(star.getPathIterator(null), max * 2, style, desc)
}

private object SvgUtils {
  fun makeStarburstSvg(
    pi: PathIterator,
    sz: Int,
    style: String?,
    desc: String?,
  ): String {
    val sb = StringBuilder()
    val c = DoubleArray(6)
    while (!pi.isDone) {
      when (pi.currentSegment(c)) {
        PathIterator.SEG_MOVETO ->
          sb.append("M%.2f,%.2f ".format(c[0], c[1]))
        PathIterator.SEG_LINETO ->
          sb.append("L%.2f,%.2f ".format(c[0], c[1]))
        PathIterator.SEG_QUADTO ->
          sb.append("Q%.2f,%.2f,%.2f,%.2f ".format(c[0], c[1], c[2], c[3]))
        PathIterator.SEG_CUBICTO ->
          sb.append("C%.2f,%.2f,%.2f,%.2f,%.2f,%.2f ".format(c[0], c[1], c[2], c[3], c[4], c[5]))
        PathIterator.SEG_CLOSE ->
          sb.append('Z')
        else -> throw InternalError("unrecognized path type")
      }
      pi.next()
    }
    val path = sb.toString()
    return """
      <?xml version="1.0" encoding="UTF-8"?>
      <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
      <svg width="$sz" height="$sz" xmlns="http://www.w3.org/2000/svg">
        <desc>$desc</desc>
        <path d="$path" style="$style" />
      </svg>
      """.trimIndent()
  }

  fun makeStar(r1: Int, r2: Int, vc: Int): Path2D {
    val or = r1.coerceAtLeast(r2).toDouble()
    val ir = r1.coerceAtMost(r2).toDouble()
    var agl = 0.0
    val add = PI / vc
    val p: Path2D = Path2D.Double()
    p.moveTo(or, 0.0)
    for (i in 0 until vc * 2 - 1) {
      agl += add
      val r = if (i % 2 == 0) ir else or
      p.lineTo(r * cos(agl), r * sin(agl))
    }
    p.closePath()
    val at = AffineTransform.getRotateInstance(-PI / 2.0, or, 0.0)
    return Path2D.Double(p, at)
  }
}

private class StarIcon(private val star: Shape, private val antialias: Boolean) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.PINK
    if (antialias) {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    }
    g2.fill(star)
    g2.dispose()
  }

  override fun getIconWidth() = star.bounds.width

  override fun getIconHeight() = star.bounds.height
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
