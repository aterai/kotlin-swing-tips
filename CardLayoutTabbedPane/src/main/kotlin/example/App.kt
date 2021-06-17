package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicButtonUI
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.View

fun makeUI(): Component {
  val tab1 = JTabbedPane()
  tab1.addTab("JTree", JScrollPane(JTree()))
  tab1.addTab("JLabel", JLabel("JLabel 1"))
  tab1.addTab("JSplitPane", JSplitPane())
  tab1.addTab("JButton 1", JButton("JButton 1"))

  val tab2 = CardLayoutTabbedPane()
  tab2.addTab("JTabbedPane: default", tab1)
  tab2.addTab("JTree", JScrollPane(JTree()))
  tab2.addTab("JSplitPane", JSplitPane())
  tab2.addTab("JLabel", JLabel("JLabel 2"))
  tab2.addTab("JButton 2", JButton("JButton 2"))

  // UIManager.put("example.TabButton", "TabViewButtonUI")
  // UIManager.put("TabViewButtonUI", "example.OperaTabViewButtonUI")
  // val tab3 = CardLayoutTabbedPane()
  // tab3.addTab("9999", JScrollPane(JTree()))
  // tab3.addTab("000000000", JLabel("JLabel 5"))
  // tab3.addTab("1111", JLabel("JLabel 6"))
  // tab3.addTab("222", JButton("JButton 3"))

  return JPanel(BorderLayout()).also {
    it.add(tab2)
    // it.add(tab3)
    it.preferredSize = Dimension(320, 240)
  }
}

private class CardLayoutTabbedPane : JPanel(BorderLayout()) {
  private val cardLayout = CardLayout()
  private val tabPanel = JPanel(GridLayout(1, 0, 0, 0))
  private val wrapPanel = JPanel(BorderLayout())
  private val contentsPanel = JPanel(cardLayout)
  private val bg = ButtonGroup()

  init {
    val left = 1
    val right = 3
    tabPanel.border = BorderFactory.createEmptyBorder(1, left, 0, right)
    contentsPanel.border = BorderFactory.createEmptyBorder(4, left, 2, right)
    wrapPanel.add(tabPanel)
    wrapPanel.add(JLabel("test:"), BorderLayout.WEST)
    add(wrapPanel, BorderLayout.NORTH)
    add(contentsPanel)
  }

  private fun createTabComponent(title: String): Component {
    val tab = TabButton(title)
    val ml = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        (e.component as? AbstractButton)?.isSelected = true
        cardLayout.show(contentsPanel, title)
      }
    }
    tab.addMouseListener(ml)
    tab.layout = BorderLayout()
    val close = object : JButton(CloseTabIcon(Color.GRAY)) {
      override fun getPreferredSize() = Dimension(12, 12)
    }
    close.addActionListener { println("dummy action: close button") }
    close.border = BorderFactory.createEmptyBorder()
    close.isFocusPainted = false
    close.isContentAreaFilled = false
    close.pressedIcon = CloseTabIcon(Color(0xFE_FE_FE))
    close.rolloverIcon = CloseTabIcon(Color(0xA0_A0_A0))

    val p = JPanel(BorderLayout())
    p.isOpaque = false
    p.add(close, BorderLayout.NORTH)

    tab.add(p, BorderLayout.EAST)
    bg.add(tab)
    tab.isSelected = true
    return tab
  }

  fun addTab(title: String, comp: Component) {
    tabPanel.add(createTabComponent(title))
    contentsPanel.add(comp, title)
    cardLayout.show(contentsPanel, title)
  }
}

private class TabButton(text: String?) : JRadioButton(text, null) {
  var textColor: Color? = null
  var pressedTc: Color? = null
  var rolloverTc: Color? = null
  var rolloverSelTc: Color? = null
  var selectedTc: Color? = null

  override fun updateUI() {
    // val tmp = if (UIManager.get(uiClassID) != null) {
    //   UIManager.getUI(this) as? TabViewButtonUI
    // } else {
    //   BasicTabViewButtonUI()
    // }
    setUI(OperaTabViewButtonUI())
  }

  override fun getUIClassID() = UI_CLASS_ID

  override fun getUI() = ui as? TabViewButtonUI

  override fun fireStateChanged() {
    val m = getModel()
    foreground = if (m.isEnabled) {
      if (m.isPressed && m.isArmed) {
        pressedTc
      } else if (m.isSelected) {
        selectedTc
      } else if (isRolloverEnabled && m.isRollover) {
        rolloverTc
      } else {
        textColor
      }
    } else {
      Color.GRAY
    }
    super.fireStateChanged()
  }

  companion object {
    private const val UI_CLASS_ID = "TabViewButtonUI"
  }
}

private class CloseTabIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.drawLine(2, 2, 9, 9)
    g2.drawLine(2, 3, 8, 9)
    g2.drawLine(3, 2, 9, 8)
    g2.drawLine(9, 2, 2, 9)
    g2.drawLine(9, 3, 3, 9)
    g2.drawLine(8, 2, 2, 8)
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
}

private open class TabViewButtonUI : BasicButtonUI() { /* ButtonUI */ }

private open class BasicTabViewButtonUI : TabViewButtonUI() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun installDefaults(b: AbstractButton) {
    super.installDefaults(b)
    b.preferredSize = Dimension(0, 24)
    b.isRolloverEnabled = true
    b.isOpaque = true
    val out = BorderFactory.createMatteBorder(2, 0, 0, 0, b.background)
    val inb = BorderFactory.createMatteBorder(1, 1, 0, 1, Color.RED)
    b.border = BorderFactory.createCompoundBorder(out, inb)
    if (b is TabButton) {
      b.textColor = Color(0x64_64_64)
      b.pressedTc = Color.GRAY
      b.rolloverTc = Color.BLACK
      b.rolloverSelTc = Color.GRAY
      b.selectedTc = Color.BLACK
    }
  }

  override fun paint(g: Graphics, c: JComponent) {
    if (c !is AbstractButton) {
      return
    }
    g.font = c.font
    SwingUtilities.calculateInnerArea(c, viewRect)
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)
    val text = SwingUtilities.layoutCompoundLabel(
      c,
      c.getFontMetrics(c.font),
      c.text,
      null,
      c.verticalAlignment,
      c.horizontalAlignment,
      c.verticalTextPosition,
      c.horizontalTextPosition,
      viewRect,
      iconRect,
      textRect,
      0
    )
    g.color = c.background
    g.fillRect(0, 0, c.width, c.height)
    val model = c.model
    g.color = if (model.isSelected || model.isArmed) Color.WHITE else Color(0xDC_DC_DC)
    g.fillRect(viewRect.x, viewRect.y, viewRect.x + viewRect.width, viewRect.y + viewRect.height)
    val color = Color(0xFF_78_28)
    if (model.isSelected) {
      g.color = color
      g.drawLine(viewRect.x + 1, viewRect.y - 2, viewRect.x + viewRect.width - 1, viewRect.y - 2)
      g.color = color.brighter()
      g.drawLine(viewRect.x, viewRect.y - 1, viewRect.x + viewRect.width, viewRect.y - 1)
      g.color = color
      g.drawLine(viewRect.x, viewRect.y, viewRect.x + viewRect.width, viewRect.y)
    } else if (model.isRollover) {
      g.color = color
      g.drawLine(viewRect.x + 1, viewRect.y, viewRect.x + viewRect.width - 1, viewRect.y)
      g.color = color.brighter()
      g.drawLine(viewRect.x, viewRect.y + 1, viewRect.x + viewRect.width, viewRect.y + 1)
      g.color = color
      g.drawLine(viewRect.x, viewRect.y + 2, viewRect.x + viewRect.width, viewRect.y + 2)
    }
    (c.getClientProperty(BasicHTML.propertyKey) as? View)?.paint(g, textRect) ?: also {
      if (model.isSelected) {
        textRect.y -= 2
        textRect.x -= 1
      }
      textRect.x += 4
      paintText(g, c, textRect, text)
    }
  }

  // companion object {
  //   fun createUI(c: JComponent): ComponentUI {
  //     return BasicTabViewButtonUI()
  //   }
  // }
}

private class OperaTabViewButtonUI : BasicTabViewButtonUI() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun installDefaults(b: AbstractButton) {
    super.installDefaults(b)
    b.border = BorderFactory.createEmptyBorder()
    b.foreground = Color.WHITE
    (b as? TabButton)?.also {
      it.textColor = Color(230, 245, 255)
      it.pressedTc = Color.WHITE.darker()
      it.rolloverTc = Color.WHITE
      it.rolloverSelTc = Color.WHITE
      it.selectedTc = Color.WHITE
    }
  }

  override fun paint(g: Graphics, c: JComponent) {
    val b = c as? AbstractButton ?: return
    val f = b.font
    g.font = f
    SwingUtilities.calculateInnerArea(b, viewRect)
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)

    val g2 = g.create() as? Graphics2D ?: return
    tabPainter(g2, viewRect)

    val icon = b.icon
    viewRect.width -= CLOSE_ICON_WIDTH
    val text = SwingUtilities.layoutCompoundLabel(
      c, c.getFontMetrics(f), b.text, icon,
      b.verticalAlignment, b.horizontalAlignment,
      b.verticalTextPosition, b.horizontalTextPosition,
      viewRect, iconRect, textRect,
      if (b.text != null) b.iconTextGap else 0
    )
    val v = c.getClientProperty(BasicHTML.propertyKey) as? View
    if (v != null) {
      v.paint(g, textRect)
    } else {
      textRect.x += 4
      paintText(g, b, textRect, text)
    }
    icon?.paintIcon(c, g, iconRect.x + 4, iconRect.y + 2)

    val model = b.model
    if (!model.isSelected && !model.isArmed && !model.isRollover) {
      g2.paint = Color(0x64_00_00_00, true)
      g2.fillRect(0, 0, c.width, c.height)
    }
    g2.dispose()
  }

  private fun tabPainter(g2: Graphics2D, r: Rectangle) {
    val r1 = Rectangle(r.x, r.y, r.width, r.height / 2)
    val r2 = Rectangle(r.x, r.y + r.height / 2, r.width, r.height / 2)
    val r3 = Rectangle(r.x, r.y + r.height / 2 - 2, r.width, r.height / 4)
    g2.paint = GradientPaint(
      0f,
      r1.y.toFloat(),
      Color(0x84_A2_B4),
      0f,
      (r1.y + r1.height).toFloat(),
      Color(0x67_85_98),
      true
    )
    g2.fill(r1)
    g2.paint = GradientPaint(
      0f,
      r2.y.toFloat(),
      Color(0x32_49_54),
      0f,
      (r2.y + r2.height).toFloat(),
      Color(0x3C_56_65),
      true
    )
    g2.fill(r2)
    g2.paint = GradientPaint(
      0f,
      r3.y.toFloat(),
      Color(0, 0, 0, 30),
      0f,
      (r3.y + r3.height).toFloat(),
      Color(0, 0, 0, 5),
      true
    )
    g2.fill(r3)
    g2.paint = Color(39, 56, 67) // g2.setPaint(Color.GREEN);
    g2.drawLine(r.x, r.y, r.x + r.width, r.y)
    g2.paint = Color(255, 255, 255, 30) // g2.setPaint(Color.RED);
    g2.drawLine(r.x + 1, r.y + 1, r.x + r.width, r.y + 1)
    g2.paint = Color(255, 255, 255, 60) // g2.setPaint(Color.BLUE);
    g2.drawLine(r.x, r.y, r.x, r.y + r.height)
    g2.paint = Color(39, 56, 67, 250) // g2.setPaint(Color.YELLOW);
    g2.drawLine(r.x + r.width - 1, r.y, r.x + r.width - 1, r.y + r.height)
    g2.drawLine(r.x, r.y + r.height - 1, r.x + r.width - 1, r.y + r.height - 1)
  }

  companion object {
    private const val CLOSE_ICON_WIDTH = 12
    // fun createUI(c: JComponent): ComponentUI {
    //   return OperaTabViewButtonUI()
    // }
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
