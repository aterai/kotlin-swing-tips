package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private val INFO =
  """
  Start editing: Double-Click, Enter-Key
  Commit rename: field-focusLost, Enter-Key
  Cancel editing: Esc-Key, title.isEmpty
  """.trimIndent()

fun makeUI() = JTabbedPane().also {
  val l = TabTitleEditListener(it)
  it.addChangeListener(l)
  it.addMouseListener(l)
  it.addTab("Shortcuts", JTextArea(INFO))
  it.addTab("JLabel", JLabel("label"))
  it.addTab("JTree", JScrollPane(JTree()))
  it.addTab("JButton", JButton("button"))
  it.preferredSize = Dimension(320, 240)
}

private class TabTitleEditListener(val tabbedPane: JTabbedPane) : MouseAdapter(), ChangeListener, DocumentListener {
  private val editor = JTextField()
  private var editingIdx = -1
  private var len = -1
  private var dim: Dimension? = null
  private var tabComponent: Component? = null
  private val startEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      editingIdx = tabbedPane.selectedIndex
      tabComponent = tabbedPane.getTabComponentAt(editingIdx)
      tabbedPane.setTabComponentAt(editingIdx, editor)
      editor.isVisible = true
      editor.text = tabbedPane.getTitleAt(editingIdx)
      editor.selectAll()
      editor.requestFocusInWindow()
      len = editor.text.length
      dim = editor.preferredSize
      editor.minimumSize = dim
    }
  }
  private val renameTabTitle = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val title = editor.text.trim()
      if (editingIdx >= 0 && title.isNotEmpty()) {
        tabbedPane.setTitleAt(editingIdx, title)
      }
      cancelEditing.actionPerformed(ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, CANCEL))
    }
  }
  private val cancelEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (editingIdx >= 0) {
        tabbedPane.setTabComponentAt(editingIdx, tabComponent)
        editor.isVisible = false
        editingIdx = -1
        len = -1
        tabComponent = null
        editor.preferredSize = null
        tabbedPane.requestFocusInWindow()
      }
    }
  }

  init {
    editor.border = BorderFactory.createEmptyBorder()
    val fl = object : FocusAdapter() {
      override fun focusLost(e: FocusEvent) {
        renameTabTitle.actionPerformed(ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, RENAME))
      }
    }
    editor.addFocusListener(fl)
    val im = editor.getInputMap(JComponent.WHEN_FOCUSED)
    val am = editor.actionMap
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL)
    am.put(CANCEL, cancelEditing)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), RENAME)
    am.put(RENAME, renameTabTitle)
    editor.document.addDocumentListener(this)
    tabbedPane.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), START)
    tabbedPane.actionMap.put(START, startEditing)
  }

  override fun stateChanged(e: ChangeEvent) {
    renameTabTitle.actionPerformed(ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, RENAME))
  }

  override fun insertUpdate(e: DocumentEvent) {
    updateTabSize()
  }

  override fun removeUpdate(e: DocumentEvent) {
    updateTabSize()
  }

  override fun changedUpdate(e: DocumentEvent) { /* not needed */
  }

  override fun mouseClicked(e: MouseEvent) {
    val r = tabbedPane.getBoundsAt(tabbedPane.selectedIndex)
    val isDoubleClick = e.clickCount >= 2
    if (isDoubleClick && r.contains(e.point)) {
      startEditing.actionPerformed(ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, START))
    } else {
      renameTabTitle.actionPerformed(ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, RENAME))
    }
  }

  private fun updateTabSize() {
    editor.preferredSize = if (editor.text.length > len) null else dim
    tabbedPane.revalidate()
  }

  companion object {
    const val START = "start-editing"
    const val CANCEL = "cancel-editing"
    const val RENAME = "rename-tab-title"
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
