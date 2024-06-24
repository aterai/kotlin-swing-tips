package example

import java.awt.*
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

fun makeUI() = JTabbedPane().also {
  it.componentPopupMenu = TabbedPanePopupMenu()
  it.addChangeListener(TabChangeListener())
  it.addTab("Title", JLabel("Tab"))
  it.preferredSize = Dimension(320, 240)
}

private class TabChangeListener : ChangeListener {
  override fun stateChanged(e: ChangeEvent) {
    val tabbedPane = e.source as? JTabbedPane
    if (tabbedPane == null || tabbedPane.tabCount <= 0) {
      return
    }
    val selectedIndex = tabbedPane.selectedIndex
    for (i in 0..<tabbedPane.tabCount) {
      if (i == selectedIndex && tabbedPane.getTitleAt(selectedIndex).endsWith("1")) {
        tabbedPane.setForegroundAt(i, Color.GREEN)
      } else if (i == selectedIndex) {
        val sc = if (selectedIndex % 2 == 0) Color.RED else Color.BLUE
        tabbedPane.setForegroundAt(i, sc)
      } else {
        tabbedPane.setForegroundAt(i, Color.BLACK)
      }
    }
  }
}

private class TabbedPanePopupMenu : JPopupMenu() {
  private var count = 0
  private val closePage: JMenuItem
  private val closeAll: JMenuItem
  private val closeAllButActive: JMenuItem

  init {
    add("New tab").addActionListener {
      (invoker as? JTabbedPane)?.also {
        it.addTab("Title: $count", JLabel("Tab: $count"))
        it.selectedIndex = it.tabCount - 1
        count++
      }
    }
    addSeparator()
    closePage = add("Close")
    closePage.addActionListener {
      (invoker as? JTabbedPane)?.also {
        it.remove(it.selectedIndex)
      }
    }
    addSeparator()
    closeAll = add("Close all")
    closeAll.addActionListener {
      (invoker as? JTabbedPane)?.removeAll()
    }
    closeAllButActive = add("Close all bat active")
    closeAllButActive.addActionListener {
      (invoker as? JTabbedPane)?.also {
        val tabIdx = it.selectedIndex
        val title = it.getTitleAt(tabIdx)
        val cmp = it.getComponentAt(tabIdx)
        it.removeAll()
        it.addTab(title, cmp)
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    (c as? JTabbedPane)?.also {
      closePage.isEnabled = it.indexAtLocation(x, y) >= 0
      closeAll.isEnabled = it.tabCount > 0
      closeAllButActive.isEnabled = it.tabCount > 0
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
