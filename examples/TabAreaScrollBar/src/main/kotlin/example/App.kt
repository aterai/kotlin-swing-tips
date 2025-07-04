package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicScrollBarUI

private const val TABAREA_SIZE = 28

fun makeUI(): Component {
  val tabs = CardLayoutTabbedPane()
  tabs.addTab("JTree", ColorIcon(Color.RED), JScrollPane(JTree()))
  tabs.addTab("JTable", ColorIcon(Color.GREEN), JScrollPane(JTable(10, 3)))
  tabs.addTab("JTextArea", ColorIcon(Color.BLUE), JScrollPane(JTextArea()))
  tabs.addTab("JButton", ColorIcon(Color.CYAN), JButton("JButton"))
  tabs.addTab("JCheckBox", ColorIcon(Color.ORANGE), JCheckBox("JCheckBox"))
  tabs.addTab("JRadioButton", ColorIcon(Color.PINK), JRadioButton("JRadioButton"))
  tabs.addTab("JSplitPane", ColorIcon(Color.YELLOW), JSplitPane())
  EventQueue.invokeLater { tabs.tabArea.horizontalScrollBar.isVisible = false }
  val popup = JPopupMenu()
  popup.add("test: add")
  popup.add("test: delete")
  tabs.tabArea.componentPopupMenu = popup
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(tabs)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class CardLayoutTabbedPane : JPanel(BorderLayout()) {
  private val cardLayout = CardLayout()
  private val tabPanel = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0))
  private val contentsPanel = JPanel(cardLayout)
  private val hiddenTabs = JButton("⊽")
  private val group = ButtonGroup()
  val tabArea = object : JScrollPane(tabPanel) {
    override fun isOptimizedDrawingEnabled() = false // JScrollBar is overlap

    override fun updateUI() {
      super.updateUI()
      verticalScrollBar.setUI(OverlappedScrollBarUI())
      verticalScrollBar.isOpaque = false
      horizontalScrollBar.setUI(OverlappedScrollBarUI())
      horizontalScrollBar.isOpaque = false
      layout = OverlapScrollPaneLayout()
      setComponentZOrder(verticalScrollBar, 0)
      setComponentZOrder(horizontalScrollBar, 1)
      setComponentZOrder(viewport, 2)
      verticalScrollBarPolicy = VERTICAL_SCROLLBAR_NEVER
      horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_ALWAYS
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
    hiddenTabs.font = hiddenTabs.font.deriveFont(8f)
    hiddenTabs.border = BorderFactory.createEmptyBorder(2, 8, 2, 8)
    hiddenTabs.isOpaque = false
    hiddenTabs.isFocusable = false
    hiddenTabs.isContentAreaFilled = false
    val header = JPanel(BorderLayout())
    header.add(JLayer(tabArea, HorizontalScrollLayerUI()))
    header.add(hiddenTabs, BorderLayout.EAST)
    add(header, BorderLayout.NORTH)
    add(contentsPanel)
  }

  override fun doLayout() {
    val m = tabArea.horizontalScrollBar.model
    hiddenTabs.isVisible = m.maximum - m.extent > 0
    super.doLayout()
  }

  private fun createTabComponent(
    title: String,
    icon: Icon,
    comp: Component,
  ): Component {
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

  init {
    layout = BorderLayout()
  }

  override fun updateUI() {
    super.updateUI()
    border = BorderFactory.createEmptyBorder(2, 4, 4, 4)
    isContentAreaFilled = false
    isFocusPainted = false
    isOpaque = true
  }

  override fun setLayout(mgr: LayoutManager?) {
    super.setLayout(mgr)
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.height = TABAREA_SIZE
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

private class CloseTabIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.translate(x, y)
    g2.paint = color
    g2.drawLine(3, 3, 9, 9)
    g2.drawLine(9, 3, 3, 9)
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
}

private class ColorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
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

  override fun paintTrack(
    g: Graphics,
    c: JComponent,
    r: Rectangle,
  ) {
    // empty paint
  }

  override fun paintThumb(
    g: Graphics,
    c: JComponent,
    r: Rectangle,
  ) {
    if (c.isEnabled && !r.isEmpty) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
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

  override fun processMouseEvent(
    e: MouseEvent,
    l: JLayer<out JScrollPane>,
  ) {
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

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out JScrollPane>,
  ) {
    if (e.id == MouseEvent.MOUSE_DRAGGED) {
      isDragging = true
    }
  }

  override fun processMouseWheelEvent(
    e: MouseWheelEvent,
    l: JLayer<out JScrollPane>,
  ) {
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
