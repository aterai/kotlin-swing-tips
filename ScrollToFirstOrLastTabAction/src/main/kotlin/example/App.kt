package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tabs = JTabbedPane()
  tabs.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  val help = "Ctrl + ScrollButton Click: scroll to first/last tabs"
  tabs.addTab("title0", JLabel(help))
  for (i in 1 until 100) {
    tabs.addTab("title$i", JLabel("label$i"))
  }

  val forward = "scrollTabsForwardAction"
  tabs.actionMap.put(forward, ScrollTabsAction(tabs, forward))

  val backward = "scrollTabsBackwardAction"
  tabs.actionMap.put(backward, ScrollTabsAction(tabs, backward))

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ScrollTabsAction(
  private val tabbedPane: JTabbedPane,
  private val direction: String
) : AbstractAction() {
  private val index = if ("scrollTabsForwardAction" == direction) tabbedPane.tabCount - 1 else 0

  override fun actionPerformed(e: ActionEvent) {
    val action = tabbedPane.actionMap[direction]
    if (action?.isEnabled == true) {
      if (e.modifiers and ActionEvent.CTRL_MASK != 0) {
        scrollTabAt(tabbedPane, index)
      } else {
        action.actionPerformed(
          ActionEvent(
            tabbedPane,
            ActionEvent.ACTION_PERFORMED,
            null,
            e.getWhen(),
            e.modifiers
          )
        )
      }
    }
  }

  private fun scrollTabAt(tabbedPane: JTabbedPane, index: Int) {
    var cmp: Component? = null
    for (c in tabbedPane.components) {
      if ("TabbedPane.scrollableViewport" == c.name) {
        cmp = c
        break
      }
    }
    if (cmp is JViewport) {
      val d = tabbedPane.size
      val r = tabbedPane.getBoundsAt(index)
      val gw = (d.width - r.width) / 2
      r.grow(gw, 0)
      cmp.scrollRectToVisible(r)
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
