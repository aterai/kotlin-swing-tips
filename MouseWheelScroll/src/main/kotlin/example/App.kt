package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.*

private var isShiftPressed = false

fun makeUI(): Component {
  val label = JLabel(getIcon())
  val ml = DragScrollListener()
  label.addMouseMotionListener(ml)
  label.addMouseListener(ml)

  val scroll = JScrollPane(label)
  val verticalBar = scroll.verticalScrollBar
  val horizontalBar = scroll.horizontalScrollBar
  val zeroVerticalBar = object : JScrollBar(Adjustable.VERTICAL) {
    override fun isVisible() = !isShiftPressed && super.isVisible()

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 0
      return d
    }
  }
  val zeroHorizontalBar = object : JScrollBar(Adjustable.HORIZONTAL) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = 0
      return d
    }
  }
  listOf(zeroVerticalBar, zeroHorizontalBar, verticalBar, horizontalBar)
    .forEach { it.unitIncrement = 25 }

  initActionMap(scroll)

  val r0 = JRadioButton("PreferredSize: 0, shift pressed: Horizontal WheelScrolling", true)
  r0.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
      scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
      scroll.verticalScrollBar = zeroVerticalBar
      scroll.horizontalScrollBar = zeroHorizontalBar
    }
  }

  val r1 = JRadioButton("SCROLLBAR_ALWAYS")
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
      scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
      scroll.verticalScrollBar = verticalBar
      scroll.horizontalScrollBar = horizontalBar
    }
  }

  val r2 = JRadioButton("SCROLLBAR_NEVER")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
      scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    }
  }

  val bg = ButtonGroup()
  listOf(r0, r1, r2).forEach { bg.add(it) }
  val b = Box.createHorizontalBox()
  val p = JPanel(GridLayout(2, 1))
  b.add(r1)
  b.add(r2)
  p.add(r0)
  p.add(b)
  scroll.verticalScrollBar = zeroVerticalBar
  scroll.horizontalScrollBar = zeroHorizontalBar

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getIcon(): Icon {
  val path = "example/CRW_3857_JFR.jpg"
  val cl = Thread.currentThread().contextClassLoader
  val image = cl.getResource(path)?.openStream()?.use(ImageIO::read)
  return image?.let { ImageIcon(it) } ?: MissingIcon()
}

private fun initActionMap(scroll: JScrollPane) {
  val im = scroll.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
  im.put(KeyStroke.getKeyStroke("SHIFT"), "pressed")
  im.put(KeyStroke.getKeyStroke("released SHIFT"), "released")

  val am = scroll.actionMap
  val a1 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      isShiftPressed = true
    }
  }
  am.put("pressed", a1)

  val a2 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      isShiftPressed = false
    }
  }
  am.put("released", a2)
}

private class DragScrollListener : MouseAdapter() {
  private val defCursor = Cursor.getDefaultCursor()
  private val hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val pp = Point()

  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    val p = SwingUtilities.getUnwrappedParent(c)
    if (p is JViewport && c is JComponent) {
      val cp = SwingUtilities.convertPoint(c, e.point, p)
      val vp = p.viewPosition
      vp.translate(pp.x - cp.x, pp.y - cp.y)
      c.scrollRectToVisible(Rectangle(vp, p.size))
      pp.location = cp
    }
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    c.cursor = hndCursor
    (SwingUtilities.getUnwrappedParent(c) as? JViewport)?.also {
      pp.location = SwingUtilities.convertPoint(c, e.point, it)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = defCursor
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

private class MissingIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.color = Color.WHITE
    g2.fillRect(x, y, w, h)
    g2.color = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 1000

  override fun getIconHeight() = 1000
}
