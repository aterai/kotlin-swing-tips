package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private val INFO = """
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

private class TabTitleEditListener(
  val tabs: JTabbedPane,
) : MouseAdapter(),
  ChangeListener,
  DocumentListener {
  private val editor = JTextField()
  private var editingIdx = -1
  private var len = -1
  private var dim: Dimension? = null
  private var tabComponent: Component? = null
  private val startEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      editingIdx = tabs.selectedIndex
      tabComponent = tabs.getTabComponentAt(editingIdx)
      tabs.setTabComponentAt(editingIdx, editor)
      editor.isVisible = true
      editor.text = tabs.getTitleAt(editingIdx)
      editor.selectAll()
      editor.requestFocusInWindow()
      len = editor.text.length
      dim = editor.preferredSize
      editor.minimumSize = dim
    }
  }
  private val renameTab = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val title = editor.text.trim()
      if (editingIdx >= 0 && title.isNotEmpty()) {
        tabs.setTitleAt(editingIdx, title)
      }
      cancelEditing.actionPerformed(
        ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, CANCEL),
      )
    }
  }
  private val cancelEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (editingIdx >= 0) {
        tabs.setTabComponentAt(editingIdx, tabComponent)
        editor.isVisible = false
        editingIdx = -1
        len = -1
        tabComponent = null
        editor.preferredSize = null
        tabs.requestFocusInWindow()
      }
    }
  }

  init {
    editor.border = BorderFactory.createEmptyBorder()
    val fl = object : FocusAdapter() {
      override fun focusLost(e: FocusEvent) {
        renameTab.actionPerformed(
          ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, RENAME),
        )
      }
    }
    editor.addFocusListener(fl)
    val im = editor.getInputMap(JComponent.WHEN_FOCUSED)
    val am = editor.actionMap
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL)
    am.put(CANCEL, cancelEditing)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), RENAME)
    am.put(RENAME, renameTab)
    editor.document.addDocumentListener(this)
    val im2 = tabs.getInputMap(JComponent.WHEN_FOCUSED)
    im2.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), START)
    tabs.actionMap.put(START, startEditing)
  }

  override fun stateChanged(e: ChangeEvent) {
    renameTab.actionPerformed(ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, RENAME))
  }

  override fun insertUpdate(e: DocumentEvent) {
    updateTabSize()
  }

  override fun removeUpdate(e: DocumentEvent) {
    updateTabSize()
  }

  override fun changedUpdate(e: DocumentEvent) {
    // not needed
  }

  override fun mouseClicked(e: MouseEvent) {
    val r = tabs.getBoundsAt(tabs.selectedIndex)
    val isDoubleClick = e.clickCount >= 2
    if (isDoubleClick && r.contains(e.point)) {
      startEditing.actionPerformed(
        ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, START),
      )
    } else {
      renameTab.actionPerformed(
        ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, RENAME),
      )
    }
  }

  private fun updateTabSize() {
    editor.preferredSize = if (editor.text.length > len) null else dim
    tabs.revalidate()
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
