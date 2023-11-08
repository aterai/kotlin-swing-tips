package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.util.Collections
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.Timer

fun makeUI() = JPanel(GridLayout(1, 2)).also {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/test.png")
  val img = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  EventQueue.invokeLater {
    it.rootPane.glassPane = LightboxGlassPane(img)
    it.rootPane.glassPane.isVisible = false
  }
  val button = JButton("Open")
  button.addActionListener { button.rootPane.glassPane.isVisible = true }
  it.add(makeSamplePanel())
  it.add(button)
  it.preferredSize = Dimension(320, 240)
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private fun makeSamplePanel(): JPanel {
  val b = JButton("Button & Mnemonic")
  b.mnemonic = KeyEvent.VK_B
  val t = JTextField("TextField & ToolTip")
  t.toolTipText = "ToolTip"
  val p = JPanel(BorderLayout(5, 5))
  p.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  p.add(b, BorderLayout.NORTH)
  p.add(t, BorderLayout.SOUTH)
  p.add(JScrollPane(JTree()))
  return p
}

private class LightboxGlassPane(private val img: BufferedImage) : JPanel() {
  private val animatedIcon = LoadingIcon()
  private var alpha = 0f
  private val currentSize = Dimension()
  private val rect = Rectangle()
  private val animator = Timer(10) {
    animatedIcon.next()
    repaint()
  }
  private var handler: Handler? = null

  override fun updateUI() {
    removeMouseListener(handler)
    removeHierarchyListener(handler)
    super.updateUI()
    isOpaque = false
    super.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
    handler = Handler()
    addMouseListener(handler)
    addHierarchyListener(handler)
  }

  private inner class Handler : MouseAdapter(), HierarchyListener {
    override fun mouseClicked(e: MouseEvent) {
      e.component.isVisible = false
    }

    override fun hierarchyChanged(e: HierarchyEvent) {
      val f = e.changeFlags.toInt() and HierarchyEvent.DISPLAYABILITY_CHANGED != 0
      if (f && !e.component.isDisplayable) {
        animator.stop()
      }
    }
  }

  override fun setVisible(b: Boolean) {
    val oldVisible = isVisible
    super.setVisible(b)
    rootPane?.takeUnless { b == oldVisible }?.layeredPane?.isVisible = !b
    if (b && !animator.isRunning) {
      currentSize.setSize(40, 40)
      alpha = 0f
      animator.start()
    } else {
      animator.stop()
    }
    animatedIcon.setRunning(b)
  }

  override fun paintComponent(g: Graphics) {
    rootPane?.layeredPane?.print(g)
    super.paintComponent(g)

    when {
      currentSize.height < img.height + BW + BW -> {
        val dh = img.height / 16
        currentSize.height += dh
      }
      currentSize.width < img.width + BW + BW -> {
        currentSize.height = img.height + BW + BW
        val dw = img.width / 16
        currentSize.width += dw
      }
      1f - alpha > 0 -> {
        currentSize.width = img.width + BW + BW
        alpha += .1f
      }
      else -> {
        animatedIcon.setRunning(false)
        animator.stop()
      }
    }
    rect.size = currentSize
    val screen = bounds
    val centerPt = Point(screen.x + screen.width / 2, screen.y + screen.height / 2)
    rect.setLocation(centerPt.x - rect.width / 2, centerPt.y - rect.height / 2)

    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = Color(0x64_64_64_64, true)
    g2.fill(screen)
    g2.paint = Color(0xC8_FF_FF_FF.toInt(), true)
    g2.fill(rect)

    if (alpha > 0) {
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.coerceAtMost(1f))
      g2.drawImage(img, rect.x + BW, rect.y + BW, img.width, img.height, this)
    } else {
      val cx = centerPt.x - animatedIcon.iconWidth / 2
      val cy = centerPt.y - animatedIcon.iconHeight / 2
      animatedIcon.paintIcon(this, g2, cx, cy)
    }
    g2.dispose()
  }

  companion object {
    private const val BW = 5
  }
}

private class LoadingIcon : Icon {
  private val list = mutableListOf(
    Ellipse2D.Double(SX + 3 * R, SY + 0 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 5 * R, SY + 1 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 6 * R, SY + 3 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 5 * R, SY + 5 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 3 * R, SY + 6 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 1 * R, SY + 5 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 0 * R, SY + 3 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 1 * R, SY + 1 * R, 2 * R, 2 * R),
  )
  private var running = false

  operator fun next() {
    if (running) {
      // list.add(list.remove(0))
      Collections.rotate(list, 1)
    }
  }

  fun setRunning(running: Boolean) {
    this.running = running
  }

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color(0x0, true)
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = ELLIPSE_COLOR
    val size = list.size
    for (i in 0 until size) {
      val alpha = if (running) (i + 1) / size.toFloat() else .5f
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
      g2.fill(list[i])
    }
    g2.dispose()
  }

  override fun getIconWidth() = WIDTH

  override fun getIconHeight() = HEIGHT

  companion object {
    private val ELLIPSE_COLOR = Color(0x80_80_80)
    private const val R = 2.0
    private const val SX = 0.0
    private const val SY = 0.0
    private const val WIDTH = (R * 8 + SX * 2).toInt()
    private const val HEIGHT = (R * 8 + SY * 2).toInt()
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
    g2.color = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 240

  override fun getIconHeight() = 180
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
