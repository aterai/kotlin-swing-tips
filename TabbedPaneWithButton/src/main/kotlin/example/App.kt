package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/page_new.gif")
  val icon = url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) }
    ?: UIManager.getIcon("html.missingImage")
  val button = object : JButton(icon) {
    private var handler: MouseListener? = null

    override fun updateUI() {
      removeMouseListener(handler)
      super.updateUI()
      isFocusable = false
      isContentAreaFilled = false
      isFocusPainted = false
      border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
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
  val tabs = object : ClippedTitleTabbedPane() {
    private val buttonPaddingTabAreaInsets: Insets
      get() {
        val ti = tabInsets
        val ai = tabAreaInsets
        val d = button.preferredSize
        val fm = getFontMetrics(font)
        val tih = d.height - fm.height - ti.top - ti.bottom - ai.bottom
        return Insets(ai.top.coerceAtLeast(tih), d.width + ai.left, ai.bottom, ai.right)
      }

    override fun updateUI() {
      val key = "TabbedPane.tabAreaInsets"
      UIManager.put(key, null)
      super.updateUI()
      UIManager.put(key, buttonPaddingTabAreaInsets)
      super.updateUI()
    }

    override fun getAlignmentX() = LEFT_ALIGNMENT

    override fun getAlignmentY() = TOP_ALIGNMENT
  }
  tabs.addTab("title1", JLabel("12345"))
  tabs.addTab("title2", JScrollPane(JTree()))
  tabs.addTab("title3", JLabel("67890"))
  button.addActionListener {
    tabs.addTab("title", JLabel("JLabel"))
  }

  val p = JPanel()
  p.layout = OverlayLayout(p)
  p.add(button)
  p.add(tabs)

  val menuBar = JMenuBar()
  val m1 = JMenu("Tab")
  m1.add("removeAll").addActionListener { tabs.removeAll() }
  menuBar.add(m1)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class ClippedTitleTabbedPane : JTabbedPane() {
  val tabInsets get() =
    UIManager.getInsets("TabbedPane.tabInsets") ?: getSynthInsets(Region.TABBED_PANE_TAB)
  val tabAreaInsets get() =
    UIManager.getInsets("TabbedPane.tabAreaInsets") ?: getSynthInsets(Region.TABBED_PANE_TAB_AREA)

  private fun getSynthInsets(region: Region): Insets {
    val style = SynthLookAndFeel.getStyle(this, region)
    val context = SynthContext(this, region, style, SynthConstants.ENABLED)
    return style.getInsets(context, null)
  }

  override fun doLayout() {
    val tabCount = tabCount
    if (tabCount == 0 || !isVisible) {
      super.doLayout()
      return
    }
    val tabInsets = tabInsets
    val tabAreaInsets = tabAreaInsets
    val ins = insets
    val tabPlacement = getTabPlacement()
    val areaWidth = width - tabAreaInsets.left - tabAreaInsets.right - ins.left - ins.right
    var tabWidth: Int
    var gap: Int
    if (tabPlacement == LEFT || tabPlacement == RIGHT) {
      tabWidth = areaWidth / 4
      gap = 0
    } else { // TOP || BOTTOM
      tabWidth = areaWidth / tabCount
      gap = areaWidth - tabWidth * tabCount
    }
    if (tabWidth > MAX_TAB_WIDTH) {
      tabWidth = MAX_TAB_WIDTH
      gap = 0
    }

    // "3" is magic number @see BasicTabbedPaneUI#calculateTabWidth
    tabWidth -= tabInsets.left + tabInsets.right + 3
    updateAllTabWidth(tabWidth, gap)
    super.doLayout()
  }

  override fun insertTab(title: String?, icon: Icon?, c: Component?, tip: String?, index: Int) {
    super.insertTab(title, icon, c, tip ?: title, index)
    setTabComponentAt(index, JLabel(title, icon, CENTER))
  }

  private fun updateAllTabWidth(tabWidth: Int, gap: Int) {
    val dim = Dimension()
    var rest = gap
    for (i in 0 until tabCount) {
      (getTabComponentAt(i) as? JComponent)?.also { tab ->
        val a = if (i == tabCount - 1) rest else 1
        val w = if (rest > 0) tabWidth + a else tabWidth
        dim.setSize(w, tab.preferredSize.height)
        tab.preferredSize = dim
        rest -= a
      }
    }
  }

  companion object {
    private const val MAX_TAB_WIDTH = 80
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
      minimumSize = Dimension(256, 200)
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
