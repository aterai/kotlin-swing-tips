package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  // CRW_3857_JFR.jpg: http://sozai-free.com/
  val url = cl.getResource("example/CRW_3857_JFR.jpg")
  val image = url?.openStream()?.use(ImageIO::read)
  val icon = image?.let { ImageIcon(it) } ?: MissingIcon()
  val label = JLabel(icon)

  val viewport = object : JViewport() {
    private var isAdjusting = false
    override fun revalidate() {
      // if (!weightMixing && isAdjusting) {
      if (isAdjusting) {
        return
      }
      super.revalidate()
    }

    override fun setViewPosition(p: Point) {
      // if (weightMixing) {
      //   super.setViewPosition(p)
      // } else {
      isAdjusting = true
      super.setViewPosition(p)
      isAdjusting = false
      // }
    }
  }
  viewport.add(label)

  val scroll = JScrollPane() // JScrollPane(label)
  scroll.viewport = viewport

  val hsl1 = HandScrollListener()
  viewport.addMouseMotionListener(hsl1)
  viewport.addMouseListener(hsl1)

  val radio = JRadioButton("scrollRectToVisible", true)
  radio.addItemListener { e ->
    hsl1.withinRangeMode = e.stateChange == ItemEvent.SELECTED
  }

  val box = Box.createHorizontalBox()
  val bg = ButtonGroup()
  listOf(radio, JRadioButton("setViewPosition")).forEach {
    box.add(it)
    bg.add(it)
  }

  // // TEST:
  // MouseAdapter hsl2 = new DragScrollListener()
  // label.addMouseMotionListener(hsl2)
  // label.addMouseListener(hsl2)

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class HandScrollListener : MouseAdapter() {
  private val defCursor = Cursor.getDefaultCursor()
  private val hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val pp = Point()
  var withinRangeMode = true

  override fun mouseDragged(e: MouseEvent) {
    val viewport = e.component as? JViewport ?: return
    val cp = e.point
    val vp = viewport.viewPosition
    vp.translate(pp.x - cp.x, pp.y - cp.y)
    val c = SwingUtilities.getUnwrappedView(viewport)
    if (withinRangeMode && c is JComponent) {
      c.scrollRectToVisible(Rectangle(vp, viewport.size))
    } else {
      viewport.viewPosition = vp
    }
    pp.location = cp
  }

  override fun mousePressed(e: MouseEvent) {
    e.component.cursor = hndCursor
    pp.location = e.point
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = defCursor
  }
}

private class MissingIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
