package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val icons = listOf(
    "wi0009-16.png",
    "wi0054-16.png",
    "wi0062-16.png",
    "wi0063-16.png",
    "wi0124-16.png",
    "wi0126-16.png"
  )
  val tabbedPane = JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
  val cl = Thread.currentThread().contextClassLoader
  icons.forEach {
    tabbedPane.addTab(it, ImageIcon(cl.getResource("example/$it")), JLabel(it), it)
  }
  tabbedPane.componentPopupMenu = PinTabPopupMenu()
  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private class PinTabPopupMenu : JPopupMenu() {
  private val pinTabMenuItem = JCheckBoxMenuItem("pin tab").also {
    it.addActionListener { e ->
      val t = invoker
      val check = e.source
      if (t is JTabbedPane && check is JCheckBoxMenuItem) {
        val idx = t.selectedIndex
        val cmp = t.getComponentAt(idx)
        val tab = t.getTabComponentAt(idx)
        val icon = t.getIconAt(idx)
        val tip = t.getToolTipTextAt(idx)
        val flg = t.isEnabledAt(idx)
        val i = searchNewSelectedIndex(t, idx, check.isSelected)
        t.remove(idx)
        t.insertTab(if (check.isSelected) "" else tip, icon, cmp, tip, i)
        t.setTabComponentAt(i, tab)
        t.setEnabledAt(i, flg)
        if (flg) {
          t.selectedIndex = i
        }
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTabbedPane) {
      val idx = c.indexAtLocation(x, y)
      pinTabMenuItem.isEnabled = idx >= 0
      pinTabMenuItem.isSelected = isSelectedPinTab(c, idx)
      super.show(c, x, y)
    }
  }

  private fun searchNewSelectedIndex(t: JTabbedPane, idx: Int, dir: Boolean): Int {
    var i: Int
    if (dir) {
      i = 0
      while (i < idx) {
        if (!isSelectedPinTab(t, i)) {
          break
        }
        i++
      }
    } else {
      i = t.tabCount - 1
      while (i > idx) {
        if (isSelectedPinTab(t, i)) {
          break
        }
        i--
      }
    }
    return i
  }

  private fun isSelectedPinTab(t: JTabbedPane, idx: Int) = idx >= 0 &&
    idx == t.selectedIndex && isEmpty(t.getTitleAt(idx))

  private fun isEmpty(s: String?) = s == null || s.isEmpty()

  init {
    add(pinTabMenuItem)
    addSeparator()
    add("close all").addActionListener {
      (invoker as? JTabbedPane)?.also {
        for (i in it.tabCount - 1 downTo 0) {
          if (!isEmpty(it.getTitleAt(i))) {
            it.removeTabAt(i)
          }
        }
      }
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
