package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val tabbedPane = EditableTabbedPane()
    tabbedPane.addTab("Title", JLabel("Tab1"))
    tabbedPane.addTab("aaa", JLabel("Tab2"))
    tabbedPane.addTab("000", JLabel("Tab3"))
    tabbedPane.setComponentPopupMenu(TabbedPanePopupMenu())

    add(tabbedPane)
    setPreferredSize(Dimension(320, 240))
  }
}

data class ComparableTab(val title: String, val component: Component)

class EditableTabbedPane : JTabbedPane() {
  private val glassPane = EditorGlassPane()
  private val editor = JTextField()
  private val startEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      getRootPane().setGlassPane(glassPane)
      val rect = getBoundsAt(getSelectedIndex())
      val p = SwingUtilities.convertPoint(this@EditableTabbedPane, rect.getLocation(), glassPane)
      // rect.setBounds(p.x + 2, p.y + 2, rect.width - 4, rect.height - 4)
      rect.setLocation(p)
      rect.grow(-2, -2)
      editor.setBounds(rect)
      editor.setText(getTitleAt(getSelectedIndex()))
      editor.selectAll()
      glassPane.add(editor)
      glassPane.setVisible(true)
      editor.requestFocusInWindow()
    }
  }
  private val cancelEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      glassPane.setVisible(false)
    }
  }

  init {
    editor.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3))
    val im = editor.getInputMap(JComponent.WHEN_FOCUSED)
    val am = editor.getActionMap()
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "rename-tab")
    am.put("rename-tab", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        if (editor.getText().trim().isNotEmpty()) {
          setTitleAt(getSelectedIndex(), editor.getText())
          getTabComponentAt(getSelectedIndex())?.revalidate()
        }
        glassPane.setVisible(false)
      }
    })
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel-editing")
    am.put("cancel-editing", cancelEditing)

    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val isDoubleClick = e.getClickCount() >= 2
        if (isDoubleClick) {
          val c = e.getComponent()
          startEditing.actionPerformed(ActionEvent(c, ActionEvent.ACTION_PERFORMED, ""))
        }
      }
    })
    getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "start-editing")
    getActionMap().put("start-editing", startEditing)
  }

  private inner class EditorGlassPane : JComponent() {
    init {
      setOpaque(false)
      setFocusTraversalPolicy(object : DefaultFocusTraversalPolicy() {
        override fun accept(c: Component) = c == editor
      })
      addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          val tabEditor = editor
          val cmd = "rename-tab"
          tabEditor.getActionMap().get(cmd)
            ?.takeUnless { tabEditor.getBounds().contains(e.getPoint()) }
            ?.also {
              val c = e.getComponent()
              it.actionPerformed(ActionEvent(c, ActionEvent.ACTION_PERFORMED, cmd))
            }
        }
      })
    }

    override fun setVisible(flag: Boolean) {
      super.setVisible(flag)
      setFocusTraversalPolicyProvider(flag)
      setFocusCycleRoot(flag)
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
      val tabs = getInvoker() as? JTabbedPane ?: return@addActionListener
      tabs.addTab("Title: $count", JLabel("Tab: $count"))
      tabs.setSelectedIndex(tabs.getTabCount() - 1)
      count++
    }
    sortTabs = add("Sort")
    sortTabs.addActionListener {
      val tabs = getInvoker() as? JTabbedPane ?: return@addActionListener
      val list = (0 until tabs.getTabCount())
        .map { ComparableTab(tabs.getTitleAt(it), tabs.getComponentAt(it)) }
        .sortedWith(compareBy(ComparableTab::title))
      tabs.removeAll()
      list.forEach { tabs.addTab(it.title, it.component) }
    }
    addSeparator()
    closePage = add("Close")
    closePage.addActionListener {
      (getInvoker() as? JTabbedPane)?.also {
        it.remove(it.getSelectedIndex())
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
        val tabIdx = it.getSelectedIndex()
        val title = it.getTitleAt(tabIdx)
        val cmp = it.getComponentAt(tabIdx)
        it.removeAll()
        it.addTab(title, cmp)
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    val tabs = c as? JTabbedPane ?: return
    sortTabs.setEnabled(tabs.getTabCount() > 1)
    closePage.setEnabled(tabs.indexAtLocation(x, y) >= 0)
    closeAll.setEnabled(tabs.getTabCount() > 0)
    closeAllButActive.setEnabled(tabs.getTabCount() > 0)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
