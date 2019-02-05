package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Arrays
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel

class MainPanel : JPanel(BorderLayout()) {
  init {
    val list = Arrays.asList(makeTestTabbedPane(JTabbedPane()), makeTestTabbedPane(ClippedTitleTabbedPane()))

    val p = JPanel(GridLayout(list.size, 1))
    list.forEach { p.add(it) }

    val check = JCheckBox("LEFT")
    check.addActionListener { e ->
      val tabPlacement = if ((e.getSource() as JCheckBox).isSelected()) JTabbedPane.LEFT else JTabbedPane.TOP
      list.forEach { it.setTabPlacement(tabPlacement) }
    }

    add(check, BorderLayout.NORTH)
    add(p)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTestTabbedPane(jtp: JTabbedPane) = jtp.apply {
    setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT)
    addTab("1111111111111111111", ColorIcon(Color.RED), JScrollPane(JTree()))
    addTab("2", ColorIcon(Color.GREEN), JLabel("bbbbbbbbb"))
    addTab("33333333333333", ColorIcon(Color.BLUE), JScrollPane(JTree()))
    addTab("444444444444444", ColorIcon(Color.ORANGE), JLabel("dddddddddd"))
    addTab("55555555555555555555555555555555", ColorIcon(Color.CYAN), JLabel("e"))
  }
}

internal class ClippedTitleTabbedPane : JTabbedPane {
  private val tabInsets = UIManager.getInsets("TabbedPane.tabInsets") ?: getSynthTabInsets()
  private val tabAreaInsets = UIManager.getInsets("TabbedPane.tabAreaInsets") ?: getSynthTabAreaInsets()

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

  constructor() : super() {}

  constructor(tabPlacement: Int) : super(tabPlacement) {}

  override fun doLayout() {
    val tabCount = getTabCount()
    if (tabCount == 0 || !isVisible()) {
      super.doLayout()
      return
    }
    val tabInsets = tabInsets
    val tabAreaInsets = tabAreaInsets
    val insets = getInsets()
    val tabPlacement = getTabPlacement()
    val areaWidth = getWidth() - tabAreaInsets.left - tabAreaInsets.right - insets.left - insets.right
    val isSide = tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT
    var tabWidth = if (isSide) areaWidth / 4 else areaWidth / tabCount
    val gap = if (isSide) 0 else areaWidth - tabWidth * tabCount

    // "3" is magic number @see BasicTabbedPaneUI#calculateTabWidth
    tabWidth -= tabInsets.left + tabInsets.right + 3
    updateAllTabWidth(tabWidth, gap)

    super.doLayout()
  }

  override fun insertTab(title: String, icon: Icon, component: Component, tip: String?, index: Int) {
    super.insertTab(title, icon, component, tip ?: title, index)
    setTabComponentAt(index, JLabel(title, icon, SwingConstants.CENTER))
  }

  protected fun updateAllTabWidth(tabWidth: Int, gap: Int) {
    val dim = Dimension()
    var rest = gap
    for (i in 0 until getTabCount()) {
      val tab: Component? = getTabComponentAt(i)
      if (tab is JComponent) {
        val a = if (i == getTabCount() - 1) rest else 1
        val w = if (rest > 0) tabWidth + a else tabWidth
        dim.setSize(w, tab.getPreferredSize().height)
        tab.setPreferredSize(dim)
        rest -= a
      }
    }
  }
}

internal class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.setPaint(color)
    g2.fillRect(1, 1, getIconWidth() - 2, getIconHeight() - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
