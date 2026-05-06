package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.*

var isWeightMixing = false

fun createUI(): Component {
  val scroll = object : JScrollPane(JLabel(createIcon())) {
    override fun createViewport(): JViewport = CustomViewport()
  }
  val hsl1 = HandDragScrollListener()
  val viewport = scroll.getViewport()
  viewport.addMouseMotionListener(hsl1)
  viewport.addMouseListener(hsl1)

  val radio = JRadioButton("scrollRectToVisible", true)
  radio.addItemListener { e ->
    hsl1.setScrollRectToVisibleMode(e.stateChange == ItemEvent.SELECTED)
  }

  val box = Box.createHorizontalBox()
  val bg = ButtonGroup()
  listOf(radio, JRadioButton("setViewPosition")).forEach {
    box.add(it)
    bg.add(it)
  }

  // // TEST:
  // val hsl2 = DragScrollListener()
  // label.addMouseMotionListener(hsl2)
  // label.addMouseListener(hsl2)

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createIcon(): Icon {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/CRW_3857_JFR.jpg")
  val image = url?.openStream()?.use(ImageIO::read)
  return image?.let { ImageIcon(it) } ?: MissingIcon()
}

private class CustomViewport : JViewport() {
  private var isAdjusting = false

  override fun revalidate() {
    if (isWeightMixing || !isAdjusting) {
      super.revalidate()
    }
  }

  override fun setViewPosition(p: Point?) {
    if (isWeightMixing) {
      super.setViewPosition(p)
    } else {
      isAdjusting = true
      super.setViewPosition(p)
      isAdjusting = false
    }
  }
}

private class HandDragScrollListener : MouseAdapter() {
  private val defaultCursor = Cursor.getDefaultCursor()
  private val handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val previousPoint = Point()
  private var isBoundedMode = true

  override fun mouseDragged(e: MouseEvent) {
    val viewport = e.component as? JViewport ?: return
    val cp = e.getPoint()
    val rect = viewport.viewRect
    rect.translate(previousPoint.x - cp.x, previousPoint.y - cp.y)
    val c = SwingUtilities.getUnwrappedView(viewport)
    if (isBoundedMode && c is JComponent) {
      c.scrollRectToVisible(rect)
    } else {
      viewport.setViewPosition(rect.location)
    }
    previousPoint.location = cp
  }

  override fun mousePressed(e: MouseEvent) {
    e.component.setCursor(handCursor)
    previousPoint.location = e.getPoint()
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.setCursor(defaultCursor)
  }

  fun setScrollRectToVisibleMode(b: Boolean) {
    isBoundedMode = b
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
    g2.paint = Color.WHITE
    g2.fillRect(x, y, w, h)
    g2.paint = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 1000

  override fun getIconHeight() = 1000
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
