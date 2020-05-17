package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import java.awt.font.FontRenderContext
import java.awt.font.GlyphVector
import java.awt.font.LineMetrics
import java.awt.geom.Line2D
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(MarqueePanel())
  it.preferredSize = Dimension(320, 240)
}

private class MarqueePanel : JComponent(), ActionListener {
  private val animator = Timer(10, this)
  private val gv: GlyphVector
  private val lm: LineMetrics
  private val corpusSize: Float // the x-height
  private var xx = 0f
  private var baseline = 0f
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = width.toFloat()
    g2.paint = Color.RED
    g2.draw(Line2D.Float(0f, baseline, w, baseline))

    g2.paint = Color.GREEN
    val ascent = baseline - lm.ascent
    g2.draw(Line2D.Float(0f, ascent, w, ascent))

    g2.paint = Color.BLUE
    val descent = baseline + lm.descent
    g2.draw(Line2D.Float(0f, descent, w, descent))

    g2.paint = Color.ORANGE
    val leading = baseline + lm.descent + lm.leading
    g2.draw(Line2D.Float(0f, leading, w, leading))

    g2.paint = Color.CYAN
    val xh = baseline - corpusSize
    g2.draw(Line2D.Float(0f, xh, w, xh))

    g2.paint = Color.BLACK
    g2.drawGlyphVector(gv, w - xx, baseline)
    g2.dispose()
  }

  override fun actionPerformed(e: ActionEvent) {
    xx = if (width + gv.visualBounds.width - xx > 0) xx + 2f else 0f
    baseline = height / 2f
    repaint()
  }

  init {
    addHierarchyListener { e ->
      if (e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L && !e.component.isDisplayable) {
        animator.stop()
      }
    }
    val text = "abcdefghijklmnopqrstuvwxyzABDEFGHIJKLMNOPQRSTUVWXYZ"
    val font = Font(Font.SERIF, Font.PLAIN, 100)
    val frc = FontRenderContext(null, true, true)
    gv = font.createGlyphVector(frc, text)
    lm = font.getLineMetrics(text, frc)
    val xgm = gv.getGlyphMetrics(23)
    corpusSize = xgm.bounds2D.height.toFloat()
    animator.start()
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
