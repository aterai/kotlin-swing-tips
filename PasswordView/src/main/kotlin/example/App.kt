package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.plaf.basic.BasicPasswordFieldUI
import javax.swing.text.Element
import javax.swing.text.PasswordView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun makeUI(): Component {
  val pf1 = JPasswordField()
  pf1.echoChar = '★'
  val pf2 = object : JPasswordField() {
    override fun updateUI() {
      super.updateUI()
      setUI(IconPasswordFieldUI.createUI(this))
    }
  }
  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("setEchoChar('★')", pf1))
    it.add(makeTitledPanel("drawEchoCharacter", pf2))
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  cmp: Component,
): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  p.add(cmp, c)
  return p
}

private class IconPasswordFieldUI : BasicPasswordFieldUI() {
  override fun create(elem: Element) = IconPasswordView(elem)

  class IconPasswordView(
    element: Element?,
  ) : PasswordView(element) {
    private val icon = StarIcon()

    override fun drawEchoCharacter(
      g: Graphics,
      x: Int,
      y: Int,
      c: Char,
    ): Int {
      val fm = g.fontMetrics
      icon.paintIcon(null, g, x, y - fm.ascent)
      return x + icon.iconWidth
    }
  }

  companion object {
    fun createUI(c: JPasswordField): IconPasswordFieldUI {
      c.echoChar = '■' // U+25A0: As wide as a CJK character cell (full width)
      return IconPasswordFieldUI()
    }
  }
}

private class StarIcon : Icon {
  private val star = makeStar(6, 3, 8)

  fun makeStar(
    r1: Int,
    r2: Int,
    vc: Int,
  ): Path2D {
    val or = r1.coerceAtLeast(r2).toDouble()
    val ir = r1.coerceAtMost(r2).toDouble()
    var agl = 0.0
    val add = PI / vc
    val p = Path2D.Double()
    p.moveTo(or, 0.0)
    for (i in 0..<vc * 2 - 1) {
      agl += add
      val r = if (i % 2 == 0) ir else or
      p.lineTo(r * cos(agl), r * sin(agl))
    }
    p.closePath()
    val at = AffineTransform.getRotateInstance(-PI / 2.0, or, 0.0)
    return Path2D.Double(p, at)
  }

  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = Color.PINK
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
