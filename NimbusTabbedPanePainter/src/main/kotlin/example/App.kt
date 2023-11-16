package example

import java.awt.*
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*

fun makeUI(): Component {
  val tabbedPane = JTabbedPane()
  tabbedPane.addTab("JTree", JScrollPane(JTree()))
  tabbedPane.addTab("JSplitPane", JSplitPane())
  tabbedPane.addTab("JTextArea", JScrollPane(JTextArea()))

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private object NimbusTabbedPanePainterUtils {
  const val OVER_PAINT = 6
  const val STROKE_SIZE = 2.0
  const val ARC = 10.0
  val CONTENT_BACKGROUND = Color.LIGHT_GRAY
  val CONTENT_BORDER = Color.ORANGE // Color.GRAY
  val TAB_TABAREA_MASK = Color.GREEN // CONTENT_BACKGROUND
  val TAB_BACKGROUND = Color.PINK // CONTENT_BORDER
  val TABAREA_BACKGROUND = Color.CYAN // CONTENT_BACKGROUND
  val TABAREA_BORDER = Color.RED // CONTENT_BORDER

  fun configureUI() {
    val d = UIManager.getLookAndFeelDefaults()
    d["TabbedPane:TabbedPaneContent.contentMargins"] = Insets(0, 5, 5, 5)
    d["TabbedPane:TabbedPaneTabArea.contentMargins"] = Insets(3, 10, OVER_PAINT, 10)
    val tabAreaPainter = TabAreaPainter()
    d["TabbedPane:TabbedPaneTabArea[Disabled].backgroundPainter"] = tabAreaPainter
    d["TabbedPane:TabbedPaneTabArea[Enabled].backgroundPainter"] = tabAreaPainter
    d["TabbedPane:TabbedPaneTabArea[Enabled+MouseOver].backgroundPainter"] = tabAreaPainter
    d["TabbedPane:TabbedPaneTabArea[Enabled+Pressed].backgroundPainter"] = tabAreaPainter
    d["TabbedPane:TabbedPaneContent.backgroundPainter"] = TabbedPaneContentPainter()
    val tabPainter = TabPainter(false)
    d["TabbedPane:TabbedPaneTab[Enabled+MouseOver].backgroundPainter"] = tabPainter
    d["TabbedPane:TabbedPaneTab[Enabled+Pressed].backgroundPainter"] = tabPainter
    d["TabbedPane:TabbedPaneTab[Enabled].backgroundPainter"] = tabPainter
    val selTabPainter = TabPainter(true)
    d["TabbedPane:TabbedPaneTab[Focused+MouseOver+Selected].backgroundPainter"] = selTabPainter
    d["TabbedPane:TabbedPaneTab[Focused+Pressed+Selected].backgroundPainter"] = selTabPainter
    d["TabbedPane:TabbedPaneTab[Focused+Selected].backgroundPainter"] = selTabPainter
    d["TabbedPane:TabbedPaneTab[MouseOver+Selected].backgroundPainter"] = selTabPainter
    d["TabbedPane:TabbedPaneTab[Selected].backgroundPainter"] = selTabPainter
    d["TabbedPane:TabbedPaneTab[Pressed+Selected].backgroundPainter"] = selTabPainter
  }

  class TabPainter(private val selected: Boolean) : Painter<JComponent> {
    private val color = if (selected) CONTENT_BACKGROUND else TAB_BACKGROUND

    override fun paint(
      g: Graphics2D,
      c: JComponent,
      width: Int,
      height: Int,
    ) {
      val a = if (selected) OVER_PAINT else 0
      val r = 6
      val x = 3.0
      val y = 3.0
      val g2 = g.create(0, 0, width, height + a) as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val w = width - x
      val h = (height + a).toDouble()

      // Paint tab shadow
      if (selected) {
        g2.paint = Color(0, 0, 0, 20)
        val rect = RoundRectangle2D.Double(0.0, 0.0, w, h, r.toDouble(), r.toDouble())
        var i = 0
        while (i < x) {
          rect.setFrame(x - i, y - i, w + i + i, h)
          g2.fill(rect)
          i++
        }
      }

      // Fill tab background
      g2.color = color
      g2.fill(RoundRectangle2D.Double(x, y, w - 1.0, h + a, r.toDouble(), r.toDouble()))
      if (selected) {
        // Draw a border
        g2.stroke = BasicStroke(STROKE_SIZE.toFloat())
        g2.paint = TABAREA_BORDER
        g2.draw(RoundRectangle2D.Double(x, y, w - 1.0, h + a, r.toDouble(), r.toDouble()))

        // Over paint the overexposed area with the background color
        g2.color = TAB_TABAREA_MASK
        val yy = height + STROKE_SIZE
        g2.fill(Rectangle2D.Double(0.0, yy, width.toDouble(), OVER_PAINT.toDouble()))
      }
      g2.dispose()
    }
  }

  class TabAreaPainter : Painter<JComponent> {
    override fun paint(
      g: Graphics2D,
      c: JComponent,
      w: Int,
      h: Int,
    ) {
      val g2 = g.create(0, 0, w, h) as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val dy = (h - OVER_PAINT).toDouble()
      val ww = w - STROKE_SIZE
      val hh = h - STROKE_SIZE
      val r = RoundRectangle2D.Double(0.0, dy, ww, hh, ARC, ARC)
      g2.paint = TABAREA_BACKGROUND
      g2.fill(r)
      g2.color = TABAREA_BORDER
      g2.stroke = BasicStroke(STROKE_SIZE.toFloat())
      g2.draw(r)
      g2.dispose()
    }
  }

  class TabbedPaneContentPainter : Painter<JComponent> {
    override fun paint(
      g: Graphics2D,
      c: JComponent,
      w: Int,
      h: Int,
    ) {
      val g2 = g.create(0, 0, w, h) as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.translate(0, -OVER_PAINT)
      val ww = w - STROKE_SIZE
      val hh = h - STROKE_SIZE + OVER_PAINT
      val r = RoundRectangle2D.Double(0.0, 0.0, ww, hh, ARC, ARC)
      g2.paint = CONTENT_BACKGROUND
      g2.fill(r)
      g2.color = CONTENT_BORDER
      g2.stroke = BasicStroke(STROKE_SIZE.toFloat())
      g2.draw(r)
      g2.dispose()
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
      NimbusTabbedPanePainterUtils.configureUI()
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
