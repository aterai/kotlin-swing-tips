package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import kotlin.math.roundToInt

fun createUI(): Component {
  val tabbedPane = createTabbedPane()
  val menu = JMenu("TabPlacement")
  val bg = ButtonGroup()
  TabPlacement.entries.forEach { tp ->
    val item = JRadioButtonMenuItem(tp.name, tp == TabPlacement.TOP)
    item.addActionListener { tabbedPane.tabPlacement = tp.placement }
    menu.add(item)
    bg.add(item)
  }
  return JPanel(BorderLayout(2, 2)).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    mb.add(menu)
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(tabbedPane)
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createTabbedPane(): JTabbedPane {
  val tabs = object : JTabbedPane(TOP, SCROLL_TAB_LAYOUT) {
    private var tip: BalloonToolTip? = null

    override fun getToolTipLocation(e: MouseEvent): Point? {
      val idx = indexAtLocation(e.x, e.y)
      val txt = if (idx >= 0) getToolTipTextAt(idx) else null
      return txt?.let {
        // ToolTipManager calls this before createToolTip() and setTipText(...),
        // so set the text here to get the size of the balloon for this tab.
        val t = getBalloonToolTip()
        t.tipText = it
        t.tailPlacement = tabPlacement
        getTipLocation(getBoundsAt(idx), t.preferredSize)
      }
    }

    // Place the tip of the tail at the center of the tab edge facing the content.
    private fun getTipLocation(
      tabRect: Rectangle,
      tipSize: Dimension,
    ) = when (tabPlacement) {
      LEFT -> createPoint(
        tabRect.maxX,
        tabRect.centerY - tipSize.getHeight() / 2.0,
      )

      RIGHT -> createPoint(
        tabRect.minX - tipSize.width,
        tabRect.centerY - tipSize.getHeight() / 2.0,
      )

      BOTTOM -> createPoint(
        tabRect.centerX - tipSize.getWidth() / 2.0,
        tabRect.minY - tipSize.height,
      )

      else -> createPoint(
        tabRect.centerX - tipSize.getWidth() / 2.0,
        tabRect.maxY,
      )
    }

    private fun createPoint(
      x: Double,
      y: Double,
    ) = Point(x.roundToInt(), y.roundToInt())

    override fun createToolTip(): JToolTip = getBalloonToolTip()

    // The JTabbedPane constructor calls updateUI() before the tip property is
    // initialized, so the tip is created here on demand instead of in updateUI()
    private fun getBalloonToolTip() = tip ?: BalloonToolTip().also {
      it.component = this
      tip = it
    }

    override fun updateUI() {
      // The cached tip is not a child of this pane, so discard it for the new LookAndFeel
      tip = null
      super.updateUI()
    }
  }
  tabs.addTab("000", ColorIcon(Color.RED), JScrollPane(JTree()), "00000")
  tabs.addTab("111", ColorIcon(Color.GREEN), JSplitPane(), "11111")
  tabs.addTab("222", ColorIcon(Color.BLUE), JScrollPane(JTable(5, 5)), "222")
  tabs.addTab("333", ColorIcon(Color.ORANGE), JLabel("6"), "33333333333333")
  tabs.addTab("444", ColorIcon(Color.CYAN), JLabel("7"), "4444444444444444444")
  tabs.addTab("555", ColorIcon(Color.PINK), JLabel("8"), "555555555555555555555")
  return tabs
}

private class BalloonToolTip : JToolTip() {
  // The text is painted by the JLabel instead of the ToolTipUI so that
  // the LookAndFeel (e.g. NimbusLookAndFeel) does not paint its own background
  private val label = JLabel("", SwingConstants.CENTER)
  private var listener: HierarchyListener? = null

  // The side of the balloon on which the tail is drawn:
  // one of SwingConstants.TOP, LEFT, BOTTOM or RIGHT
  var tailPlacement = SwingConstants.TOP
    set(placement) {
      if (field != placement) {
        field = placement
        repaint()
      }
    }

  init {
    LookAndFeel.installColorsAndFont(
      label,
      "ToolTip.background",
      "ToolTip.foreground",
      "ToolTip.font",
    )
    label.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
    layout = BorderLayout()
    add(label)
  }

  override fun updateUI() {
    removeHierarchyListener(listener)
    super.updateUI()
    listener = HierarchyListener { e ->
      val c = e.component
      val f = e.changeFlags.toInt() and HierarchyEvent.SHOWING_CHANGED != 0
      if (f && c.isShowing) {
        // Popup$HeavyWeightWindow: make the area outside the balloon transparent
        SwingUtilities
          .getWindowAncestor(c)
          ?.takeIf { isTranslucencyCapablePopup(it) }
          ?.background = Color(0x0, true)
      }
    }
    addHierarchyListener(listener)
    isOpaque = false
    // Leave room for the tail on every side
    border = BorderFactory.createEmptyBorder(TAIL_SIZE, TAIL_SIZE, TAIL_SIZE, TAIL_SIZE)
  }

  private fun isTranslucencyCapablePopup(w: Window) =
    w.graphicsConfiguration?.isTranslucencyCapable == true && w.type == Window.Type.POPUP

  override fun setTipText(tipText: String?) {
    super.setTipText(tipText)
    label.text = tipText
  }

  override fun getPreferredSize(): Dimension = layout.preferredLayoutSize(this)

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val balloon = createBalloonShape()
    g2.paint = background
    g2.fill(balloon)
    g2.paint = foreground
    g2.draw(balloon)
    g2.dispose()
    // super.paintComponent(g)
  }

  private fun createBalloonShape(): Shape {
    val i = insets
    // -1: keep the 1px outline inside the component bounds
    val w = width - i.left - i.right - 1.0
    val h = height - i.top - i.bottom - 1.0
    val cx = w / 2.0
    val cy = h / 2.0
    val tail = Path2D.Double()
    when (tailPlacement) {
      SwingConstants.LEFT -> {
        tail.moveTo(0.0, cy - TAIL_SIZE)
        tail.lineTo(-TAIL_SIZE.toDouble(), cy)
        tail.lineTo(0.0, cy + TAIL_SIZE)
      }

      SwingConstants.RIGHT -> {
        tail.moveTo(w, cy - TAIL_SIZE)
        tail.lineTo(w + TAIL_SIZE, cy)
        tail.lineTo(w, cy + TAIL_SIZE)
      }

      SwingConstants.BOTTOM -> {
        tail.moveTo(cx - TAIL_SIZE, h)
        tail.lineTo(cx, h + TAIL_SIZE)
        tail.lineTo(cx + TAIL_SIZE, h)
      }

      else -> {
        tail.moveTo(cx - TAIL_SIZE, 0.0)
        tail.lineTo(cx, -TAIL_SIZE.toDouble())
        tail.lineTo(cx + TAIL_SIZE, 0.0)
      }
    }
    val area = Area(RoundRectangle2D.Double(0.0, 0.0, w, h, ARC, ARC))
    area.add(Area(tail))
    val tx = i.left.toDouble()
    val ty = i.top.toDouble()
    val at = AffineTransform.getTranslateInstance(tx, ty)
    return at.createTransformedShape(area)
  }

  companion object {
    private const val TAIL_SIZE = 4
    private const val ARC = 4.0
  }
}

private class ColorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 2, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
}

private enum class TabPlacement(
  val placement: Int,
) {
  TOP(SwingConstants.TOP),
  BOTTOM(SwingConstants.BOTTOM),
  LEFT(SwingConstants.LEFT),
  RIGHT(SwingConstants.RIGHT),
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

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
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
