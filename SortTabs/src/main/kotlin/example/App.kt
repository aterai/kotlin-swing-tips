package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  val tabbedPane = EditableTabbedPane()
  tabbedPane.addTab("Title", JLabel("Tab1"))
  tabbedPane.addTab("aaa", JLabel("Tab2"))
  tabbedPane.addTab("000", JLabel("Tab3"))
  tabbedPane.componentPopupMenu = TabbedPanePopupMenu()

  it.add(tabbedPane)
  it.preferredSize = Dimension(320, 240)
}

private data class ComparableTab(val title: String, val component: Component)

private class EditableTabbedPane : JTabbedPane() {
  private val glassPane = EditorGlassPane()
  private val editor = JTextField()
  private val startEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      rootPane.glassPane = glassPane
      val rect = getBoundsAt(selectedIndex)
      val src = this@EditableTabbedPane
      val p = SwingUtilities.convertPoint(src, rect.location, glassPane)
      // rect.setBounds(p.x + 2, p.y + 2, rect.width - 4, rect.height - 4)
      rect.location = p
      rect.grow(-2, -2)
      editor.bounds = rect
      editor.text = getTitleAt(selectedIndex)
      editor.selectAll()
      glassPane.add(editor)
      glassPane.isVisible = true
      editor.requestFocusInWindow()
    }
  }
  private val cancelEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      glassPane.isVisible = false
    }
  }
  private val renameTab = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val str = editor.text.trim()
      if (str.isNotEmpty()) {
        setTitleAt(selectedIndex, str)
        getTabComponentAt(selectedIndex)?.revalidate()
      }
      glassPane.isVisible = false
    }
  }

  init {
    editor.border = BorderFactory.createEmptyBorder(0, 3, 0, 3)
    val im = editor.getInputMap(JComponent.WHEN_FOCUSED)
    val am = editor.actionMap

    val renameCmd = "rename-tab"
    val enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
    im.put(enterKey, renameCmd)
    am.put(renameCmd, renameTab)

    val cancelCmd = "cancel-editing"
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelCmd)
    am.put(cancelCmd, cancelEditing)

    val startCmd = "start-editing"
    getInputMap(JComponent.WHEN_FOCUSED).put(enterKey, startCmd)
    actionMap.put(startCmd, startEditing)
    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val isDoubleClick = e.clickCount >= 2
        if (isDoubleClick) {
          val c = e.component
          startEditing.actionPerformed(ActionEvent(c, ActionEvent.ACTION_PERFORMED, startCmd))
        }
      }
    })
  }

  private inner class EditorGlassPane : JComponent() {
    init {
      isOpaque = false
      focusTraversalPolicy = object : DefaultFocusTraversalPolicy() {
        override fun accept(c: Component) = c == editor
      }
      addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          val tabEditor = editor
          val cmd = "rename-tab"
          tabEditor.actionMap.get(cmd)
            ?.takeUnless { tabEditor.bounds.contains(e.point) }
            ?.also {
              val c = e.component
              it.actionPerformed(ActionEvent(c, ActionEvent.ACTION_PERFORMED, cmd))
            }
        }
      })
    }

    override fun setVisible(flag: Boolean) {
      super.setVisible(flag)
      isFocusTraversalPolicyProvider = flag
      isFocusCycleRoot = flag
    }
  }
}

class TabbedPanePopupMenu : JPopupMenu() {
  private var count = 0
  private val sortTabs: JMenuItem
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
    sortTabs = add("Sort")
    sortTabs.addActionListener {
      (invoker as? JTabbedPane)?.also { tabs ->
        val list = (0 until tabs.tabCount)
          .map { ComparableTab(tabs.getTitleAt(it), tabs.getComponentAt(it)) }
          .sortedWith(compareBy(ComparableTab::title))
        tabs.removeAll()
        list.forEach { tabs.addTab(it.title, it.component) }
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

  override fun show(c: Component, x: Int, y: Int) {
    val tabs = c as? JTabbedPane ?: return
    sortTabs.isEnabled = tabs.tabCount > 1
    closePage.isEnabled = tabs.indexAtLocation(x, y) >= 0
    closeAll.isEnabled = tabs.tabCount > 0
    closeAllButActive.isEnabled = tabs.tabCount > 0
    super.show(c, x, y)
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
