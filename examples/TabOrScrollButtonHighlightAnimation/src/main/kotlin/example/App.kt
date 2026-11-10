package example

import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import javax.swing.*
import javax.swing.plaf.LayerUI

fun createUI(): Component {
  val tabs = JTabbedPane()
  tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT)
  tabs.setComponentPopupMenu(TabbedPanePopupMenu())
  tabs.addTab("JTree", JScrollPane(JTree()))
  tabs.addTab("JLabel", JLabel("Test"))
  tabs.addTab("JTable", JScrollPane(JTable(10, 3)))
  tabs.addTab("JTextArea", JScrollPane(JTextArea()))
  tabs.addTab("JSplitPane", JSplitPane())
  return JPanel(BorderLayout()).also {
    it.add(JLayer(tabs, TabHighlightLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TabbedPanePopupMenu : JPopupMenu() {
  private val closeTab: JMenuItem
  private val closeAll: JMenuItem
  private val closeAllButActive: JMenuItem
  private var counter = 0

  init {
    add("New tab").addActionListener {
      (getInvoker() as? JTabbedPane)?.also {
        addTab(it)
        it.setSelectedIndex(it.tabCount - 1)
      }
    }
    add("New tab Opens in Background").addActionListener {
      (getInvoker() as? JTabbedPane)?.also {
        addTab(it)
      }
    }
    addSeparator()
    closeTab = add("Close")
    closeTab.addActionListener {
      (getInvoker() as? JTabbedPane)?.also {
        it.remove(it.selectedIndex)
      }
    }
    addSeparator()
    closeAll = add("Close all")
    closeAll.addActionListener {
      (getInvoker() as? JTabbedPane)?.removeAll()
    }
    closeAllButActive = add("Close all but active")
    closeAllButActive.addActionListener {
      (getInvoker() as? JTabbedPane)?.also {
        val selected = it.selectedIndex
        // Keep the active tab in place: re-adding it would start the highlight animation.
        for (i in it.tabCount - 1 downTo 0) {
          if (i != selected) {
            it.removeTabAt(i)
          }
        }
      }
    }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTabbedPane) {
      closeTab.setEnabled(c.indexAtLocation(x, y) >= 0)
      closeAll.setEnabled(c.tabCount > 0)
      closeAllButActive.setEnabled(c.tabCount > 0)
      super.show(c, x, y)
    }
  }

  private fun addTab(tabs: JTabbedPane) {
    tabs.addTab("Title: $counter", JLabel("Tab: $counter"))
    counter++
  }
}

private class TabHighlightLayerUI : LayerUI<JTabbedPane>() {
  private val highlightRect = Rectangle()
  private val animator = Timer(10, null)
  private var listener: ActionListener? = null
  private var alpha = 0

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.setLayerEventMask(AWTEvent.HIERARCHY_EVENT_MASK)
      val l = ActionListener { updateAlpha(c) }
      animator.addActionListener(l)
      listener = l
    }
  }

  override fun uninstallUI(c: JComponent?) {
    if (c is JLayer<*>) {
      c.setLayerEventMask(0)
      animator.stop()
      animator.removeActionListener(listener)
      alpha = 0
    }
    super.uninstallUI(c)
  }

  override fun paint(g: Graphics, c: JComponent?) {
    super.paint(g, c)
    if (alpha > 0) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 100f)
      g2.paint = Color.RED
      g2.fill(highlightRect)
      g2.dispose()
    }
  }

  override fun processHierarchyEvent(e: HierarchyEvent, l: JLayer<out JTabbedPane>) {
    super.processHierarchyEvent(e, l)
    // The same event is also dispatched to all descendants of the added component,
    // such as the JViewport of a JScrollPane, so ignore everything but the component itself.
    val c = e.component
    val isAdded = c == e.changed && e.changedParent == l.view
    if (isAdded && e.changeFlags == HierarchyEvent.PARENT_CHANGED.toLong()) {
      EventQueue.invokeLater { startAnimation(l, c) }
    }
  }

  private fun startAnimation(l: JLayer<out JTabbedPane>, c: Component?) {
    val tabs = l.view
    // The JTabbedPane also adds its own children, such as the scroll buttons,
    // and the tab may already have been removed.
    val idx = tabs.indexOfComponent(c)
    if (idx >= 0) {
      getHighlightBounds(tabs, idx)?.also {
        l.repaint(highlightRect) // Erase the highlight of the animation in progress
        highlightRect.bounds = it
        alpha = 0
        animator.restart()
      }
    }
  }

  private fun updateAlpha(l: JLayer<*>) {
    if (alpha < MAX_ALPHA) {
      alpha += 1
    } else {
      alpha = 0
      animator.stop()
    }
    l.paintImmediately(highlightRect)
  }

  private fun getHighlightBounds(tabs: JTabbedPane, idx: Int): Rectangle? {
    val tabRect = tabs.getBoundsAt(idx)
    // JTabbedPane#getBoundsAt(...) returns the bounds in the JTabbedPane coordinate system
    return if (Rectangle(tabs.size).contains(tabRect)) {
      tabRect
    } else {
      getScrollForwardButton(tabs)?.bounds
    }
  }

  // The scrollTabsForward button is placed to the right of the scrollTabsBackward button.
  private fun getScrollForwardButton(tabs: JTabbedPane) =
    tabs.components.filterIsInstance<JButton>().maxByOrNull { it.x }

  companion object {
    private const val MAX_ALPHA = 32
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
