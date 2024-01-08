package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicTabbedPaneUI
import javax.swing.plaf.metal.MetalTabbedPaneUI

fun makeUI(): Component {
  val tabbedPane = object : JTabbedPane() {
    override fun updateUI() {
      super.updateUI()
      val ui2 = if (ui is WindowsTabbedPaneUI) {
        object : WindowsTabbedPaneUI() {
          override fun createMouseListener() = object : TabSelectionMouseListener(this) {
            override fun mouseEntered(e: MouseEvent) {
              rolloverTab = tabForCoordinate(tabPane, e.x, e.y)
            }

            override fun mouseExited(e: MouseEvent) {
              rolloverTab = -1
            }
          }
        }
      } else {
        object : MetalTabbedPaneUI() {
          override fun createMouseListener() = object : TabSelectionMouseListener(this) {
            override fun mouseEntered(e: MouseEvent) {
              rolloverTab = tabForCoordinate(tabPane, e.x, e.y)
            }

            override fun mouseExited(e: MouseEvent) {
              rolloverTab = -1
            }
          }
        }
      }
      setUI(ui2)
    }
  }
  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTabbedPane(JTabbedPane(), "Default"))
    it.add(makeTabbedPane(tabbedPane, "requestFocusForVisibleComponent"))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabbedPane(
  tabs: JTabbedPane,
  title: String,
): JTabbedPane {
  tabs.componentPopupMenu = TabbedPanePopupMenu()
  tabs.addTab(title, JTextArea(title))
  tabs.addTab("000", JTextArea("000000000000000000000000"))
  tabs.addTab("111", JTextArea("1111111111111"))
  tabs.addTab("222", JTextArea("2222222222"))
  return tabs
}

private open class TabSelectionMouseListener(private val ui: BasicTabbedPaneUI) : MouseAdapter() {
  override fun mousePressed(e: MouseEvent) {
    val tabPane = e.component
    if (tabPane !is JTabbedPane || !tabPane.isEnabled || SwingUtilities.isRightMouseButton(e)) {
      return
    }
    val tabIndex = ui.tabForCoordinate(tabPane, e.x, e.y)
    if (tabIndex >= 0 && tabPane.isEnabledAt(tabIndex)) {
      if (tabIndex != tabPane.selectedIndex && e.clickCount < 2) {
        tabPane.selectedIndex = tabIndex
        val cmd = "requestFocusForVisibleComponent"
        val a = ActionEvent(tabPane, ActionEvent.ACTION_PERFORMED, cmd)
        EventQueue.invokeLater { tabPane.actionMap[cmd].actionPerformed(a) }
      } else if (tabPane.isRequestFocusEnabled) {
        tabPane.requestFocusInWindow()
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
        it.addTab("Title: $count", JTextArea("Tab: $count"))
        it.selectedIndex = it.tabCount - 1
        count++
      }
    }
    addSeparator()
    closePage = add("Close")
    closePage.addActionListener {
      (invoker as? JTabbedPane)?.also { it.remove(it.selectedIndex) }
    }
    addSeparator()
    closeAll = add("Close all")
    closeAll.addActionListener { (invoker as? JTabbedPane)?.removeAll() }
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
    if (c is JTabbedPane) {
      closePage.isEnabled = c.indexAtLocation(x, y) >= 0
      closeAll.isEnabled = c.tabCount > 0
      closeAllButActive.isEnabled = c.tabCount > 0
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
