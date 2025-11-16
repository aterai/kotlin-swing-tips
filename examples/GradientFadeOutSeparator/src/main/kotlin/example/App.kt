package example

import java.awt.*
import java.awt.geom.Point2D
import javax.swing.*
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.basic.BasicSeparatorUI
import kotlin.math.max

fun makeUI(): Component {
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.setJMenuBar(mb) }
    it.add(makeVerticalBox(), BorderLayout.NORTH)
    it.add(makeHorizontalBox(), BorderLayout.EAST)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeVerticalBox(): Box {
  val box = Box.createVerticalBox()
  box.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
  box.add(Box.createVerticalStrut(10))
  box.add(JSeparator(SwingConstants.HORIZONTAL))
  box.add(Box.createVerticalStrut(10))
  box.add(JLabel("↑ Default JSeparator"))
  box.add(Box.createVerticalStrut(20))
  box.add(GradientFadeOutSeparator(SwingConstants.HORIZONTAL))
  box.add(Box.createVerticalStrut(10))
  box.add(JLabel("↑ GradientFadeOutSeparator"))
  box.add(Box.createVerticalStrut(10))
  return box
}

private fun makeHorizontalBox(): Box {
  val box = Box.createHorizontalBox()
  box.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
  box.add(Box.createHorizontalStrut(10))
  box.add(JSeparator(SwingConstants.VERTICAL))
  box.add(Box.createHorizontalStrut(10))
  box.add(GradientFadeOutSeparator(SwingConstants.VERTICAL))
  box.add(Box.createHorizontalStrut(10))
  return box
}

private class GradientFadeOutSeparator(
  orientation: Int,
) : JSeparator(orientation) {
  override fun updateUI() {
    super.updateUI()
    setUI(GradientSeparatorUI())
  }
}

private class GradientSeparatorUI : BasicSeparatorUI() {
  private var backgroundColor: Color? = null
  private var shadowColor: Color? = null
  private var highlightColor: Color? = null

  private fun updateColors(c: Component) {
    val bgc = c.getBackground()
    val c1 = UIManager.getColor("Panel.background")
    backgroundColor = c1 as? ColorUIResource ?: bgc
    val c2 = UIManager.getColor("Separator.shadow")
    shadowColor = c2 as? ColorUIResource ?: bgc.darker()
    val c3 = UIManager.getColor("Separator.highlight")
    highlightColor = c3 as? ColorUIResource ?: bgc.brighter()
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    updateColors(c)
  }

  override fun paint(g: Graphics, c: JComponent?) {
    if (c is JSeparator) {
      val g2 = g.create() as? Graphics2D ?: return
      val r = SwingUtilities.calculateInnerArea(c, null)
      val centerX = r.centerX.toFloat()
      val centerY = r.centerY.toFloat()
      val center = Point2D.Float(centerX, centerY)
      val radius = max(r.width, r.height).toFloat()
      val dist = floatArrayOf(.1f, .6f)
      val colors1 = arrayOf(shadowColor, backgroundColor)
      val p1 = RadialGradientPaint(center, radius, dist, colors1)
      val colors2 = arrayOf(highlightColor, backgroundColor)
      val p2 = RadialGradientPaint(center, radius, dist, colors2)
      if (c.orientation == SwingConstants.HORIZONTAL) {
        g2.paint = p1
        g2.fillRect(0, 0, r.width, 1)
        g2.paint = p2
        g2.fillRect(0, 1, r.width, 1)
      } else {
        g2.paint = p1
        g2.fillRect(0, 0, 1, r.height)
        g2.paint = p2
        g2.fillRect(1, 0, 1, r.height)
      }
      g2.dispose()
    }
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
