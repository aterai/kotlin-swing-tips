package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.EmptyBorder
import kotlin.math.roundToInt

fun makeLabelTable(row: Int, column: Int): Component {
  val p = JPanel(GridBagLayout())
  val c = GridBagConstraints()
  c.fill = GridBagConstraints.BOTH
  c.weighty = 1.0
  c.weightx = 1.0
  val length = 5f
  val spacing = 5f
  val array = floatArrayOf(length - 1f, spacing + 1f)
  val dashedStroke = BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 2f, array, 0f)
  val dashed = StrokeMatteBorder(0, 0, 1, 1, dashedStroke, Color.BLACK)
  c.gridy = 0
  while (c.gridy < row) {
    c.gridx = 0
    while (c.gridx < column) {
      val l = makeLabel(String.format(Locale.ENGLISH, "%d%d", c.gridx, c.gridy))
      l.border = BorderFactory.createCompoundBorder(dashed, BorderFactory.createEmptyBorder(1, 1, 0, 0))
      p.add(l, c)
      c.gridx++
    }
    c.gridy++
  }
  p.border = BorderFactory.createCompoundBorder(
    BorderFactory.createEmptyBorder(15, 15, 15 + 1, 15 + 1),
    StrokeMatteBorder(1, 1, 0, 0, dashedStroke, Color.RED)
  )
  return p
}

private fun makeLabel(title: String) = JLabel(title, SwingConstants.CENTER)

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(makeLabelTable(6, 4))
  it.preferredSize = Dimension(320, 240)
}

private class StrokeMatteBorder(
  top: Int,
  left: Int,
  bottom: Int,
  right: Int,
  @field:Transient private val stroke: BasicStroke,
  private val color: Color?
) : EmptyBorder(top, left, bottom, right) {
  override fun paintBorder(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int
  ) {
    val size = stroke.lineWidth
    if (size > 0) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.stroke = stroke
      g2.paint = color ?: c?.foreground
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.translate(x, y)
      val s = size.roundToInt()
      val sd2 = (size / 2f).roundToInt()
      val insets = getBorderInsets(c)
      if (insets.top > 0) {
        g2.drawLine(0, sd2, width - s, sd2)
      }
      if (insets.left > 0) {
        g2.drawLine(sd2, sd2, sd2, height - s)
      }
      if (insets.bottom > 0) {
        g2.drawLine(0, height - s, width - s, height - s)
      }
      if (insets.right > 0) {
        g2.drawLine(width - sd2, 0, width - sd2, height - s)
      }
      g2.dispose()
    }
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
