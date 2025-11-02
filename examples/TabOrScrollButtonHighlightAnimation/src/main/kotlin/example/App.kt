package example

import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
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
  private val closePage: JMenuItem
  private val closeAll: JMenuItem
  private val closeAllButActive: JMenuItem

  init {
    val counter = AtomicInteger()
    add("New tab").addActionListener {
      (getInvoker() as? JTabbedPane)?.also {
        addTab(counter, it)
        it.setSelectedIndex(it.tabCount - 1)
      }
    }
    add("New tab Opens in Background").addActionListener {
      (getInvoker() as? JTabbedPane)?.also {
        addTab(counter, it)
      }
    }
    addSeparator()
    closePage = add("Close")
    closePage.addActionListener {
      (getInvoker() as? JTabbedPane)?.also {
        it.remove(it.selectedIndex)
      }
    }
    addSeparator()
    closeAll = add("Close all")
    closeAll.addActionListener {
      (getInvoker() as? JTabbedPane)?.removeAll()
    }
    closeAllButActive = add("Close all bat active")
    closeAllButActive.addActionListener {
      (getInvoker() as? JTabbedPane)?.also {
        val idx = it.selectedIndex
        val title = it.getTitleAt(idx)
        val cmp = it.getComponentAt(idx)
        it.removeAll()
        it.addTab(title, cmp)
      }
    }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTabbedPane) {
      closePage.setEnabled(c.indexAtLocation(x, y) >= 0)
      closeAll.setEnabled(c.tabCount > 0)
      closeAllButActive.setEnabled(c.tabCount > 0)
      super.show(c, x, y)
    }
  }

  private fun addTab(counter: AtomicInteger, tabs: JTabbedPane) {
    val iv = counter.getAndIncrement()
    tabs.addTab("Title: $iv", JLabel("Tab: $iv"))
  }
}

private class TabHighlightLayerUI : LayerUI<JTabbedPane>() {
  private val rect = Rectangle()
  private val animator = Timer(10, null)
  private var listener: ActionListener? = null
  private var alpha = 0

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.setLayerEventMask(AWTEvent.HIERARCHY_EVENT_MASK)
    }
  }

  override fun uninstallUI(c: JComponent?) {
    if (c is JLayer<*>) {
      c.setLayerEventMask(0)
    }
    super.uninstallUI(c)
  }

  override fun paint(g: Graphics, c: JComponent?) {
    super.paint(g, c)
    if (c is JLayer<*>) {
      val g2 = g.create() as Graphics2D
      val a = alpha / 100f
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a)
      g2.paint = Color.RED
      g2.fill(rect)
      g2.dispose()
    }
  }

  override fun processHierarchyEvent(e: HierarchyEvent, l: JLayer<out JTabbedPane>) {
    super.processHierarchyEvent(e, l)
    val parent = e.getChangedParent()
    val tabs = l.getView()
    val flags = e.getChangeFlags()
    if (parent == tabs && flags == HierarchyEvent.PARENT_CHANGED.toLong()) {
      EventQueue.invokeLater { startAnime(l, e.component) }
    }
  }

  private fun startAnime(l: JLayer<out JTabbedPane>, c: Component?) {
    val tabs: JTabbedPane = l.getView()
    val idx = tabs.indexOfComponent(c)
    val tabRect = tabs.getBoundsAt(idx)
    if (tabs.bounds.contains(tabRect)) {
      rect.bounds = tabRect
    } else {
      val b = getScrollForwardButton(tabs)
      if (b != null) {
        rect.bounds = b.bounds
      }
    }
    animator.start()
    animator.removeActionListener(listener)
    listener = ActionListener {
      if (alpha < 32) {
        alpha += 1
      } else {
        alpha = 0
        animator.stop()
      }
      l.paintImmediately(rect)
    }
    animator.addActionListener(listener)
    animator.start()
  }

  private fun getScrollForwardButton(tabs: JTabbedPane): JButton? {
    var button1: JButton? = null
    var button2: JButton? = null
    for (c in tabs.components) {
      if (c is JButton) {
        if (button1 == null) {
          button1 = c
        } else if (button2 == null) {
          button2 = c
        }
      }
    }
    val x1 = button1?.getX() ?: -1
    val x2 = button2?.getX() ?: -1
    return if (x1 > x2) button1 else button2
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
