package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*

fun makeUI() = JPanel(BorderLayout()).also {
  val tabbedPane = EditableTabbedPane()
  tabbedPane.addTab("Title", JLabel("Tab1"))
  tabbedPane.addTab("aaa", JLabel("Tab2"))
  tabbedPane.addTab("000", JLabel("Tab3"))
  tabbedPane.componentPopupMenu = TabbedPanePopupMenu()

  it.add(tabbedPane)
  it.preferredSize = Dimension(320, 240)
}

private data class ComparableTab(
  val title: String,
  val component: Component,
)

private class EditableTabbedPane : JTabbedPane() {
  private val glassPane = object : JComponent() {
    override fun setVisible(flag: Boolean) {
      super.setVisible(flag)
      isFocusTraversalPolicyProvider = flag
      isFocusCycleRoot = flag
    }
  }
  private val editor = JTextField()
  private val startEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      rootPane.glassPane = glassPane
      val rect = getBoundsAt(selectedIndex)
      val src = this@EditableTabbedPane
      val p = SwingUtilities.convertPoint(src, rect.location, glassPane)
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
  private var listener: MouseListener? = null

  init {
    editor.border = BorderFactory.createEmptyBorder(0, 3, 0, 3)
    val enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
    val im = editor.getInputMap(WHEN_FOCUSED)
    im.put(enterKey, EDIT_KEY)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL_KEY)

    val am = editor.actionMap
    am.put(EDIT_KEY, renameTab)
    am.put(CANCEL_KEY, cancelEditing)

    getInputMap(WHEN_FOCUSED).put(enterKey, START_EDITING)
    actionMap.put(START_EDITING, startEditing)

    glassPane.focusTraversalPolicy = object : DefaultFocusTraversalPolicy() {
      override fun accept(c: Component) = c == editor
    }
    glassPane.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (!editor.bounds.contains(e.point)) {
          actionPerformed(e.component, renameTab, EDIT_KEY)
        }
      }
    })
  }

  override fun updateUI() {
    removeMouseListener(listener)
    super.updateUI()
    listener = object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val isDoubleClick = e.clickCount >= 2
        if (isDoubleClick) {
          actionPerformed(e.component, startEditing, START_EDITING)
        }
      }
    }
    addMouseListener(listener)
    EventQueue.invokeLater {
      SwingUtilities.updateComponentTreeUI(editor)
    }
  }

  private fun actionPerformed(
    c: Component,
    a: Action,
    command: String,
  ) {
    a.actionPerformed(ActionEvent(c, ActionEvent.ACTION_PERFORMED, command))
  }

  companion object {
    const val EDIT_KEY = "rename-tab"
    const val CANCEL_KEY = "cancel-editing"
    const val START_EDITING = "start-editing"
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
        val list = (0..<tabs.tabCount)
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

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
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
