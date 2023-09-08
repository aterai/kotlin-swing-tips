package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import javax.swing.*

fun makeUI(): Component {
  val tabbedPane = HoverCloseButtonTabbedPane()
  tabbedPane.componentPopupMenu = TabbedPanePopupMenu()
  tabbedPane.addTab("JTree", JScrollPane(JTree()))
  tabbedPane.addTab("JLabel", JScrollPane(JLabel("JLabel")))
  tabbedPane.addTab("JSplitPane", JSplitPane())
  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private class HoverCloseButtonTabbedPane(
  tabPlacement: Int = TOP,
) : JTabbedPane(tabPlacement, SCROLL_TAB_LAYOUT) {
  private var hoverHandler: MouseMotionListener? = null

  override fun updateUI() {
    removeMouseMotionListener(hoverHandler)
    super.updateUI()
    hoverHandler = object : MouseAdapter() {
      private var prev = -1

      override fun mouseMoved(e: MouseEvent) {
        val source = e.component as? JTabbedPane
        val focused = source?.indexAtLocation(e.x, e.y)
        if (source == null || focused == null || focused == prev) {
          return
        }
        for (i in 0 until source.tabCount) {
          (source.getTabComponentAt(i) as? TabPanel)?.setButtonVisible(i == focused)
        }
        prev = focused
      }
    }
    addMouseMotionListener(hoverHandler)
  }

  override fun addTab(title: String, content: Component) {
    super.addTab(title, content)
    setTabComponentAt(tabCount - 1, TabPanel(this, title, content))
  }
}

private class TabPanel(
  pane: JTabbedPane,
  title: String?,
  content: Component?,
) : JPanel(BorderLayout()) {
  private val button = object : JButton(CloseTabIcon()) {
    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createEmptyBorder()
      isBorderPainted = false
      isFocusPainted = false
      isContentAreaFilled = false
      isFocusable = false
      isVisible = false
    }
  }

  init {
    isOpaque = false
    val label = object : JLabel() {
      override fun getPreferredSize(): Dimension {
        val dim = super.getPreferredSize()
        val bw = if (button.isVisible) button.preferredSize.width else 0
        return Dimension(TAB_WIDTH - bw, dim.height)
      }
    }
    label.text = title
    label.border = BorderFactory.createEmptyBorder(0, 0, 0, 1)
    button.addActionListener {
      val idx = pane.indexOfComponent(content)
      pane.removeTabAt(idx)
      if (pane.tabCount > idx) {
        (pane.getTabComponentAt(idx) as? TabPanel)?.setButtonVisible(true)
      }
    }
    add(label)
    add(button, BorderLayout.EAST)
  }

  fun setButtonVisible(flag: Boolean) {
    button.isVisible = flag
  }

  companion object {
    private const val TAB_WIDTH = 80
  }
}

private class CloseTabIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.ORANGE
    g2.drawLine(2, 3, 9, 10)
    g2.drawLine(2, 4, 8, 10)
    g2.drawLine(3, 3, 9, 9)
    g2.drawLine(9, 3, 2, 10)
    g2.drawLine(9, 4, 3, 10)
    g2.drawLine(8, 3, 2, 9)
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
}

private class TabbedPanePopupMenu : JPopupMenu() {
  private var count = 0
  private val closeAll: JMenuItem

  init {
    add("Add").addActionListener {
      (invoker as? JTabbedPane)?.also {
        it.addTab("Title$count", JLabel("Tab$count"))
        it.selectedIndex = it.tabCount - 1
        count++
      }
    }
    addSeparator()
    closeAll = add("Close All")
    closeAll.addActionListener { (invoker as? JTabbedPane)?.removeAll() }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTabbedPane) {
      closeAll.isEnabled = c.tabCount > 0
      super.show(c, x, y)
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
