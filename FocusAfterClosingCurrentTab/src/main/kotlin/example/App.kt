package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.LocalTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel

private const val CLOSE_CURRENT_TAB = "close_current_tab"

fun makeUI(): Component {
  val tabbedPane = object : ClippedTitleTabbedPane() {
    private val history = ArrayList<Component>(5)

    override fun setSelectedIndex(index: Int) {
      super.setSelectedIndex(index)
      val component = getComponentAt(index)
      history.remove(component)
      history.add(0, component)
    }

    override fun removeTabAt(index: Int) {
      val component = getComponentAt(index)
      super.removeTabAt(index)
      history.remove(component)
      if (history.isNotEmpty()) {
        selectedComponent = history[0]
      }
    }
  }
  tabbedPane.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  tabbedPane.addTab("aa aa", JLabel("aaa"))
  tabbedPane.addTab("bbb bbb", JLabel("bbb"))
  tabbedPane.addTab("ccc", JLabel("ccc"))
  tabbedPane.addTab("d", JLabel("ddd"))
  tabbedPane.addTab("ee", JLabel("eee"))

  val modifiers = Toolkit.getDefaultToolkit().menuShortcutKeyMask
  val im = tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, modifiers), CLOSE_CURRENT_TAB)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK), CLOSE_CURRENT_TAB)
  val a = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val t = e.source as? JTabbedPane ?: return
      val idx = t.selectedIndex
      if (idx >= 0) {
        t.removeTabAt(idx)
      }
    }
  }
  tabbedPane.actionMap.put(CLOSE_CURRENT_TAB, a)

  val button1 = JButton("add tab")
  button1.addActionListener {
    val title = LocalTime.now(ZoneId.systemDefault()).toString()
    tabbedPane.addTab(title, JLabel(title))
  }

  val button2 = JButton("add tab with focus")
  button2.addActionListener {
    val title = LocalTime.now(ZoneId.systemDefault()).toString()
    tabbedPane.addTab(title, JLabel(title))
    tabbedPane.selectedIndex = tabbedPane.tabCount - 1
  }

  val p = JPanel(GridLayout(1, 2, 2, 2))
  p.add(button1)
  p.add(button2)

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class ClippedTitleTabbedPane : JTabbedPane() {
  private val tabInsets = UIManager.getInsets("TabbedPane.tabInsets")
    ?: getSynthTabInsets()
  private val tabAreaInsets = UIManager.getInsets("TabbedPane.tabAreaInsets")
    ?: getSynthTabAreaInsets()

  private fun getSynthTabInsets(): Insets {
    val region = Region.TABBED_PANE_TAB
    val style = SynthLookAndFeel.getStyle(this, region)
    val ctx = SynthContext(this, region, style, SynthConstants.ENABLED)
    return style.getInsets(ctx, null)
  }

  private fun getSynthTabAreaInsets(): Insets {
    val region = Region.TABBED_PANE_TAB_AREA
    val style = SynthLookAndFeel.getStyle(this, region)
    val ctx = SynthContext(this, region, style, SynthConstants.ENABLED)
    return style.getInsets(ctx, null)
  }

  override fun doLayout() {
    val tabCount = tabCount
    if (tabCount == 0 || !isVisible) {
      super.doLayout()
      return
    }
    val tabIns = tabInsets
    val tabAreaIns = tabAreaInsets
    val ins = insets
    val tabPlacement = getTabPlacement()
    val areaWidth = width - tabAreaIns.left - tabAreaIns.right - ins.left - ins.right
    val isSide = tabPlacement == LEFT || tabPlacement == RIGHT
    val tw = if (isSide) areaWidth / 3 else areaWidth / tabCount
    var tabWidth = tw.coerceIn(MIN_TAB_WIDTH, MAX_TAB_WIDTH)
    val gap = if (isSide) 0 else areaWidth - tabWidth * tabCount

    // "3" is magic number @see BasicTabbedPaneUI#calculateTabWidth
    tabWidth -= tabIns.left + tabIns.right + 3
    updateAllTabWidth(tabWidth, gap)

    super.doLayout()
  }

  override fun insertTab(
    title: String?,
    icon: Icon?,
    c: Component?,
    tip: String?,
    index: Int,
  ) {
    super.insertTab(title, icon, c, tip ?: title, index)
    setTabComponentAt(index, ButtonTabComponent(this))
  }

  private fun updateAllTabWidth(
    tabWidth: Int,
    gap: Int,
  ) {
    val dim = Dimension()
    var rest = gap
    for (i in 0..<tabCount) {
      val tab = getTabComponentAt(i) as? JComponent ?: continue
      val a = if (i == tabCount - 1) rest else 1
      val w = if (rest > 0) tabWidth + a else tabWidth
      dim.setSize(w, tab.preferredSize.height)
      tab.preferredSize = dim
      rest -= a
    }
  }

  companion object {
    private const val MAX_TAB_WIDTH = 200
    private const val MIN_TAB_WIDTH = 50
  }
}

private class ButtonTabComponent(
  val tabbedPane: JTabbedPane,
) : JPanel(BorderLayout()) {
  private inner class TabButtonHandler :
    MouseAdapter(),
    ActionListener {
    override fun actionPerformed(e: ActionEvent) {
      val i = tabbedPane.indexOfTabComponent(this@ButtonTabComponent)
      if (i != -1) {
        tabbedPane.remove(i)
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
        val i = tabbedPane.indexOfTabComponent(this@ButtonTabComponent)
        if (i != -1) {
          return tabbedPane.getTitleAt(i)
        }
        return null
      }

      override fun getIcon(): Icon? {
        val i = tabbedPane.indexOfTabComponent(this@ButtonTabComponent)
        if (i != -1) {
          return tabbedPane.getIconAt(i)
        }
        return null
      }
    }
    add(label)
    label.border = BorderFactory.createEmptyBorder(0, 0, 0, 2)

    val button = JButton(CloseTabIcon(Color.BLACK)).also {
      it.rolloverIcon = CloseTabIcon(Color.ORANGE)
      it.border = BorderFactory.createEmptyBorder()
      it.isFocusPainted = false
      it.isBorderPainted = false
      it.isContentAreaFilled = false
      val handler = TabButtonHandler()
      it.addActionListener(handler)
      it.addMouseListener(handler)
    }

    add(button, BorderLayout.EAST)
    border = BorderFactory.createEmptyBorder(1, 0, 0, 0)
  }
}

private class CloseTabIcon(
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
    g2.drawLine(4, 4, 11, 11)
    g2.drawLine(4, 5, 10, 11)
    g2.drawLine(5, 4, 11, 10)
    g2.drawLine(11, 4, 4, 11)
    g2.drawLine(11, 5, 5, 11)
    g2.drawLine(10, 4, 4, 10)
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
