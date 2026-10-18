package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.plaf.basic.BasicTabbedPaneUI
import kotlin.math.abs
import kotlin.math.max

fun createUI(): Component {
  val tabs = object : JTabbedPane() {
    override fun updateUI() {
      super.updateUI()
      setUI(SpotlightTabbedPaneUI())
      isOpaque = true
      background = SpotlightTabbedPaneUI.BACKGROUND
      font = font.deriveFont(Font.PLAIN, 15f)
    }
  }
  listOf("File", "Home", "Data", "Help").forEach {
    tabs.addTab(it, createPanel(it))
  }
  tabs.setEnabledAt(2, false)
  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createPanel(title: String): Component {
  val label = JLabel(title, SwingConstants.CENTER)
  label.foreground = SpotlightTabbedPaneUI.INACTIVE_TEXT
  return JPanel(BorderLayout()).also {
    it.background = SpotlightTabbedPaneUI.BACKGROUND
    it.add(label)
  }
}

private class SpotlightTabbedPaneUI : BasicTabbedPaneUI() {
  private val timer = Timer(16) { stepSpotlight() }
  private var spotlight: Rectangle2D.Double? = null
  private var hoverIndex = -1
  private var hoverHandler: MouseAdapter? = null
  private var selectionHandler: ChangeListener? = null

  override fun installListeners() {
    super.installListeners()
    val handler = object : MouseAdapter() {
      override fun mouseMoved(e: MouseEvent) {
        setHoverIndex(tabForCoordinate(tabPane, e.x, e.y))
      }

      override fun mouseExited(e: MouseEvent) {
        setHoverIndex(-1)
      }
    }
    tabPane.addMouseListener(handler)
    tabPane.addMouseMotionListener(handler)
    hoverHandler = handler
    val listener = ChangeListener { timer.start() }
    tabPane.addChangeListener(listener)
    selectionHandler = listener
  }

  override fun uninstallListeners() {
    timer.stop()
    tabPane.removeMouseListener(hoverHandler)
    tabPane.removeMouseMotionListener(hoverHandler)
    tabPane.removeChangeListener(selectionHandler)
    hoverHandler = null
    selectionHandler = null
    super.uninstallListeners()
  }

  private fun setHoverIndex(index: Int) {
    if (hoverIndex != index) {
      hoverIndex = index
      val type = if (index >= 0) Cursor.HAND_CURSOR else Cursor.DEFAULT_CURSOR
      tabPane.cursor = Cursor.getPredefinedCursor(type)
      timer.start()
      tabPane.repaint()
    }
  }

  private fun getTabBounds(index: Int): Rectangle2D.Double? {
    val inRange = index >= 0 && index < tabPane.tabCount && index < rects.size
    return if (inRange) {
      val tab = rects[index]
      Rectangle2D.Double(
        (tab.x + MARGIN).toDouble(),
        (tab.y + MARGIN).toDouble(),
        tab.width - 2.0 * MARGIN,
        tab.height - 2.0 * MARGIN,
      )
    } else {
      null
    }
  }

  private fun getTargetBounds() =
    getTabBounds(if (hoverIndex >= 0) hoverIndex else tabPane.selectedIndex)

  private fun stepSpotlight() {
    val t = getTargetBounds()
    val s = spotlight
    if (t == null || s == null) {
      spotlight = t
      timer.stop()
    } else {
      val dx = t.x - s.x
      val dy = t.y - s.y
      val dw = t.width - s.width
      val dh = t.height - s.height
      val maxX = max(abs(dx), abs(dw))
      val maxY = max(abs(dy), abs(dh))
      if (max(maxX, maxY) < EPSILON) {
        s.setRect(t)
        timer.stop()
      } else {
        val sx = s.x + dx * SPEED
        val sy = s.y + dy * SPEED
        val sw = s.width + dw * SPEED
        val sh = s.height + dh * SPEED
        s.setRect(sx, sy, sw, sh)
      }
    }
    tabPane.repaint()
  }

  override fun paintTabArea(
    g: Graphics,
    tabPlacement: Int,
    selectedIndex: Int,
  ) {
    if (!timer.isRunning) {
      spotlight = getTargetBounds()
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    fillRoundRect(g2, getTabBounds(selectedIndex), SELECTED_FILL)
    fillRoundRect(g2, spotlight, SPOTLIGHT)
    g2.dispose()
    super.paintTabArea(g, tabPlacement, selectedIndex)
  }

  override fun paintText(
    g: Graphics,
    tabPlacement: Int,
    font: Font,
    metrics: FontMetrics,
    tabIndex: Int,
    title: String,
    textRect: Rectangle,
    isSelected: Boolean,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
    )
    g2.font = font
    g2.color = if (tabPane.isEnabledAt(tabIndex)) {
      val active = isSelected || tabIndex == hoverIndex
      if (active) ACTIVE_FOREGROUND else INACTIVE_TEXT
    } else {
      DISABLED_TEXT
    }
    g2.drawString(title, textRect.x, textRect.y + metrics.ascent)
    g2.dispose()
  }

  override fun paintTabBackground(
    g: Graphics,
    tabPlacement: Int,
    tabIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    isSelected: Boolean,
  ) {
    // Do not paint anything
  }

  override fun paintTabBorder(
    g: Graphics,
    tabPlacement: Int,
    tabIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    isSelected: Boolean,
  ) {
    // Do not paint anything
  }

  override fun paintFocusIndicator(
    g: Graphics,
    tabPlacement: Int,
    rects: Array<Rectangle>,
    tabIndex: Int,
    iconRect: Rectangle,
    textRect: Rectangle,
    isSelected: Boolean,
  ) {
    // Do not paint anything
  }

  override fun paintContentBorder(
    g: Graphics,
    tabPlacement: Int,
    selectedIndex: Int,
  ) {
    // Do not paint anything
  }

  override fun getTabInsets(
    tabPlacement: Int,
    tabIndex: Int,
  ) = Insets(6, 16, 6, 16)

  override fun getTabAreaInsets(tabPlacement: Int) = Insets(8, 8, 4, 8)

  override fun getSelectedTabPadInsets(tabPlacement: Int) = Insets(0, 0, 0, 0)

  override fun getContentBorderInsets(tabPlacement: Int) = Insets(0, 0, 0, 0)

  override fun getTabLabelShiftX(
    tabPlacement: Int,
    tabIndex: Int,
    isSelected: Boolean,
  ) = 0

  override fun getTabLabelShiftY(
    tabPlacement: Int,
    tabIndex: Int,
    isSelected: Boolean,
  ) = 0

  companion object {
    val BACKGROUND = Color(0x11_11_11)
    val INACTIVE_TEXT = Color(0x8A_8A_8A)
    private val SELECTED_FILL = Color(0x1E_1E_1E)
    private val SPOTLIGHT = Color(0x2A_2A_2A)
    private val ACTIVE_FOREGROUND = Color.WHITE
    private val DISABLED_TEXT = Color(0x55_55_55)
    private const val MARGIN = 2
    private const val ARC = 12.0
    private const val SPEED = .3
    private const val EPSILON = .5

    private fun fillRoundRect(
      g2: Graphics2D,
      r: Rectangle2D?,
      color: Color,
    ) {
      if (r != null) {
        g2.color = color
        g2.fill(RoundRectangle2D.Double(r.x, r.y, r.width, r.height, ARC, ARC))
      }
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
