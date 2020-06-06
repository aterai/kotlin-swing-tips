package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicButtonUI
import javax.swing.plaf.metal.MetalTabbedPaneUI
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel

fun makeUI(): Component {
  val tabbedPane = JTabbedPane(SwingConstants.LEFT)
  if (tabbedPane.ui is WindowsTabbedPaneUI) {
    tabbedPane.ui = LeftAlignmentWindowsTabbedPaneUI()
  } else {
    tabbedPane.ui = LeftAlignmentTabbedPaneUI()
  }

  val list = listOf(
    makeTestTabbedPane(JTabbedPane(SwingConstants.LEFT)),
    makeTestTabbedPane(tabbedPane),
    makeTestTabbedPane(ClippedTitleTabbedPane(SwingConstants.LEFT))
  )

  val p = JPanel(GridLayout(list.size, 1))
  list.forEach { p.add(it) }

  val check = JCheckBox("TOP")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    list.forEach { it.tabPlacement = if (b) SwingConstants.LEFT else SwingConstants.TOP }
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTestTabbedPane(tabbedPane: JTabbedPane) = tabbedPane.also {
  it.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  it.addTab("1111111111111111111", ColorIcon(Color.RED), JScrollPane(JTree()))
  it.addTab("2", ColorIcon(Color.GREEN), JLabel("1111111"))
  it.addTab("33333333333333", ColorIcon(Color.BLUE), JScrollPane(JTree()))
  it.addTab("444444444444444", ColorIcon(Color.ORANGE), JLabel("11111111"))
  it.addTab("55555555555555555555555555555555", ColorIcon(Color.CYAN), JLabel("e"))
}

private class ClippedTitleTabbedPane(tabPlacement: Int) : JTabbedPane(tabPlacement) {
  private fun getSynthInsets(region: Region): Insets {
    val style = SynthLookAndFeel.getStyle(this, region)
    val context = SynthContext(this, region, style, SynthConstants.ENABLED)
    return style.getInsets(context, null)
  }

  private val tabInsets: Insets
    get() = UIManager.getInsets("TabbedPane.tabInsets") ?: getSynthInsets(Region.TABBED_PANE_TAB)

  private val tabAreaInsets: Insets
    get() = UIManager.getInsets("TabbedPane.tabAreaInsets") ?: getSynthInsets(Region.TABBED_PANE_TAB_AREA)

  override fun doLayout() {
    val tabCount = tabCount
    if (tabCount == 0 || !isVisible) {
      super.doLayout()
      return
    }
    val tabInsets: Insets = tabInsets
    val tabAreaInsets: Insets = tabAreaInsets
    val insets: Insets = insets
    val tabPlacement = getTabPlacement()
    val areaWidth = width - tabAreaInsets.left - tabAreaInsets.right - insets.left - insets.right
    var tabWidth: Int // = tabInsets.left + tabInsets.right + 3;
    val gap: Int
    if (tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT) {
      tabWidth = areaWidth / 2
      gap = 0
    } else { // TOP || BOTTOM
      tabWidth = areaWidth / tabCount
      gap = areaWidth - tabWidth * tabCount
    }

    // "3" is magic number @see BasicTabbedPaneUI#calculateTabWidth
    tabWidth -= tabInsets.left + tabInsets.right + 3
    updateAllTabWidth(tabWidth, gap)
    super.doLayout()
  }

  override fun insertTab(
    title: String,
    icon: Icon,
    component: Component,
    tip: String?,
    index: Int
  ) {
    super.insertTab(title, icon, component, tip ?: title, index)
    setTabComponentAt(index, ButtonTabComponent(this))
  }

  private fun updateAllTabWidth(tabWidth: Int, gap: Int) {
    val dim = Dimension()
    var rest = gap
    for (i in 0 until tabCount) {
      (getTabComponentAt(i) as? JComponent)?.also {
        val a = if ((i == tabCount - 1)) rest else 1
        val w = if (rest > 0) tabWidth + a else tabWidth
        dim.setSize(w, it.preferredSize.height)
        it.preferredSize = dim
        rest -= a
      }
    }
  }
}

private class LeftAlignmentWindowsTabbedPaneUI : WindowsTabbedPaneUI() {
  override fun layoutLabel(
    tabPlacement: Int,
    metrics: FontMetrics,
    tabIndex: Int,
    title: String,
    icon: Icon,
    tabRect: Rectangle,
    iconRect: Rectangle,
    textRect: Rectangle,
    isSelected: Boolean
  ) {
    textRect.setLocation(0, 0)
    iconRect.setLocation(0, 0)
    val html = "html"
    getTextViewForTab(tabIndex)?.also {
      tabPane.putClientProperty(html, it)
    }
    SwingUtilities.layoutCompoundLabel(
      tabPane,
      metrics, title, icon,
      SwingConstants.CENTER,
      SwingConstants.LEFT, // CENTER, <----
      SwingConstants.CENTER,
      SwingConstants.TRAILING,
      tabRect,
      iconRect,
      textRect,
      textIconGap
    )
    tabPane.putClientProperty(html, null)
    textRect.translate(tabInsets.left + 2, 0)
    iconRect.translate(tabInsets.left + 2, 0)
    val xnu = getTabLabelShiftX(tabPlacement, tabIndex, isSelected)
    val ynu = getTabLabelShiftY(tabPlacement, tabIndex, isSelected)
    iconRect.x += xnu
    iconRect.y += ynu
    textRect.x += xnu
    textRect.y += ynu
  }
}

private class LeftAlignmentTabbedPaneUI : MetalTabbedPaneUI() {
  override fun layoutLabel(
    tabPlacement: Int,
    metrics: FontMetrics,
    tabIndex: Int,
    title: String,
    icon: Icon,
    tabRect: Rectangle,
    iconRect: Rectangle,
    textRect: Rectangle,
    isSelected: Boolean
  ) {
    textRect.setLocation(0, 0)
    iconRect.setLocation(0, 0)
    val html = "html"
    getTextViewForTab(tabIndex)?.also {
      tabPane.putClientProperty(html, it)
    }
    SwingUtilities.layoutCompoundLabel(
      tabPane,
      metrics, title, icon,
      SwingConstants.CENTER,
      SwingConstants.LEFT, // CENTER,
      SwingConstants.CENTER,
      SwingConstants.TRAILING,
      tabRect,
      iconRect,
      textRect,
      textIconGap
    )
    tabPane.putClientProperty(html, null)
    textRect.translate(tabInsets.left + 2, 0)
    iconRect.translate(tabInsets.left + 2, 0)
    val xnu = getTabLabelShiftX(tabPlacement, tabIndex, isSelected)
    val ynu = getTabLabelShiftY(tabPlacement, tabIndex, isSelected)
    iconRect.x += xnu
    iconRect.y += ynu
    textRect.x += xnu
    textRect.y += ynu
  }
}

// How to Use Tabbed Panes (The Java? Tutorials > Creating a GUI With JFC/Swing > Using Swing Components)
// https://docs.oracle.com/javase/tutorial/uiswing/components/tabbedpane.html
private class ButtonTabComponent(pane: JTabbedPane) : JPanel(BorderLayout()) {
  private val tabs = pane

  private inner class TabButtonHandler : MouseAdapter(), ActionListener {
    override fun actionPerformed(e: ActionEvent) {
      val i = tabs.indexOfTabComponent(this@ButtonTabComponent)
      if (i != -1) {
        tabs.remove(i)
      }
    }

    override fun mouseEntered(e: MouseEvent) {
      (e.component as? AbstractButton)?.isBorderPainted = true
    }

    override fun mouseExited(e: MouseEvent) {
      (e.component as? AbstractButton)?.isBorderPainted = false
    }
  }

  init {
    isOpaque = false
    val label = object : JLabel() {
      override fun getText(): String? {
        val i = tabs.indexOfTabComponent(this@ButtonTabComponent)
        if (i != -1) {
          return tabs.getTitleAt(i)
        }
        return null
      }

      override fun getIcon(): Icon? {
        val i = tabs.indexOfTabComponent(this@ButtonTabComponent)
        if (i != -1) {
          return tabs.getIconAt(i)
        }
        return null
      }
    }
    add(label)
    label.border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
    val button: JButton = TabButton()
    val handler = TabButtonHandler()
    button.addActionListener(handler)
    button.addMouseListener(handler)
    add(button, BorderLayout.EAST)
    border = BorderFactory.createEmptyBorder(2, 0, 0, 0)
  }
}

private class TabButton : JButton() {
  override fun getPreferredSize() = Dimension(SIZE, SIZE)

  override fun updateUI() {
    // we don't want to update UI for this button
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2: Graphics2D = g.create() as Graphics2D
    g2.stroke = BasicStroke(2f)
    g2.paint = Color.BLACK
    if (getModel().isRollover) {
      g2.paint = Color.ORANGE
    }
    if (getModel().isPressed) {
      g2.paint = Color.BLUE
    }
    g2.drawLine(DELTA, DELTA, width - DELTA - 1, height - DELTA - 1)
    g2.drawLine(width - DELTA - 1, DELTA, DELTA, height - DELTA - 1)
    g2.dispose()
  }

  companion object {
    private const val SIZE: Int = 17
    private const val DELTA: Int = 6
  }

  init {
    setUI(BasicButtonUI())
    toolTipText = "close this tab"
    isContentAreaFilled = false
    isFocusable = false
    border = BorderFactory.createEtchedBorder()
    isBorderPainted = false
    isRolloverEnabled = true
  }
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2: Graphics2D = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
