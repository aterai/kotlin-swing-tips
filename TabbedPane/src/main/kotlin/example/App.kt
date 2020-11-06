package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JTabbedPane().also {
  it.componentPopupMenu = TabbedPanePopupMenu()
  it.addTab("Title", JLabel("Tab"))
  it.preferredSize = Dimension(320, 240)
}

private class TabbedPanePopupMenu : JPopupMenu() {
  private val closePage: JMenuItem
  private val closeAll: JMenuItem
  private val closeAllButActive: JMenuItem

  init {
    val counter = AtomicInteger()
    add("New tab").addActionListener {
      (invoker as? JTabbedPane)?.also {
        val iv = counter.getAndIncrement()
        it.addTab("Title: $iv", JLabel("Tab: $iv"))
        it.selectedIndex = it.tabCount - 1
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
        val idx = it.selectedIndex
        val title = it.getTitleAt(idx)
        val cmp = it.getComponentAt(idx)
        it.removeAll()
        it.addTab(title, cmp)
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTabbedPane)?.also {
      closePage.isEnabled = it.indexAtLocation(x, y) >= 0
      closeAll.isEnabled = it.tabCount > 0
      closeAllButActive.isEnabled = it.tabCount > 0
      super.show(it, x, y)
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
