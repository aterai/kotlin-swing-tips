package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseWheelEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicScrollBarUI
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel

fun makeUI() = JPanel(GridLayout(2, 1)).also {
  it.add(makeOverlayTabbedPane())
  it.add(makeCardLayoutTabbedPane())
  it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  it.preferredSize = Dimension(320, 240)
}

private fun makeCardLayoutTabbedPane(): Component {
  val tabs = CardLayoutTabbedPane()
  tabs.addTab("1:JTree", ColorIcon(Color.RED), JScrollPane(JTree()))
  tabs.addTab("2:JTable", ColorIcon(Color.GREEN), JScrollPane(JTable(10, 3)))
  tabs.addTab("3:JTextArea", ColorIcon(Color.BLUE), JScrollPane(JTextArea()))
  tabs.addTab("4:JButton", ColorIcon(Color.CYAN), JButton("JButton"))
  tabs.addTab("5:JCheckBox", ColorIcon(Color.ORANGE), JCheckBox("JCheckBox"))
  tabs.addTab("6:JRadioButton", ColorIcon(Color.PINK), JRadioButton("JRadioButton"))
  tabs.addTab("7:JSplitPane", ColorIcon(Color.YELLOW), JSplitPane())
  EventQueue.invokeLater { tabs.tabArea.horizontalScrollBar.isVisible = false }
  return tabs
}

private fun makeOverlayTabbedPane(): Component {
  val buttonSize = 20
  val tabs = ClippedTitleTabbedPane()
  tabs.addTab("1:JTree", ColorIcon(Color.RED), JScrollPane(JTree()))
  tabs.addTab("2:JTable", ColorIcon(Color.GREEN), JScrollPane(JTable(10, 3)))
  tabs.addTab("3:JTextArea", ColorIcon(Color.BLUE), JScrollPane(JTextArea()))
  tabs.addTab("4:JButton", ColorIcon(Color.CYAN), JButton("JButton"))
  tabs.addTab("5:JCheckBox", ColorIcon(Color.ORANGE), JCheckBox("JCheckBox"))

  val button = object : JButton("⊽") {
    private var handler: MouseListener? = null

    override fun updateUI() {
      removeMouseListener(handler)
      super.updateUI()
      isFocusable = false
      isContentAreaFilled = false
      isFocusPainted = false
      border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
      handler = object : MouseAdapter() {
        override fun mouseEntered(e: MouseEvent) {
          isContentAreaFilled = true
        }

        override fun mouseExited(e: MouseEvent) {
          isContentAreaFilled = false
        }
      }
      addMouseListener(handler)
    }

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = buttonSize
      return d
    }

    override fun getAlignmentY() = TOP_ALIGNMENT
  }
  button.addActionListener { e ->
    val b = e.source as? JButton ?: button
    val p = b.location
    val popup = JPopupMenu()
    val selected = tabs.selectedIndex
    for (i in 0 until tabs.tabCount) {
      popup.add(makeRadioMenuItem(tabs, i, selected))
    }
    p.x += b.width - popup.preferredSize.width - 1
    p.y += b.height
    popup.show(b.parent, p.x, p.y)
  }
  val box = Box.createHorizontalBox()
  box.alignmentX = Component.LEFT_ALIGNMENT
  box.add(Box.createHorizontalGlue())
  box.add(button)

  val p = JPanel()
  p.layout = OverlayLayout(p)
  p.add(box)
  p.add(tabs)
  return p
}

private fun makeRadioMenuItem(tabs: JTabbedPane, i: Int, selected: Int): JMenuItem {
  val b = JRadioButtonMenuItem(tabs.getTitleAt(i), i == selected)
  b.addActionListener { tabs.selectedIndex = i }
  return b
}

private class CardLayoutTabbedPane : JPanel(BorderLayout()) {
  private val cardLayout = CardLayout()
  private val tabPanel = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0))
  private val contentsPanel = JPanel(cardLayout)
  private val hiddenTabs = object : JButton("⊽") {
    private var handler: MouseListener? = null

    override fun updateUI() {
      removeMouseListener(handler)
      super.updateUI()
      isFocusable = false
      isContentAreaFilled = false
      isFocusPainted = false
      border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
      handler = object : MouseAdapter() {
        override fun mouseEntered(e: MouseEvent) {
          isContentAreaFilled = true
        }

        override fun mouseExited(e: MouseEvent) {
          isContentAreaFilled = false
        }
      }
      addMouseListener(handler)
    }

    override fun getAlignmentY() = TOP_ALIGNMENT
  }
  private val group = ButtonGroup()
  val tabArea = object : JScrollPane(tabPanel) {
    override fun isOptimizedDrawingEnabled() = false // JScrollBar is overlap

    override fun updateUI() {
      super.updateUI()
      EventQueue.invokeLater {
        getVerticalScrollBar().ui = OverlappedScrollBarUI()
        getHorizontalScrollBar().ui = OverlappedScrollBarUI()
        layout = OverlapScrollPaneLayout()
        setComponentZOrder(getVerticalScrollBar(), 0)
        setComponentZOrder(getHorizontalScrollBar(), 1)
        setComponentZOrder(getViewport(), 2)
      }
      setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
      setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS)
      getVerticalScrollBar().isOpaque = false
      getHorizontalScrollBar().isOpaque = false
      background = Color.DARK_GRAY
      viewportBorder = BorderFactory.createEmptyBorder()
      border = BorderFactory.createEmptyBorder()
    }

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = TABAREA_SIZE
      return d
    }
  }

  init {
    border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    background = Color(16, 16, 16)
    tabPanel.inheritsPopupMenu = true
    hiddenTabs.addActionListener { e ->
      val b = e.source as? JButton ?: hiddenTabs
      val p = b.location
      val popup = JPopupMenu()
      for (i in 0 until tabPanel.componentCount) {
        val mi = makeRadioMenuItem(tabPanel, i)
        if (mi != null) {
          popup.add(mi)
        }
      }
      p.x += b.width - popup.preferredSize.width - 1
      p.y += b.height
      popup.show(b.parent, p.x, p.y)
    }
    val buttons = JPanel(GridBagLayout())
    buttons.add(hiddenTabs)
    val header = JPanel(BorderLayout())
    header.add(JLayer(tabArea, HorizontalScrollLayerUI()))
    header.add(buttons, BorderLayout.EAST)
    add(header, BorderLayout.NORTH)
    add(contentsPanel)
  }

  private fun makeRadioMenuItem(c: Container, i: Int): JMenuItem? {
    val tab = c.getComponent(i)
    val viewport = tabArea.viewport
    val r = tab.bounds
    if (tab !is JToggleButton || viewport.viewRect.contains(r)) {
      return null
    }
    val title = (tab.getComponent(0) as? JLabel)?.text ?: ""
    val mi = JRadioButtonMenuItem(title)
    mi.addActionListener {
      tab.isSelected = true
      cardLayout.show(contentsPanel, title)
      viewport.scrollRectToVisible(SwingUtilities.convertRectangle(c, r, viewport))
    }
    return mi
  }

  override fun doLayout() {
    val m = tabArea.horizontalScrollBar.model
    hiddenTabs.isVisible = m.maximum - m.extent > 0
    super.doLayout()
  }

  private fun createTabComponent(title: String, icon: Icon, comp: Component): Component {
    val tab = TabButton()
    tab.inheritsPopupMenu = true
    group.add(tab)
    tab.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) {
          (e.component as? AbstractButton)?.isSelected = true
          cardLayout.show(contentsPanel, title)
        }
      }
    })
    EventQueue.invokeLater { tab.isSelected = true }
    val label = JLabel(title, icon, SwingConstants.LEADING)
    label.foreground = Color.WHITE
    label.icon = icon
    label.isOpaque = false
    val close = object : JButton(CloseTabIcon(Color(0xB0_B0_B0))) {
      override fun getPreferredSize() = Dimension(12, 12)
    }
    close.addActionListener {
      tabPanel.remove(tab)
      contentsPanel.remove(comp)
      val oneOrMore = tabPanel.componentCount > 1
      if (oneOrMore) {
        tabPanel.revalidate()
        (tabPanel.getComponent(0) as? TabButton)?.isSelected = true
        cardLayout.first(contentsPanel)
      }
      tabPanel.revalidate()
    }
    close.border = BorderFactory.createEmptyBorder()
    close.isFocusable = false
    close.isOpaque = false
    close.isContentAreaFilled = false
    close.pressedIcon = CloseTabIcon(Color(0xFE_FE_FE))
    close.rolloverIcon = CloseTabIcon(Color(0xA0_A0_A0))
    tab.add(label)
    tab.add(close, BorderLayout.EAST)
    return tab
  }

  fun addTab(title: String, icon: Icon, comp: Component) {
    val tab = createTabComponent(title, icon, comp)
    tabPanel.add(tab)
    contentsPanel.add(comp, title)
    cardLayout.show(contentsPanel, title)
    EventQueue.invokeLater { tabPanel.scrollRectToVisible(tab.bounds) }
  }

  companion object {
    const val TABAREA_SIZE = 28
  }
}

private class TabButton : JToggleButton() {
  private val emptyBorder = BorderFactory.createEmptyBorder(2, 4, 4, 4)
  private val selectedBorder = BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(0, 0, 3, 0, Color(0xFA_00_AA_FF.toInt(), true)),
    BorderFactory.createEmptyBorder(2, 4, 1, 4),
  )
  private val pressedColor = Color(32, 32, 32)
  private val selectedColor = Color(48, 32, 32)
  private val rolloverColor = Color(48, 48, 48)

  override fun updateUI() {
    super.updateUI()
    layout = BorderLayout()
    border = BorderFactory.createEmptyBorder(2, 4, 4, 4)
    isContentAreaFilled = false
    isFocusPainted = false
    isOpaque = true
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.height = CardLayoutTabbedPane.TABAREA_SIZE
    return d
  }

  override fun fireStateChanged() {
    val model = getModel()
    if (model.isEnabled) {
      if (model.isPressed || model.isArmed) {
        background = pressedColor
        border = selectedBorder
      } else if (isSelected) {
        background = selectedColor
        border = selectedBorder
      } else if (isRolloverEnabled && model.isRollover) {
        background = rolloverColor
        border = emptyBorder
      } else {
        background = Color.GRAY
        border = emptyBorder
      }
    } else {
      background = Color.GRAY
      border = emptyBorder
    }
    super.fireStateChanged()
  }
}

private class CloseTabIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.translate(x, y)
    g2.paint = color
    g2.drawLine(3, 3, 9, 9)
    g2.drawLine(9, 3, 3, 9)
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.translate(x, y)
    g2.paint = color
    g2.fillOval(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
}

private class OverlapScrollPaneLayout : ScrollPaneLayout() {
  override fun layoutContainer(parent: Container) {
    if (parent is JScrollPane) {
      val availR = SwingUtilities.calculateInnerArea(parent, null)
      if (colHead != null && colHead.isVisible) {
        val colHeadR = Rectangle(0, availR.y, 0, 0)
        val colHeadHeight = availR.height.coerceAtMost(colHead.preferredSize.height)
        colHeadR.height = colHeadHeight
        availR.y += colHeadHeight
        availR.height -= colHeadHeight
        colHeadR.width = availR.width
        colHeadR.x = availR.x
        colHead.bounds = colHeadR
      }
      viewport?.bounds = availR
      vsb?.also {
        it.setLocation(availR.x + availR.width - BAR_SIZE, availR.y)
        it.setSize(BAR_SIZE, availR.height - BAR_SIZE)
      }
      hsb?.also {
        it.setLocation(availR.x, availR.y + availR.height - BAR_SIZE)
        it.setSize(availR.width, BAR_SIZE)
      }
    }
  }

  companion object {
    private const val BAR_SIZE = 5
  }
}

private class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

private class OverlappedScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  override fun paintTrack(g: Graphics, c: JComponent, r: Rectangle) {
    // empty paint
  }

  override fun paintThumb(g: Graphics, c: JComponent, r: Rectangle) {
    if (c.isEnabled && !r.isEmpty) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = DEFAULT_COLOR
      g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
      g2.dispose()
    }
  }

  companion object {
    private val DEFAULT_COLOR = Color(0xAA_16_32_64.toInt(), true)
  }
}

private class HorizontalScrollLayerUI : LayerUI<JScrollPane>() {
  private var isDragging = false

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or
      AWTEvent.MOUSE_MOTION_EVENT_MASK or AWTEvent.MOUSE_WHEEL_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val hsb = l.view.horizontalScrollBar
    when (e.id) {
      MouseEvent.MOUSE_ENTERED -> hsb.isVisible = true
      MouseEvent.MOUSE_EXITED -> if (!isDragging) {
        hsb.isVisible = false
      }
      MouseEvent.MOUSE_RELEASED -> if (isDragging) {
        isDragging = false
        hsb.isVisible = false
      }
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    if (e.id == MouseEvent.MOUSE_DRAGGED) {
      isDragging = true
    }
  }

  override fun processMouseWheelEvent(e: MouseWheelEvent, l: JLayer<out JScrollPane>) {
    val scroll = l.view
    val hsb = scroll.horizontalScrollBar
    val viewport = scroll.viewport
    val vp = viewport.viewPosition
    vp.translate(hsb.blockIncrement * e.wheelRotation, 0)
    (SwingUtilities.getUnwrappedView(viewport) as? JComponent)?.also {
      it.scrollRectToVisible(Rectangle(vp, viewport.size))
    }
  }
}

open class ClippedTitleTabbedPane : JTabbedPane() {
  override fun updateUI() {
    val key = "TabbedPane.tabAreaInsets"
    UIManager.put(key, null) // uninstall
    super.updateUI()
    val tabInsets = UIManager.getInsets("TabbedPane.tabInsets") ?: getSynthTabInsets()
    val tabAreaInsets = UIManager.getInsets(key) ?: getSynthTabAreaInsets()
    val fm = getFontMetrics(font)
    val buttonSize = 20
    val tih = buttonSize - fm.height - tabInsets.top - tabInsets.bottom - tabAreaInsets.bottom
    val ins = Insets(
      tabAreaInsets.top.coerceAtLeast(tih),
      tabAreaInsets.left,
      tabAreaInsets.bottom,
      tabAreaInsets.left + buttonSize,
    )
    UIManager.put(key, ins)
    super.updateUI() // reinstall
  }

  override fun getAlignmentX() = LEFT_ALIGNMENT

  override fun getAlignmentY() = TOP_ALIGNMENT

  private fun getSynthTabInsets(): Insets {
    val style = SynthLookAndFeel.getStyle(this, Region.TABBED_PANE_TAB)
    val context = SynthContext(this, Region.TABBED_PANE_TAB, style, SynthConstants.ENABLED)
    return style.getInsets(context, null)
  }

  private fun getSynthTabAreaInsets(): Insets {
    val style = SynthLookAndFeel.getStyle(this, Region.TABBED_PANE_TAB_AREA)
    val context = SynthContext(this, Region.TABBED_PANE_TAB_AREA, style, SynthConstants.ENABLED)
    return style.getInsets(context, null)
  }

  override fun doLayout() {
    val tabCount = tabCount
    if (tabCount == 0 || !isVisible) {
      super.doLayout()
      return
    }
    val tabInsets = UIManager.getInsets("TabbedPane.tabInsets") ?: getSynthTabInsets()
    val tabAreaInsets = UIManager.getInsets("TabbedPane.tabAreaInsets") ?: getSynthTabAreaInsets()
    val ins = insets
    val tabPlacement = getTabPlacement()
    val areaWidth = width - tabAreaInsets.left - tabAreaInsets.right - ins.left - ins.right
    val isSide = tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT
    var tabWidth = if (isSide) areaWidth / 4 else areaWidth / tabCount
    val gap = if (isSide) 0 else areaWidth - tabWidth * tabCount

    // "3" is magic number @see BasicTabbedPaneUI#calculateTabWidth
    tabWidth -= tabInsets.left + tabInsets.right + 3
    updateAllTabWidth(tabWidth, gap)

    super.doLayout()
  }

  override fun insertTab(title: String?, icon: Icon?, c: Component?, tip: String?, index: Int) {
    super.insertTab(title, icon, c, tip ?: title, index)
    setTabComponentAt(index, JLabel(title, icon, SwingConstants.CENTER))
  }

  private fun updateAllTabWidth(tabWidth: Int, gap: Int) {
    val dim = Dimension()
    var rest = gap
    for (i in 0 until tabCount) {
      val tab = getTabComponentAt(i) as? JComponent ?: continue
      val a = if (i == tabCount - 1) rest else 1
      val w = if (rest > 0) tabWidth + a else tabWidth
      dim.setSize(w, tab.preferredSize.height)
      tab.preferredSize = dim
      rest -= a
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
