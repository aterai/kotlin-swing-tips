package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.geom.Ellipse2D
import javax.swing.*
import kotlin.math.roundToInt
import kotlin.math.sin

private const val BLINK_DELAY_MS = 30
private const val BLINK_PERIOD_MS = 1000L
private const val MIN_ALPHA_RATIO = .5f

fun createUI(): Component {
  val dotCount = 3
  val width = 32
  val height = 24
  val icon = BouncingDots(dotCount, width, height)
  val label = JLabel("Loading...", icon, SwingConstants.CENTER)
  label.setVerticalAlignment(SwingConstants.CENTER)
  label.setVerticalTextPosition(SwingConstants.BOTTOM)
  label.setHorizontalTextPosition(SwingConstants.CENTER)
  label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))

  val blinkStartTime = System.currentTimeMillis()
  val blinkTimer = Timer(BLINK_DELAY_MS) {
    updateBlinkColor(label, blinkStartTime)
  }
  blinkTimer.start()

  val menuBar = JMenuBar()
  menuBar.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(GridBagLayout()).also {
    EventQueue.invokeLater { it.rootPane.setJMenuBar(menuBar) }
    it.add(label)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateBlinkColor(label: JLabel, startTime: Long) {
  val elapsed = System.currentTimeMillis() - startTime
  val t = (sin(elapsed / BLINK_PERIOD_MS.toDouble() * 2 * Math.PI) + 1) / 2.0
  val ratio = (MIN_ALPHA_RATIO + t * (1 - MIN_ALPHA_RATIO)).toFloat()
  val uiColor = UIManager.getColor("Label.foreground")
  val foreground = uiColor ?: Color.GRAY
  val parent = SwingUtilities.getUnwrappedParent(label)
  label.setForeground(interpolateColor(parent.getBackground(), foreground, ratio))
}

private fun interpolateColor(from: Color, to: Color, ratio: Float) = Color(
  (from.red + (to.red - from.red) * ratio).roundToInt(),
  (from.green + (to.green - from.green) * ratio).roundToInt(),
  (from.blue + (to.blue - from.blue) * ratio).roundToInt(),
)

private class BouncingDots(
  dotCount: Int,
  width: Int,
  height: Int,
) : Icon {
  private val dotCount: Int
  private val iconWidth: Int
  private val iconHeight: Int
  private val timer = Timer(TIMER_DELAY_MS, RepaintAction())
  private var startTimeMillis = -1L
  private var attachedComponent: Component? = null
  private var hierarchyListener: HierarchyListener? = null

  init {
    require(dotCount > 0) { "dotCount must be positive: $dotCount" }
    this.dotCount = dotCount
    this.iconWidth = width
    this.iconHeight = height
    this.timer.setCoalesce(true)
  }

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    attachTo(c)

    if (startTimeMillis < 0) {
      startTimeMillis = System.currentTimeMillis()
    }
    if (c.isShowing() && !timer.isRunning) {
      timer.start()
    }

    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.translate(x, y)
    g2.color = resolveColor(c)

    val diameter = iconHeight * DOT_SIZE_RATIO
    val maxBounce = (iconHeight - diameter) / 2.0
    val gap = if (dotCount >
      0
    ) {
      iconWidth.toDouble() / dotCount
    } else {
      iconWidth.toDouble()
    }
    val elapsed = System.currentTimeMillis() - startTimeMillis
    val dot = Ellipse2D.Double()
    for (i in 0..<dotCount) {
      val phase = elapsed.toDouble() / PERIOD_MS + i * PHASE_OFFSET
      val t = (sin(phase * 2 * Math.PI) + 1) / 2.0
      val dy = maxBounce - t * maxBounce * 2
      val dotCenterX = gap * i + gap / 2.0
      val dotCenterY = iconHeight / 2.0 + dy
      val dotX = dotCenterX - diameter / 2.0
      val dotY = dotCenterY - diameter / 2.0
      dot.setFrame(dotX, dotY, diameter, diameter)
      g2.fill(dot)
    }
    g2.dispose()
  }

  override fun getIconWidth() = iconWidth

  override fun getIconHeight() = iconHeight

  private fun resolveColor(c: Component?) = c?.getForeground()
    ?: UIManager.getColor("Label.foreground")
    ?: Color.GRAY

  private fun attachTo(c: Component) {
    if (c != attachedComponent) {
      if (attachedComponent is Component && hierarchyListener != null) {
        attachedComponent?.removeHierarchyListener(hierarchyListener)
      }
      attachedComponent = c
      hierarchyListener = ShowingStateListener()
      c.addHierarchyListener(hierarchyListener)
    }
  }

  private inner class ShowingStateListener : HierarchyListener {
    override fun hierarchyChanged(e: HierarchyEvent) {
      val flags = e.getChangeFlags() and HierarchyEvent.SHOWING_CHANGED.toLong()
      if (flags != 0L && attachedComponent is Component) {
        val showing = attachedComponent?.isShowing() == true
        if (showing && !timer.isRunning) {
          timer.start()
        } else if (!showing) {
          timer.stop()
        }
      }
    }
  }

  private inner class RepaintAction : ActionListener {
    override fun actionPerformed(e: ActionEvent) {
      attachedComponent?.also {
        if (it.isShowing()) {
          it.repaint()
        } else {
          timer.stop()
        }
      }
    }
  }

  companion object {
    private const val TIMER_DELAY_MS = 30
    private const val PERIOD_MS = 1000L
    private const val PHASE_OFFSET = .2
    private const val DOT_SIZE_RATIO = .3
  }
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
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
