package example

import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.util.Arrays
import java.util.Objects
import javax.swing.*
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel
import javax.swing.plaf.synth.SynthStyle

class MainPanel : JPanel(BorderLayout()) {
  init {
    val list = Arrays.asList(
        makeTestTabbedPane(ClippedTitleTabbedPane()),
        makeTestTabbedPane(TextOverfloFadeTabbedPane()))

    val p = JPanel(GridLayout(2, 1))
    list.forEach({ t -> p.add(t) })

    val check = JCheckBox("LEFT")
    check.addActionListener({ e ->
      val tabPlacement = if ((e.getSource() as JCheckBox).isSelected()) JTabbedPane.LEFT else JTabbedPane.TOP
      list.forEach({ t -> t.setTabPlacement(tabPlacement) })
    })

    add(check, BorderLayout.NORTH)
    add(p)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTestTabbedPane(jtp: JTabbedPane): JTabbedPane {
    jtp.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT)
    jtp.addTab("1111111111111111111", ColorIcon(Color.RED), JScrollPane(JTree()))
    jtp.addTab("2", ColorIcon(Color.GREEN), JLabel("bbbbbbbbb"))
    jtp.addTab("33333333333333", ColorIcon(Color.BLUE), JScrollPane(JTree()))
    jtp.addTab("444444444444444", ColorIcon(Color.ORANGE), JLabel("dddddddddd"))
    jtp.addTab("55555555555555555555555555555555", ColorIcon(Color.CYAN), JLabel("e"))
    return jtp
  }
}

open class ClippedTitleTabbedPane : JTabbedPane {
  private val tabInsets: Insets
    get() {
      val insets = UIManager.getInsets("TabbedPane.tabInsets")
      if (Objects.nonNull(insets)) {
        return insets
      } else {
        val style = SynthLookAndFeel.getStyle(this, Region.TABBED_PANE_TAB)
        val context = SynthContext(this, Region.TABBED_PANE_TAB, style, SynthConstants.ENABLED)
        return style.getInsets(context, null)
      }
    }

  private val tabAreaInsets: Insets
    get() {
      val insets = UIManager.getInsets("TabbedPane.tabAreaInsets")
      if (Objects.nonNull(insets)) {
        return insets
      } else {
        val style = SynthLookAndFeel.getStyle(this, Region.TABBED_PANE_TAB_AREA)
        val context = SynthContext(this, Region.TABBED_PANE_TAB_AREA, style, SynthConstants.ENABLED)
        return style.getInsets(context, null)
      }
    }

  constructor() : super() {}

  protected constructor(tabPlacement: Int) : super(tabPlacement) {}

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
    var tabWidth : Int // = 0 // = tabInsets.left + tabInsets.right + 3;
    var gap : Int // = 0

    if (tabPlacement == LEFT || tabPlacement == RIGHT) {
      tabWidth = areaWidth / 4
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

  override fun insertTab(title: String, icon: Icon, component: Component, tip: String?, index: Int) {
    super.insertTab(title, icon, component, Objects.toString(tip, title), index)
    setTabComponentAt(index, JLabel(title))
  }

  protected fun updateAllTabWidth(tabWidth: Int, gap: Int) {
    val dim = Dimension()
    var rest = gap
    for (i in 0 until getTabCount()) {
      val tab = getTabComponentAt(i) as JComponent
      if (Objects.nonNull(tab)) {
        val a = if (i == getTabCount() - 1) rest else 1
        val w = if (rest > 0) tabWidth + a else tabWidth
        dim.setSize(w, tab.getPreferredSize().height)
        tab.setPreferredSize(dim)
        rest -= a
      }
    }
  }
}

internal class TextOverfloFadeTabbedPane : ClippedTitleTabbedPane {
  constructor() : super() {}

  protected constructor(tabPlacement: Int) : super(tabPlacement) {}

  override fun insertTab(title: String, icon: Icon, component: Component, tip: String?, index: Int) {
    super.insertTab(title, icon, component, Objects.toString(tip, title), index)
    val p = JPanel(BorderLayout(2, 0))
    p.setOpaque(false)
    p.add(JLabel(icon), BorderLayout.WEST)
    p.add(TextOverfloFadeLabel(title))
    setTabComponentAt(index, p)
  }
}

internal class TextOverfloFadeLabel(text: String) : JLabel(text) {
  override fun paintComponent(g: Graphics) {
    val i = getInsets()
    val w = getWidth() - i.left - i.right
    val h = getHeight() - i.top - i.bottom
    val rect = Rectangle(i.left, i.top, w - LENGTH, h)

    val g2 = g.create() as Graphics2D
    g2.setFont(g.getFont())
    g2.setPaint(getForeground())

    val frc = g2.getFontRenderContext()
    val tl = TextLayout(getText(), getFont(), frc)
    val baseline = getBaseline(w, h).toFloat()

    g2.setClip(rect)
    tl.draw(g2, getInsets().left.toFloat(), baseline)

    rect.width = 1
    var alpha = 1f
    for (x in w - LENGTH until w) {
      rect.x = x
      alpha = Math.max(0f, alpha - DIFF)
      g2.setComposite(AlphaComposite.SrcOver.derive(alpha))
      g2.setClip(rect)
      tl.draw(g2, getInsets().left.toFloat(), baseline)
    }
    g2.dispose()
  }

  companion object {
      private val LENGTH = 20
      private val DIFF = .05f
  }
}

internal class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.setPaint(color)
    g2.fillRect(1, 2, getIconWidth() - 2, getIconHeight() - 2)
    g2.dispose()
  }

  override fun getIconWidth() : Int {
    return 16
  }

  override fun getIconHeight() : Int {
    return 16
  }
}

fun main() {
  EventQueue.invokeLater(object : Runnable {
    override fun run() {
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
  })
}
