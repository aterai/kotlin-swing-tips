package example

import java.awt.*
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.InsetsUIResource

fun makeUI(): Component {
  val tabs = LineFocusTabbedPane()
  tabs.addTab("JTree", ColorIcon(Color.RED), JScrollPane(JTree()))
  tabs.addTab("JTextArea", ColorIcon(Color.GREEN), JScrollPane(JTextArea()))
  tabs.addTab("JTable", ColorIcon(Color.BLUE), JScrollPane(JTable(8, 3)))
  tabs.addTab("JSplitPane", ColorIcon(Color.ORANGE), JScrollPane(JSplitPane()))
  tabs.selectedIndex = -1
  EventQueue.invokeLater { tabs.selectedIndex = 0 }
  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private class LineFocusTabbedPane : JTabbedPane() {
  private var listener: ChangeListener? = null

  override fun updateUI() {
    removeChangeListener(listener)
    UIManager.put("TabbedPane.tabInsets", InsetsUIResource(1, 4, 0, 4))
    UIManager.put("TabbedPane.selectedTabPadInsets", InsetsUIResource(1, 1, 1, 1))
    UIManager.put("TabbedPane.tabAreaInsets", InsetsUIResource(3, 2, 0, 2))
    UIManager.put("TabbedPane.selectedLabelShift", 0)
    UIManager.put("TabbedPane.labelShift", 0)
    UIManager.put("TabbedPane.focus", ColorUIResource(Color(0x0, true)))
    super.updateUI()
    listener = TabSelectionListener()
    addChangeListener(listener)
    tabLayoutPolicy = SCROLL_TAB_LAYOUT
  }

  override fun insertTab(
    title: String?,
    icon: Icon?,
    c: Component?,
    tip: String?,
    index: Int,
  ) {
    super.insertTab(title, icon, c, tip, index)
    val label = JLabel(title, icon, CENTER)
    setTabComponentAt(index, label)
  }
}

private class TabSelectionListener : ChangeListener {
  private var prev = -1

  override fun stateChanged(e: ChangeEvent) {
    val tabbedPane = e.source as? JTabbedPane
    if (tabbedPane == null || tabbedPane.tabCount <= 0) {
      return
    }
    val idx = tabbedPane.selectedIndex
    for (i in 0 until tabbedPane.tabCount) {
      val tab = tabbedPane.getTabComponentAt(i)
      if (tab is JComponent) {
        val color = when (i) {
          idx -> SELECTION_COLOR
          prev -> PREV_COLOR
          else -> ALPHA_ZERO
        }
        tab.border = BorderFactory.createMatteBorder(3, 0, 0, 0, color)
      }
    }
    prev = idx
  }

  companion object {
    private val ALPHA_ZERO = Color(0x0, true)
    private val SELECTION_COLOR = Color(0x00_AA_FF)
    private val PREV_COLOR = Color(0x48_00_AA_FF, true)
  }
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 3, iconHeight - 3)
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
