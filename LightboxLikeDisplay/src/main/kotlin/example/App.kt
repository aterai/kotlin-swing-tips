package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.util.Collections
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(GridLayout(1, 2)).also {
  EventQueue.invokeLater {
    it.rootPane.glassPane = LightboxGlassPane()
    it.rootPane.glassPane.isVisible = false
  }
  val button = JButton("Open")
  button.addActionListener { button.rootPane.glassPane.isVisible = true }
  it.add(makeDummyPanel())
  it.add(button)
  it.preferredSize = Dimension(320, 240)
}

private fun makeDummyPanel(): JPanel {
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

private class LightboxGlassPane : JPanel() {
  private val img = ImageIcon(LightboxGlassPane::class.java.getResource("test.png"))
  @Transient private val animatedIcon = LoadingIcon()
  private var alpha = 0f
  private var curImgWidth = 0
  private var curImgHeight = 0
  private val rect = Rectangle()
  private val animator = Timer(10) {
    animatedIcon.next()
    repaint()
  }
  @Transient private var handler: Handler? = null

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
      curImgWidth = 40
      curImgHeight = 40
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
      curImgHeight < img.iconHeight + BW + BW ->
        curImgHeight += img.iconHeight / 16
      curImgWidth < img.iconWidth + BW + BW -> {
        curImgHeight = img.iconHeight + BW + BW
        curImgWidth += img.iconWidth / 16
      }
      1f - alpha > 0 -> {
        curImgWidth = img.iconWidth + BW + BW
        alpha += .1f
      }
      else -> {
        animatedIcon.setRunning(false)
        animator.stop()
      }
    }
    rect.setSize(curImgWidth, curImgHeight)
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
      g2.drawImage(img.image, rect.x + BW, rect.y + BW, img.iconWidth, img.iconHeight, this)
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
    Ellipse2D.Double(SX + 1 * R, SY + 1 * R, 2 * R, 2 * R)
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

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
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
