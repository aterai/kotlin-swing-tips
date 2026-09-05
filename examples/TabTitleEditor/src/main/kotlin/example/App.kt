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
import kotlin.math.max

private val INFO = """
  Start editing: Double-Click, Enter-Key
  Commit rename: field-focusLost, Enter-Key
  Cancel editing: Esc-Key, title.isEmpty
""".trimIndent()

fun createUI() = JTabbedPane().also {
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
  val tabbedPane: JTabbedPane,
) : MouseAdapter(),
  ChangeListener,
  DocumentListener {
  private val editor = JTextField()
  private var editingIdx = -1
  private var minSize: Dimension? = null
  private var tabComponent: Component? = null
  private val startEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val idx = tabbedPane.selectedIndex
      if (editingIdx < 0 && idx >= 0) {
        startEditingAt(idx)
      }
    }
  }
  private val renameTabTitle = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val title = editor.text.trim()
      if (editingIdx >= 0 && title.isNotEmpty()) {
        tabbedPane.setTitleAt(editingIdx, title)
      }
      cancelEditing.actionPerformed(
        ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, CANCEL_EDITING),
      )
    }
  }
  private val cancelEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (editingIdx >= 0) {
        tabbedPane.setTabComponentAt(editingIdx, tabComponent)
        editingIdx = -1
        minSize = null
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
        renameTabTitle.actionPerformed(
          ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, RENAME_TAB_TITLE),
        )
      }
    }
    editor.addFocusListener(fl)
    editor.document.addDocumentListener(this)

    val enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
    val im = editor.getInputMap(JComponent.WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL_EDITING)
    im.put(enterKey, RENAME_TAB_TITLE)

    val am = editor.actionMap
    am.put(CANCEL_EDITING, cancelEditing)
    am.put(RENAME_TAB_TITLE, renameTabTitle)

    tabbedPane.getInputMap(JComponent.WHEN_FOCUSED).put(enterKey, START_EDITING)
    tabbedPane.actionMap.put(START_EDITING, startEditing)
  }

  override fun stateChanged(e: ChangeEvent) {
    val a = ActionEvent(
      tabbedPane,
      ActionEvent.ACTION_PERFORMED,
      RENAME_TAB_TITLE,
    )
    renameTabTitle.actionPerformed(a)
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
    val idx = tabbedPane.indexAtLocation(e.getX(), e.getY())
    val isDoubleClick = e.getClickCount() >= 2
    if (isDoubleClick && idx >= 0 && idx == tabbedPane.selectedIndex) {
      val a = ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, START_EDITING)
      startEditing.actionPerformed(a)
    } else {
      val a = ActionEvent(
        tabbedPane,
        ActionEvent.ACTION_PERFORMED,
        RENAME_TAB_TITLE,
      )
      renameTabTitle.actionPerformed(a)
    }
  }

  fun startEditingAt(index: Int) {
    editingIdx = index
    tabComponent = tabbedPane.getTabComponentAt(index)
    tabbedPane.setTabComponentAt(index, editor)
    // updateTabSize() called from setText(...) does nothing while minSize is null
    editor.setPreferredSize(null)
    editor.text = tabbedPane.getTitleAt(index)
    minSize = editor.getPreferredSize()
    editor.selectAll()
    editor.requestFocusInWindow()
  }

  fun updateTabSize() {
    if (minSize != null) {
      // Grow to fit the text, but never shrink below the initial title width
      editor.setPreferredSize(null)
      val d = editor.getPreferredSize()
      d.width = max(d.width, minSize!!.width)
      editor.preferredSize = d
      tabbedPane.revalidate()
    }
  }

  companion object {
    private const val START_EDITING = "start-editing"
    private const val CANCEL_EDITING = "cancel-editing"
    private const val RENAME_TAB_TITLE = "rename-tab-title"
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
