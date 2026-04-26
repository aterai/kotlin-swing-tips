package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.plaf.LayerUI
import kotlin.math.abs
import kotlin.math.max

fun createUI(): Component {
  val names = arrayOf("Alice", "Bob", "Carol", "Dave", "Eve")
  val colors = arrayOf(
    Color(0xFF_63_86),
    Color(0x36_A2_EB),
    Color(0xFF_CE_56),
    Color(0x4B_C0_C0),
    Color(0x99_66_FF),
  )
  val layer1 = createAvatarGroup(names, colors)
  val layer2 = createAvatarGroup(names, colors)
  return JPanel(BorderLayout()).also {
    it.add(layer1, BorderLayout.NORTH)
    it.add(layer2, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(50, 50, 50, 50)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createAvatarGroup(names: Array<String>, colors: Array<Color>): JLayer<JPanel> {
  // Container for displaying avatars
  val avatarPanel = JPanel(StackedLayout(0.0))
  avatarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

  // Create 5 avatars
  for (i in colors.indices) {
    avatarPanel.add(createAvatarButton(i, names[i], colors[i]))
  }

  // Wrap with JLayer and apply animation UI
  return JLayer<JPanel>(avatarPanel, AvatarLayerUI())
}

private fun createAvatarButton(i: Int, name: String, color: Color): JButton {
  // Generate icons with varying sizes (100x100 to 200x200)
  val randomSize = 100 + (i * 25)
  val button = AvatarButton(UserIcon(name, color, randomSize))
  button.setToolTipText("User $name")
  button.addActionListener { Toolkit.getDefaultToolkit().beep() }
  return button
}

// Custom Layout Manager
// Arranges components based on gapFraction (0.0=stacked, 1.0=spread)
private class StackedLayout(private var gapFraction: Double) : LayoutManager {
  fun setGapFraction(gapFraction: Double) {
    this.gapFraction = gapFraction
  }

  override fun layoutContainer(parent: Container) {
    val n = parent.componentCount
    if (n > 0) {
      val insets = parent.insets
      var x = insets.left
      val y = insets.top
      for (i in 0..<n) {
        val c = parent.getComponent(i)
        val d = c.preferredSize
        c.setBounds(x, y, d.width, d.height)
        // Step calc: 60% of width as default overlap, 40% as animated spread
        val step = (d.width * .6 + (d.width * .4 * gapFraction)).toInt()
        x += step
      }
    }
  }

  override fun preferredLayoutSize(parent: Container): Dimension {
    val size = Dimension()
    val n = parent.componentCount
    if (n > 0) {
      var totalWidth = 0
      var maxHeight = 0
      for (i in 0..<n) {
        val c = parent.getComponent(i)
        val d = c.preferredSize
        maxHeight = max(maxHeight, d.height)
        if (i < n - 1) {
          // Add overlap for all but the last component
          val step = (d.width * .6 + (d.width * .4 * gapFraction)).toInt()
          totalWidth += step
        } else {
          totalWidth += d.width
        }
      }
      val insets = parent.insets
      totalWidth += insets.left + insets.right
      maxHeight += insets.top + insets.bottom
      size.setSize(totalWidth, maxHeight)
    }
    return size
  }

  override fun minimumLayoutSize(parent: Container): Dimension {
    return preferredLayoutSize(parent)
  }

  override fun addLayoutComponent(name: String, comp: Component) {
    // empty
  }

  override fun removeLayoutComponent(comp: Component) {
    // empty
  }
}

// Circular Avatar Button
internal class AvatarButton(icon: Icon) : JButton(icon) {
  private var tip: JToolTip? = null

  override fun updateUI() {
    super.updateUI()
    setContentAreaFilled(false)
    setBorderPainted(false)
    setFocusPainted(false)
  }

  override fun getPreferredSize(): Dimension {
    val w = DIAMETER + INSETS.left + INSETS.right
    val h = DIAMETER + INSETS.top + INSETS.bottom
    return Dimension(w, h)
  }

  override fun contains(x: Int, y: Int): Boolean {
    val w = getWidth()
    val h = getHeight()
    val circle = Ellipse2D.Double(0.0, 0.0, w.toDouble(), h.toDouble())
    return circle.contains(x.toDouble(), y.toDouble())
  }

  override fun createToolTip(): JToolTip {
    return tip ?: BalloonToolTip().also {
      it.setComponent(this)
    }
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
    )

    val w = getWidth()
    val h = getHeight()

    // 1. Draw the border with background color
    g2.color = getParent().getBackground()
    g2.fill(Ellipse2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))

    // 2. Render with Soft Clipping
    val gc = g2.deviceConfiguration
    val buffer = gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT)
    val g2d = buffer.createGraphics()
    g2d.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
    )
    g2d.setRenderingHint(
      RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR
    )

    g2d.composite = AlphaComposite.Src
    g2d.fill(Ellipse2D.Double(INSETS.left.toDouble(), INSETS.top.toDouble(), DIAMETER.toDouble(), DIAMETER.toDouble()))

    // Composite icon inside the circle using SrcAtop
    g2d.composite = AlphaComposite.SrcAtop

    // Scale icon to fit the circle
    val icon = getIcon()
    val scale = DIAMETER.toDouble() / max(icon.iconWidth, icon.iconHeight)

    val at = AffineTransform.getTranslateInstance(INSETS.left.toDouble(), INSETS.top.toDouble())
    at.scale(scale, scale)
    g2d.transform(at)

    icon.paintIcon(this, g2d, 0, 0)
    g2d.dispose()

    g2.drawImage(buffer, 0, 0, null)
    g2.dispose()
  }

  override fun getToolTipLocation(e: MouseEvent) = getToolTipText(e)?.let {
    val toolTip = createToolTip()
    toolTip.setTipText(toolTipText)
    val buttonBounds = SwingUtilities.calculateInnerArea(this, null)
    val tooltipSize = toolTip.getPreferredSize()
    val centerX = (buttonBounds.centerX - tooltipSize.getWidth() / 2.0).toInt()
    val topY = buttonBounds.y - tooltipSize.height - 2
    Point(centerX, topY)
  }

  companion object {
    private const val DIAMETER = 24
    private val INSETS = Insets(2, 2, 2, 2)
  }
}

// LayerUI that controls animation on mouse hover
private class AvatarLayerUI : LayerUI<JPanel?>() {
  private val timer = Timer(15, null)
  private var currentFraction = 0.0
  private var targetFraction = 0.0

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    val l = c as JLayer<*>
    l.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK)
    timer.addActionListener {
      (l.getView() as? JPanel)?.also {
        animation(it)
      }
    }
  }

  // Ease-Out: Moves 25% closer to target per frame
  private fun animation(panel: JPanel) {
    val diff = targetFraction - currentFraction
    val isEnd = abs(diff) < .1
    if (isEnd) {
      currentFraction = targetFraction
      timer.stop()
    } else {
      currentFraction += diff * .25
    }
    (panel.layout as? StackedLayout)?.also {
      it.setGapFraction(currentFraction)
      panel.revalidate()
      panel.repaint()
    }
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JPanel>) {
    if (e.getID() == MouseEvent.MOUSE_ENTERED) {
      startAnimation(1.0)
    } else if (e.getID() == MouseEvent.MOUSE_EXITED) {
      startAnimation(0.0)
    }
  }

  private fun startAnimation(target: Double) {
    this.targetFraction = target
    if (!timer.isRunning) {
      timer.start()
    }
  }
}

private class UserIcon(
  private val name: String,
  private val color: Color,
  private val size: Int,
) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = color
    g2.fillRect(x, y, size, size)

    g2.color = Color.RED
    g2.drawLine(x, y + size / 2, x + size, y + size / 2)
    g2.drawLine(x + size / 2, y, x + size / 2, y + size)

    g2.color = Color.WHITE
    g2.font = Font(Font.SANS_SERIF, Font.BOLD, size / 2)
    val fontMetrics = g2.fontMetrics
    val initial = name.substring(0, 1).uppercase()
    val textX = x + (size - fontMetrics.stringWidth(initial)) / 2
    val textY = y + ((size - fontMetrics.height) / 2) + fontMetrics.ascent
    g2.drawString(initial, textX, textY)
    g2.dispose()
  }

  override fun getIconWidth() = size

  override fun getIconHeight() = size
}

private class BalloonToolTip : JToolTip() {
  private var listener: HierarchyListener? = null

  override fun updateUI() {
    removeHierarchyListener(listener)
    super.updateUI()
    listener = HierarchyListener { e ->
      val c = e.component
      val f = e.changeFlags.toInt() and HierarchyEvent.SHOWING_CHANGED != 0
      if (f && c.isShowing) {
        SwingUtilities
          .getWindowAncestor(c)
          ?.takeIf { isHeavyWeight(it) }
          ?.background = Color(0x0, true)
      }
    }
    addHierarchyListener(listener)
    setOpaque(false)
    setForeground(Color.WHITE)
    setBackground(Color(0xC8_00_00_00.toInt(), true))
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5 + TRI_HEIGHT, 5))
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.height = 32
    return d
  }

  override fun paintComponent(g: Graphics) {
    val shape = createBalloonShape()
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = getBackground()
    g2.fill(shape)
    g2.dispose()
    super.paintComponent(g)
  }

  private fun createBalloonShape(): Shape {
    val w = getWidth() - 1.0
    val h = getHeight() - TRI_HEIGHT - 1.0
    val centerX = getWidth() * .5
    val triangle = Path2D.Double()
    triangle.moveTo(centerX - TRI_HEIGHT, h)
    triangle.lineTo(centerX, h + TRI_HEIGHT)
    triangle.lineTo(centerX + TRI_HEIGHT, h)
    val arc = 10.0
    val area = Area(RoundRectangle2D.Double(0.0, 0.0, w, h, arc, arc))
    area.add(Area(triangle))
    return area
  }

  companion object {
    private const val TRI_HEIGHT = 4
    private fun isHeavyWeight(w: Window): Boolean {
      val isHeavyWeight = w.type == Window.Type.POPUP
      val gc = w.graphicsConfiguration
      return gc != null && gc.isTranslucencyCapable && isHeavyWeight
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
