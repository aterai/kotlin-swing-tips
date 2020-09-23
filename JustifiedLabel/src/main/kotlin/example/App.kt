package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.font.GlyphVector
import java.awt.geom.Point2D
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(GridBagLayout())
  val inside = BorderFactory.createEmptyBorder(10, 5 + 2, 10, 10 + 2)
  val outside = BorderFactory.createTitledBorder("JLabel text-align:justify")
  p.border = BorderFactory.createCompoundBorder(outside, inside)
  val c = GridBagConstraints()
  c.insets = Insets(5, 5, 5, 0)
  c.fill = GridBagConstraints.HORIZONTAL
  c.gridx = 0
  p.add(JLabel("打率"), c)
  p.add(JLabel("打率", SwingConstants.RIGHT), c)
  p.add(JustifiedLabel("打率"), c)
  p.add(JLabel("出塁率", SwingConstants.CENTER), c)
  p.add(JustifiedLabel("出塁率"), c)
  p.add(JustifiedLabel("チーム出塁率"), c)
  c.gridx = 1
  c.weightx = 1.0
  p.add(JTextField(), c)
  p.add(JTextField(), c)
  p.add(JTextField(), c)
  p.add(JTextField(), c)
  p.add(JTextField(), c)
  p.add(JTextField(), c)
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(JustifiedLabel("あいうえおかきくけこ"), BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class JustifiedLabel(str: String? = null) : JLabel(str) {
  @Transient
  private var gvText: GlyphVector? = null
  private var prevWidth = -1
  override fun setText(text: String) {
    super.setText(text)
    prevWidth = -1
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    val font = font
    val d = size
    val ins = insets
    val w = d.width - ins.left - ins.right
    if (w != prevWidth) {
      val gv = font.createGlyphVector(g2.fontRenderContext, text)
      gvText = makeJustifiedGlyphVector(gv, w)
      prevWidth = w
    }
    gvText?.also {
      g2.paint = background
      g2.fillRect(0, 0, d.width, d.height)
      g2.paint = foreground
      g2.drawGlyphVector(it, ins.left.toFloat(), ins.top + font.size2D)
    }
    g2.dispose()
  }

  private fun makeJustifiedGlyphVector(gv: GlyphVector, width: Int): GlyphVector {
    val r = gv.visualBounds
    val jw = width.toFloat()
    val vw = r.width.toFloat()
    if (jw > vw) {
      val num = gv.numGlyphs
      val xx = (jw - vw) / (num - 1f)
      var pos = if (num == 1) (jw - vw) * .5f else 0f
      val gmPos = Point2D.Float()
      for (i in 0 until num) {
        val gm = gv.getGlyphMetrics(i)
        gmPos.setLocation(pos.toDouble(), 0.0)
        gv.setGlyphPosition(i, gmPos)
        pos += gm.advance + xx
      }
    }
    return gv
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
