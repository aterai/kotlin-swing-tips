package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Path2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val list = listOf("aaa", "bb", "c")

  val p1 = JPanel(GridLayout(0, 1)).also {
    it.border = BorderFactory.createTitledBorder("Icon border")
    it.add(makeBreadcrumb(list, Color.PINK, 1))
    it.add(makeChevronBreadcrumb(list, Color.PINK, 11))
    it.add(makeRibbonBreadcrumb(list, Color.PINK, 11))
  }

  val p2 = JPanel(GridLayout(0, 1)).also {
    val layerUI = BreadcrumbLayerUI<Component>()
    it.border = BorderFactory.createTitledBorder("JLayer border")
    it.add(JLayer(makeBreadcrumb(list, Color.ORANGE, 1), layerUI))
    it.add(JLayer(makeChevronBreadcrumb(list, Color.ORANGE, 11), layerUI))
    it.add(JLayer(makeRibbonBreadcrumb(list, Color.ORANGE, 11), layerUI))
  }

  val p = JPanel(GridLayout(0, 1)).also {
    it.add(p1)
    it.add(p2)
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun makePanel(overlap: Int) = object : JPanel(FlowLayout(FlowLayout.LEADING, -overlap, 0)) {
  override fun isOptimizedDrawingEnabled() = false
}.also {
  it.isOpaque = false
}

fun makeBreadcrumb(list: List<String>, color: Color, overlap: Int): JPanel {
  val p = makePanel(overlap)
  p.border = BorderFactory.createEmptyBorder(5, overlap + 5, 5, 5)
  val bg = ButtonGroup()
  list.forEach {
    val b = makeButton(it, SizeIcon(), color)
    p.add(b)
    bg.add(b)
  }
  return p
}

fun makeChevronBreadcrumb(list: List<String>, color: Color, overlap: Int): JPanel {
  val p = makePanel(overlap)
  p.border = BorderFactory.createEmptyBorder(5, overlap + 5, 5, 5)
  val bg = ButtonGroup()
  list.forEach {
    val b = makeButton(it, ArrowToggleButtonIcon(), color)
    p.add(b)
    bg.add(b)
  }
  return p
}

fun makeRibbonBreadcrumb(list: List<String>, color: Color, overlap: Int): JPanel {
  val p = makePanel(overlap)
  p.border = BorderFactory.createEmptyBorder(5, overlap + 5, 5, 5)
  val bg = ButtonGroup()
  list.forEach {
    val b = makeButton(it, RibbonToggleButtonIcon(), color)
    p.add(b)
    bg.add(b)
  }
  return p
}

private fun makeButton(title: String, icon: Icon, color: Color): AbstractButton {
  val b = object : JRadioButton(title) {
    override fun contains(x: Int, y: Int) = (getIcon() as? ArrowToggleButtonIcon)?.let {
      it.shape?.contains(Point(x, y))
    } ?: super.contains(x, y)
  }
  b.icon = icon
  b.isContentAreaFilled = false
  b.border = BorderFactory.createEmptyBorder()
  b.horizontalTextPosition = SwingConstants.CENTER
  b.isFocusPainted = false
  b.isOpaque = false
  b.background = color
  return b
}

private open class ArrowToggleButtonIcon : Icon {
  var shape: Shape? = null
    private set

  open fun makeShape(parent: Container, c: Component, x: Int, y: Int): Shape {
    val w = c.width - 1.0
    val h = c.height - 1.0
    val h2 = h * .5
    val w2 = TH.toDouble()
    val p = Path2D.Double()
    p.moveTo(0.0, 0.0)
    p.lineTo(w - w2, 0.0)
    p.lineTo(w, h2)
    p.lineTo(w - w2, h)
    p.lineTo(0.0, h)
    if (c != parent.getComponent(0)) {
      p.lineTo(w2, h2)
    }
    p.closePath()
    val tx = x.toDouble()
    val ty = y.toDouble()
    return AffineTransform.getTranslateInstance(tx, ty).createTransformedShape(p)
  }

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val parent = c.parent ?: return
    shape = makeShape(parent, c, x, y)
    var bgc = parent.background
    var borderColor = Color.GRAY.brighter()
    if (c is AbstractButton) {
      val m = c.model
      if (m.isSelected || m.isRollover) {
        bgc = c.getBackground()
        borderColor = Color.GRAY
      }
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = bgc
    g2.fill(shape)
    g2.paint = borderColor
    g2.draw(shape)
    g2.dispose()
  }

  override fun getIconWidth() = WIDTH

  override fun getIconHeight() = HEIGHT

  companion object {
    const val TH = 10 // The height of a triangle
    private const val HEIGHT = TH * 2 + 1
    private const val WIDTH = 100
  }
}

private class SizeIcon : ArrowToggleButtonIcon() {
  override fun makeShape(parent: Container, c: Component, x: Int, y: Int): Shape {
    val w = c.width - 1.0
    val h = c.height - 1.0
    val p: Path2D = Path2D.Double()
    p.moveTo(0.0, 0.0)
    p.lineTo(w, 0.0)
    p.lineTo(w, h)
    p.lineTo(0.0, h)
    p.closePath()
    val tx = x.toDouble()
    val ty = y.toDouble()
    return AffineTransform.getTranslateInstance(tx, ty).createTransformedShape(p)
  }
}

private class RibbonToggleButtonIcon : ArrowToggleButtonIcon() {
  override fun makeShape(parent: Container, c: Component, x: Int, y: Int): Shape {
    val r = 4.0
    val w = c.width - 1.0
    val h = c.height - 1.0
    val h2 = h * .5
    val p: Path2D = Path2D.Double()
    p.moveTo(w - h2, 0.0)
    p.quadTo(w, 0.0, w, h2)
    p.quadTo(w, 0.0 + h, w - h2, h)
    if (c == parent.getComponent(0)) {
      // :first-child
      p.lineTo(r, h)
      p.quadTo(0.0, h, 0.0, h - r)
      p.lineTo(0.0, r)
      p.quadTo(0.0, 0.0, r, 0.0)
    } else {
      p.lineTo(0.0, h)
      p.quadTo(h2, h, h2, h2)
      p.quadTo(h2, 0.0, 0.0, 0.0)
    }
    p.closePath()
    val tx = x.toDouble()
    val ty = y.toDouble()
    return AffineTransform.getTranslateInstance(tx, ty).createTransformedShape(p)
  }
}

internal class BreadcrumbLayerUI<V : Component?> : LayerUI<V>() {
  @Transient
  private var shape: Shape? = null
  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val s = shape ?: return
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val r = Rectangle(c.width, c.height)
    val area = Area(r)
    area.subtract(Area(s))
    g2.clip = area
    g2.paint = Color(0x55_66_66_66, true)
    g2.stroke = BasicStroke(3f)
    g2.draw(s)
    g2.stroke = BasicStroke(2f)
    g2.draw(s)
    g2.stroke = BasicStroke(1f)
    g2.clip = r
    g2.paint = Color.WHITE
    g2.draw(s)
    g2.dispose()
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  private fun update(e: MouseEvent, l: JLayer<out V>) {
    val id = e.id
    val s = if (id == MouseEvent.MOUSE_ENTERED || id == MouseEvent.MOUSE_MOVED) {
      val c = e.component
      ((c as? AbstractButton)?.icon as? ArrowToggleButtonIcon)?.let {
        val r = c.bounds
        val at = AffineTransform.getTranslateInstance(r.x.toDouble(), r.y.toDouble())
        at.createTransformedShape(it.shape)
      }
    } else {
      null
    }
    if (s != shape) {
      shape = s
      l.view?.repaint()
    }
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out V>) {
    update(e, l)
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out V>) {
    update(e, l)
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
