package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.geom.Ellipse2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer

fun makeUI(): Component {
  val l1 = object : JLabel("Timer Animated ToolTip") {
    override fun createToolTip(): JToolTip {
      val tip = AnimatedToolTip(AnimatedLabel(""))
      tip.component = this
      return tip
    }
  }
  l1.toolTipText = "Test1"

  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/anime.gif")
  val l2 = object : JLabel("Gif Animated ToolTip") {
    override fun createToolTip(): JToolTip {
      val label = JLabel("", ImageIcon(url), SwingConstants.LEFT)
      val tip = AnimatedToolTip(label)
      tip.component = this
      return tip
    }
  }
  l2.toolTipText = "Test2"

  val l3 = JLabel("Gif Animated ToolTip(html)")
  l3.toolTipText = String.format("<html><img src='%s'>Test3</html>", url)

  val p1 = JPanel(BorderLayout())
  p1.border = BorderFactory.createTitledBorder("javax.swing.Timer")
  p1.add(l1)

  val p2 = JPanel(BorderLayout())
  p2.border = BorderFactory.createTitledBorder("Animated Gif")
  p2.add(l2, BorderLayout.NORTH)
  p2.add(l3, BorderLayout.SOUTH)

  val box = Box.createVerticalBox()
  box.add(p1)
  box.add(Box.createVerticalStrut(20))
  box.add(p2)
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class AnimatedToolTip(private val iconLabel: JLabel?) : JToolTip() {
  override fun getPreferredSize(): Dimension? = layout.preferredLayoutSize(this)

  override fun setTipText(tipText: String) {
    val oldValue = iconLabel?.text
    iconLabel?.text = tipText
    firePropertyChange("tiptext", oldValue, tipText)
  }

  override fun getTipText() = iconLabel?.text ?: ""

  init {
    LookAndFeel.installColorsAndFont(iconLabel, "ToolTip.background", "ToolTip.foreground", "ToolTip.font")
    iconLabel?.isOpaque = true
    layout = BorderLayout()
    add(iconLabel)
  }
}

private class AnimatedLabel(title: String?) : JLabel(title) {
  @Transient
  private val icon = AnimeIcon()
  private val animator = Timer(100) {
    icon.next()
    repaint()
  }

  private fun startAnimation() {
    icon.setRunning(true)
    animator.start()
  }

  private fun stopAnimation() {
    icon.setRunning(false)
    animator.stop()
  }

  init {
    isOpaque = true
    setIcon(icon)
    addHierarchyListener { e ->
      if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
        if (e.component.isShowing) {
          startAnimation()
        } else {
          stopAnimation()
        }
      }
    }
  }
}

private class AnimeIcon : Icon {
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
      // list.add(list.remove(0));
      java.util.Collections.rotate(list, 1)
    }
  }

  fun setRunning(running: Boolean) {
    this.running = running
  }

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = c.background ?: Color.WHITE
    g2.fillRect(x, y, iconWidth, iconHeight)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = ELLIPSE_COLOR
    g2.translate(x, y)
    val size = list.size.toFloat()
    list.forEach {
      val alpha = if (running) (list.indexOf(it) + 1) / size else .5f
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
      g2.fill(it)
    }
    g2.dispose()
  }

  override fun getIconWidth() = WIDTH

  override fun getIconHeight() = HEIGHT

  companion object {
    private val ELLIPSE_COLOR = Color(0x80_80_80)
    private const val R = 2.0
    private const val SX = 1.0
    private const val SY = 1.0
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
