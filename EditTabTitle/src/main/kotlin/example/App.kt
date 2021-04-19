package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private val INFO = """
  Start editing: Double-Click, Enter-Key
  Commit rename: field-focusLost, Enter-Key
  Cancel editing: Esc-Key, title.isEmpty
""".trimIndent()

fun makeUI(): Component {
  val a = JTextArea(INFO)
  a.isEditable = false
  val tabbedPane = EditableTabbedPane().also {
    it.addTab("Shortcuts", JScrollPane(a))
    it.addTab("JLabel", JLabel("label"))
    it.addTab("JTree", JScrollPane(JTree()))
    it.addTab("JButton", JButton("button"))
  }
  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private class EditableTabbedPane : JTabbedPane() {
  private val glassPane = EditorGlassPane()
  private val editor = JTextField()
  private val startEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      rootPane.glassPane = glassPane
      val rect = getBoundsAt(selectedIndex)
      val p = SwingUtilities.convertPoint(this@EditableTabbedPane, rect.location, glassPane)
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
  val renameTab = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (editor.text.trim().isNotEmpty()) {
        setTitleAt(selectedIndex, editor.text)
        getTabComponentAt(selectedIndex)?.revalidate()
      }
      glassPane.isVisible = false
    }
  }

  init {
    editor.border = BorderFactory.createEmptyBorder(0, 3, 0, 3)
    val am = editor.actionMap
    val im = editor.getInputMap(JComponent.WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "rename-tab")
    am.put("rename-tab", renameTab)

    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel-editing")
    am.put("cancel-editing", cancelEditing)

    val ml = object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val isDoubleClick = e.clickCount >= 2
        if (isDoubleClick) {
          startEditing.actionPerformed(ActionEvent(e.component, ActionEvent.ACTION_PERFORMED, ""))
        }
      }
    }
    addMouseListener(ml)
    getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "start-editing")
    actionMap.put("start-editing", startEditing)
  }

  private inner class EditorGlassPane : JComponent() {
    init {
      isOpaque = false
      focusTraversalPolicy = object : DefaultFocusTraversalPolicy() {
        override fun accept(c: Component) = c == editor
      }
      val ml = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          if (!editor.bounds.contains(e.point)) {
            renameTab.actionPerformed(ActionEvent(e.component, ActionEvent.ACTION_PERFORMED, ""))
          }
        }
      }
      addMouseListener(ml)
    }

    override fun setVisible(flag: Boolean) {
      super.setVisible(flag)
      isFocusTraversalPolicyProvider = flag
      isFocusCycleRoot = flag
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
