package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.ItemEvent
import java.awt.event.MouseEvent
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tabbedPane = makeTabbedPane()
  val cb = JComboBox(TabPlacement.values())
  cb.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      tabbedPane.tabPlacement = cb.getItemAt(cb.selectedIndex).placement
    }
  }
  return JPanel(BorderLayout(2, 2)).also {
    it.add(cb, BorderLayout.NORTH)
    it.add(tabbedPane)
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabbedPane(): JTabbedPane {
  val tabs = object : JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT) {
    private var tip: BalloonToolTip? = null

    override fun getToolTipLocation(e: MouseEvent): Point? {
      val idx = indexAtLocation(e.x, e.y)
      val txt = if (idx >= 0) getToolTipTextAt(idx) else null
      return txt?.let {
        val tips = createToolTip()
        tips.tipText = it
        (tips as? BalloonToolTip)?.updateBalloonShape(getTabPlacement())
        getToolTipPoint(getBoundsAt(idx), tips.preferredSize)
      }
    }

    private fun getToolTipPoint(r: Rectangle, d: Dimension) = when (getTabPlacement()) {
      LEFT -> makePoint(r.maxX, r.centerY - d.getHeight() / 2.0)
      RIGHT -> makePoint(r.minX - d.width, r.centerY - d.getHeight() / 2.0)
      BOTTOM -> makePoint(r.centerX - d.getWidth() / 2.0, r.minY - d.height)
      else -> makePoint(r.centerX - d.getWidth() / 2.0, r.maxY)
    }

    private fun makePoint(x: Double, y: Double) = Point((x + .5).toInt(), (y + .5).toInt())

    override fun createToolTip(): JToolTip {
      val t = tip ?: BalloonToolTip().also {
        it.updateBalloonShape(getTabPlacement())
        it.component = this
      }
      tip = t
      return t
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
  }

  override fun getPreferredSize() = super.getPreferredSize()?.also { it.height = 24 }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.color = getBackground()
    g2.fill(shape)
    g2.paint = getForeground()
    g2.draw(shape)
    g2.dispose()
    super.paintComponent(g)
  }

  fun updateBalloonShape(placement: Int) {
    val r = visibleRect
    r.size = preferredSize ?: Dimension()
    val tail = Path2D.Double()
    var w = r.getWidth() - 1.0
    var h = r.getHeight() - 1.0
    val arc = 10.0
    val bubble = when (placement) {
      SwingConstants.LEFT -> {
        border = BorderFactory.createEmptyBorder(2, 2 + SIZE, 2, 2)
        tail.moveTo(r.minX + SIZE, r.centerY - SIZE)
        tail.lineTo(r.minX, r.centerY)
        tail.lineTo(r.minX + SIZE, r.centerY + SIZE)
        w -= SIZE.toDouble()
        RoundRectangle2D.Double(r.minX + SIZE, r.minY, w, h, arc, arc)
      }
      SwingConstants.RIGHT -> {
        border = BorderFactory.createEmptyBorder(2, 2, 2, 2 + SIZE)
        tail.moveTo(r.maxX - SIZE - 1.0, r.centerY - SIZE)
        tail.lineTo(r.maxX, r.centerY)
        tail.lineTo(r.maxX - SIZE - 1.0, r.centerY + SIZE)
        w -= SIZE.toDouble()
        RoundRectangle2D.Double(r.minX, r.minY, w, h, arc, arc)
      }
      SwingConstants.BOTTOM -> {
        border = BorderFactory.createEmptyBorder(2, 2, 2 + SIZE, 2)
        tail.moveTo(r.centerX - SIZE, r.maxY - SIZE - 1.0)
        tail.lineTo(r.centerX, r.maxY)
        tail.lineTo(r.centerX + SIZE, r.maxY - SIZE - 1.0)
        h -= SIZE.toDouble()
        RoundRectangle2D.Double(r.minX, r.minY, w, h, arc, arc)
      }
      else -> {
        border = BorderFactory.createEmptyBorder(2 + SIZE, 2, 2, 2)
        tail.moveTo(r.centerX - SIZE, r.minY + SIZE)
        tail.lineTo(r.centerX, r.minY)
        tail.lineTo(r.centerX + SIZE, r.minY + SIZE)
        h -= SIZE.toDouble()
        RoundRectangle2D.Double(r.minX, r.minY + SIZE, w, h, arc, arc)
      }
    }
    val area = Area(bubble)
    area.add(Area(tail))
    shape = area
  }

  companion object {
    private const val SIZE = 4
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
