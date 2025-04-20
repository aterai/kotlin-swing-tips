package example

import java.awt.*
import java.awt.event.FocusEvent
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val p1 = JPanel(BorderLayout())
  p1.setBorder(makeRoundedTitledBorder("Title001", 4))
  p1.add(JScrollPane(JTree()))
  val p2 = JPanel(BorderLayout())
  p2.setBorder(makeRoundedTitledBorder("Title(2)", 8))
  p2.add(JScrollPane(JTable(5, 3)))
  val scroll = JScrollPane(JTextArea(10, 20))
  scroll.setBorder(RoundedBorder(8))
  scroll.setViewportBorder(BorderFactory.createEmptyBorder())
  val p3 = JLayer(scroll, TitleLayerUI("Title: 3", 8))
  return JPanel(GridLayout(3, 1, 5, 5)).also {
    it.add(p1)
    it.add(p2)
    it.add(p3)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeRoundedTitledBorder(text: String, arc: Int): Border {
  val b = RoundedTitledBorder(text, arc)
  b.setTitlePosition(TitledBorder.BELOW_TOP)
  b.setTitleColor(Color.WHITE)
  return b
}

private class RoundedTitledBorder(
  title: String,
  private val arc: Int,
) : TitledBorder(RoundedBorder(arc), title) {
  private val label = JLabel(" ")

  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val b = getBorder()
    if (b is RoundedBorder) {
      val d: Dimension = getLabel(c).getPreferredSize()
      val i = b.getBorderInsets(c)
      val a2 = arc * 2
      val w = d.width + i.left + i.right + a2
      val h = d.height + i.top + i.bottom
      g2.clip = b.getBorderShape(x + i.left, y + i.top, w, h)
      g2.paint = Color.GRAY
      val titleBg: Shape = RoundRectangle2D.Float(
        (x - a2).toFloat(),
        (y - a2).toFloat(),
        (w + a2).toFloat(),
        (h + a2).toFloat(),
        arc.toFloat(),
        arc.toFloat(),
      )
      g2.fill(titleBg)
      g2.dispose()
    }
    g2.dispose()
    super.paintBorder(c, g, x, y, width, height)
  }

  private fun getLabel(c: Component): JLabel {
    this.label.setText(getTitle())
    this.label.setFont(getFont(c))
    this.label.setComponentOrientation(c.getComponentOrientation())
    this.label.setEnabled(c.isEnabled)
    return this.label
  }
}

private class RoundedBorder(
  private val arc: Int,
) : EmptyBorder(2, 2, 2, 2) {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val border = getBorderShape(x, y, width, height)
    g2.paint = ALPHA_ZERO
    val s = Rectangle2D.Double(
      x.toDouble(),
      y.toDouble(),
      width.toDouble(),
      height.toDouble(),
    )
    val clear = Area(s)
    clear.subtract(Area(border))
    g2.fill(clear)
    g2.paint = Color.GRAY
    g2.stroke = BasicStroke(1.5f)
    g2.draw(border)
    g2.dispose()
  }

  fun getBorderShape(x: Int, y: Int, w: Int, h: Int): Shape = RoundRectangle2D.Double(
    x.toDouble(),
    y.toDouble(),
    (w - 1).toDouble(),
    (h - 1).toDouble(),
    arc.toDouble(),
    arc.toDouble(),
  )

  companion object {
    private val ALPHA_ZERO: Paint = Color(0x0, true)
  }
}

private class TitleLayerUI(
  title: String,
  arc: Int,
) : LayerUI<JScrollPane>() {
  private val label = RoundedLabel(title, arc)

  init {
    label.border = BorderFactory.createEmptyBorder(2, 10, 2, 10)
    label.foreground = Color.WHITE
    label.background = Color.GRAY
  }

  override fun paint(g: Graphics?, c: JComponent?) {
    super.paint(g, c)
    if (c is JLayer<*>) {
      val sp = c.getView() as? JScrollPane ?: return
      val r = SwingUtilities.calculateInnerArea(sp, sp.bounds)
      if (r != null && !sp.viewport.view.hasFocus()) {
        val d = label.getPreferredSize()
        SwingUtilities.paintComponent(g, label, sp, r.x - 1, r.y - 1, d.width, d.height)
      }
    }
  }

  override fun updateUI(l: JLayer<out JScrollPane>) {
    super.updateUI(l)
    SwingUtilities.updateComponentTreeUI(label)
  }

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.setLayerEventMask(AWTEvent.FOCUS_EVENT_MASK)
    }
  }

  override fun uninstallUI(c: JComponent?) {
    if (c is JLayer<*>) {
      c.setLayerEventMask(0)
    }
    super.uninstallUI(c)
  }

  override fun processFocusEvent(e: FocusEvent?, l: JLayer<out JScrollPane>) {
    super.processFocusEvent(e, l)
    l.getView().repaint()
  }

  private class RoundedLabel(
    title: String?,
    private val arc: Int,
  ) : JLabel(title) {
    override fun paintComponent(g: Graphics) {
      if (!isOpaque) {
        val d = getPreferredSize()
        val w = d.width - 1
        val h = d.height - 1
        val h2 = h / 2
        val g2 = g.create() as? Graphics2D ?: return
        g2.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON,
        )
        g2.paint = getBackground()
        g2.fillRect(0, 0, w, h2)
        g2.fillRoundRect(-arc, 0, w + arc, h, arc, arc)
        g2.dispose()
      }
      super.paintComponent(g)
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
