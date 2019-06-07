package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val a = JTextArea(INFO)
    a.setEditable(false)
    val tabbedPane = EditableTabbedPane().also {
      it.addTab("Shortcuts", JScrollPane(a))
      it.addTab("badfasdf", JLabel("bbbbbbbbbbbafasdf"))
      it.addTab("cccc", JScrollPane(JTree()))
      it.addTab("ddddddd", JButton("dadfasdfasd"))
    }
    add(tabbedPane)
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    private const val INFO = """Start editing: Double-Click, Enter-Key
Commit rename: field-focusLost, Enter-Key
Cancel editing: Esc-Key, title.isEmpty
"""
  }
}

internal class EditableTabbedPane : JTabbedPane() {
  protected val glassPane: Container = EditorGlassPane()
  protected val editor = JTextField()
  protected val startEditing: Action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      getRootPane().setGlassPane(glassPane)
      val rect = getBoundsAt(getSelectedIndex())
      val p = SwingUtilities.convertPoint(this@EditableTabbedPane, rect.getLocation(), glassPane)
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
  protected val cancelEditing: Action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      glassPane.setVisible(false)
    }
  }
  protected val renameTab: Action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (!editor.getText().trim { it <= ' ' }.isEmpty()) {
        setTitleAt(getSelectedIndex(), editor.getText())
        (getTabComponentAt(getSelectedIndex()) as? JComponent)?.revalidate()
      }
      glassPane.setVisible(false)
    }
  }

  init {
    editor.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3))
    val am = editor.getActionMap()
    val im = editor.getInputMap(JComponent.WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "rename-tab")
    am.put("rename-tab", renameTab)

    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel-editing")
    am.put("cancel-editing", cancelEditing)

    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val isDoubleClick = e.getClickCount() >= 2
        if (isDoubleClick) {
          startEditing.actionPerformed(ActionEvent(e.getComponent(), ActionEvent.ACTION_PERFORMED, ""))
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
          if (!editor.getBounds().contains(e.getPoint())) {
            renameTab.actionPerformed(ActionEvent(e.getComponent(), ActionEvent.ACTION_PERFORMED, ""))
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
