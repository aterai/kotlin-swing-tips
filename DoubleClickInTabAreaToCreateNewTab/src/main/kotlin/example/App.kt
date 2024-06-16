package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
  val tabbedPane = JTabbedPane()
  tabbedPane.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  val addAction: Action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      (e.source as? JTabbedPane)?.also {
        val cnt = it.tabCount
        it.addTab("Untitled-$cnt", JScrollPane(JTextArea()))
        it.selectedIndex = cnt
      }
    }
  }
  val im = tabbedPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  val am = tabbedPane.actionMap
  val addKey = "AddTab"
  addAction.putValue(Action.ACTION_COMMAND_KEY, addKey)
  im.put(KeyStroke.getKeyStroke("ctrl N"), addKey)
  am.put(addKey, addAction)

  val removeKey = "RemoveTab"
  val removeAction = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      (e.source as? JTabbedPane)?.also {
        it.remove(it.selectedIndex)
      }
    }
  }
  removeAction.putValue(Action.ACTION_COMMAND_KEY, removeKey)
  im.put(KeyStroke.getKeyStroke("ctrl W"), removeKey)
  am.put(removeKey, removeAction)
  tabbedPane.componentPopupMenu = TabbedPanePopupMenu()
  val help = "Double-click in tab area to quickly create a new tab."
  tabbedPane.addTab("Title", JScrollPane(JTextArea(help)))
  tabbedPane.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      val leftButton = e.button == MouseEvent.BUTTON1
      val doubleClick = e.clickCount >= 2
      val tabs = e.component as JTabbedPane
      val idx = tabs.indexAtLocation(e.x, e.y)
      val r = getTabAreaBounds(tabs)
      val b = idx < 0 && r.contains(e.point)
      if (leftButton && doubleClick && b) {
        tabs.actionMap[addKey]?.also { a ->
          a.actionPerformed(ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, addKey))
        }
      }
    }
  })
  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getTabAreaBounds(tabbedPane: JTabbedPane): Rectangle {
  val r = SwingUtilities.calculateInnerArea(tabbedPane, null)
  val cr = tabbedPane.selectedComponent?.bounds ?: Rectangle()
  val tp = tabbedPane.tabPlacement
  val i1 = UIManager.getInsets("TabbedPane.tabAreaInsets")
  val i2 = UIManager.getInsets("TabbedPane.contentBorderInsets")
  if (tp == SwingConstants.TOP || tp == SwingConstants.BOTTOM) {
    r.height -= cr.height + i1.top + i1.bottom + i2.top + i2.bottom
    r.y += if (tp == SwingConstants.TOP) i1.top else cr.y + cr.height + i1.bottom + i2.bottom
  } else {
    r.width -= cr.width + i1.top + i1.bottom + i2.left + i2.right
    r.x += if (tp == SwingConstants.LEFT) i1.top else cr.x + cr.width + i1.bottom + i2.right
  }
  return r
}

private class TabbedPanePopupMenu : JPopupMenu() {
  private val removeTab: JMenuItem
  private val closeAll: JMenuItem
  private val closeAllButActive: JMenuItem

  init {
    val addTab = add("New tab")
    addTab.actionCommand = "AddTab"
    addTab.addActionListener {
      (invoker as? JTabbedPane)?.also { tabs ->
        val key = addTab.actionCommand
        tabs.actionMap[key]?.also {
          it.actionPerformed(ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, key))
        }
      }
    }
    addSeparator()

    removeTab = add("Close")
    removeTab.actionCommand = "RemoveTab"
    removeTab.addActionListener {
      (invoker as? JTabbedPane)?.also { tabs ->
        val key = addTab.actionCommand
        tabs.actionMap[key]?.also {
          it.actionPerformed(ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, key))
        }
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

  override fun show(
    c: Component,
    x: Int,
    y: Int,
  ) {
    if (c is JTabbedPane) {
      removeTab.isEnabled = c.indexAtLocation(x, y) >= 0
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
