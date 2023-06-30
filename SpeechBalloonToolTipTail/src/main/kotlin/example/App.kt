package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*

fun makeUI(): Component {
  val tabbedPane = makeTabbedPane()
  val menu = JMenu("TabPlacement")
  val bg = ButtonGroup()
  val handler = ItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val tp = TabPlacement.valueOf(bg.selection.actionCommand)
      tabbedPane.tabPlacement = tp.placement
    }
  }
  TabPlacement.values().forEach {
    val name = it.name
    val selected = it == TabPlacement.TOP
    val item: JMenuItem = JRadioButtonMenuItem(name, selected)
    item.addItemListener(handler)
    item.actionCommand = name
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

private fun makeTabbedPane(): JTabbedPane {
  val tabs = object : JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT) {
    private var tip: BalloonToolTip? = null
    private val label = JLabel(" ", CENTER)

    override fun getToolTipLocation(e: MouseEvent): Point? {
      val idx = indexAtLocation(e.x, e.y)
      val txt = if (idx >= 0) getToolTipTextAt(idx) else null
      return txt?.let {
        val tips = createToolTip()
        tips.tipText = it
        label.text = it
        (tips as? BalloonToolTip)?.updateBalloonShape(getTabPlacement())
        getToolTipPoint(getBoundsAt(idx), tips.preferredSize)
      }
    }

    private fun getToolTipPoint(r: Rectangle, d: Dimension) = when (getTabPlacement()) {
      LEFT -> makePoint(r.maxX, r.centerY - d.getHeight() / 2.0)
      RIGHT -> makePoint(r.minX - d.width, r.centerY - d.getHeight() / 2.0)
      BOTTOM -> makePoint(r.centerX - d.getWidth() / 2.0, r.minY - d.height)
      else -> makePoint(r.centerX - d.getWidth() / 2.0, r.maxY + 8.0)
    }

    private fun makePoint(x: Double, y: Double) = Point((x + .5).toInt(), (y + .5).toInt())

    override fun createToolTip(): JToolTip {
      val t = tip ?: BalloonToolTip().also {
        LookAndFeel.installColorsAndFont(
          label,
          "ToolTip.background",
          "ToolTip.foreground",
          "ToolTip.font"
        )
        it.add(label)
        it.updateBalloonShape(getTabPlacement())
        it.component = this
      }
      tip = t
      return t
    }

    override fun updateUI() {
      tip = null
      super.updateUI()
    }
  }
  tabs.addTab("000", ColorIcon(Color.RED), JScrollPane(JTree()), "00000")
  tabs.addTab("111", ColorIcon(Color.GREEN), JScrollPane(JSplitPane()), "11111")
  tabs.addTab("222", ColorIcon(Color.BLUE), JScrollPane(JTable(5, 5)), "22222")
  tabs.addTab("333", ColorIcon(Color.ORANGE), JLabel("6"), "33333")
  tabs.addTab("444", ColorIcon(Color.CYAN), JLabel("7"), "44444")
  tabs.addTab("555", ColorIcon(Color.PINK), JLabel("8"), "55555")
  return tabs
}

private class BalloonToolTip : JToolTip() {
  private var listener: HierarchyListener? = null
  private var shape: Shape? = null

  override fun updateUI() {
    removeHierarchyListener(listener)
    super.updateUI()
    layout = BorderLayout()
    listener = HierarchyListener { e ->
      val c = e.component
      if (e.changeFlags.toInt() and HierarchyEvent.SHOWING_CHANGED != 0 && c.isShowing) {
        SwingUtilities.getWindowAncestor(c)
          ?.takeIf { it.type == Window.Type.POPUP }
          ?.background = Color(0x0, true)
      }
    }
    addHierarchyListener(listener)
    isOpaque = false
    border = BorderFactory.createEmptyBorder(SIZE, SIZE, SIZE, SIZE)
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.width += SIZE
    d.height += SIZE
    return d
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.color = getBackground()
    g2.fill(shape)
    g2.paint = getForeground()
    g2.draw(shape)
    g2.dispose()
    // super.paintComponent(g)
  }

  fun updateBalloonShape(placement: Int) {
    val i = insets
    val d = preferredSize
    val tail = Path2D.Double()
    val w = d.getWidth() - i.left - i.right - 1.0
    val h = d.getHeight() - i.top - i.bottom - 1.0
    val cx = w / 2.0
    val cy = h / 2.0
    when (placement) {
      SwingConstants.LEFT -> {
        tail.moveTo(0.0, cy - SIZE)
        tail.lineTo(-SIZE.toDouble(), cy)
        tail.lineTo(0.0, cy + SIZE)
      }
      SwingConstants.RIGHT -> {
        tail.moveTo(w, cy - SIZE)
        tail.lineTo(w + SIZE, cy)
        tail.lineTo(w, cy + SIZE)
      }
      SwingConstants.BOTTOM -> {
        tail.moveTo(cx - SIZE, h)
        tail.lineTo(cx, h + SIZE)
        tail.lineTo(cx + SIZE, h)
      }
      else -> {
        tail.moveTo(cx - SIZE, 0.0)
        tail.lineTo(cx, -SIZE.toDouble())
        tail.lineTo(cx + SIZE, 0.0)
      }
    }
    val area = Area(RoundRectangle2D.Double(0.0, 0.0, w, h, ARC, ARC))
    area.add(Area(tail))
    val at = AffineTransform.getTranslateInstance(i.left.toDouble(), i.top.toDouble())
    shape = at.createTransformedShape(area)
  }

  companion object {
    private const val SIZE = 4
    private const val ARC = 4.0
  }
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: java.awt.Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 2, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
}

private enum class TabPlacement(val placement: Int) {
  TOP(SwingConstants.TOP),
  BOTTOM(SwingConstants.BOTTOM),
  LEFT(SwingConstants.LEFT),
  RIGHT(SwingConstants.RIGHT)
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
    UnsupportedLookAndFeelException::class
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
