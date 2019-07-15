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

class MainPanel : JPanel(BorderLayout()) {
  init {
    add(JTabbedPane().also {
      val l = TabTitleEditListener(it)
      it.addChangeListener(l)
      it.addMouseListener(l)
      it.addTab("Shortcuts", JTextArea(INFO))
      it.addTab("badfasdfa", JLabel("bbbbbbbbbbbafasdf"))
      it.addTab("cccc", JScrollPane(JTree()))
      it.addTab("dddddddd", JLabel("dadfasdfasd"))
    })
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    private const val INFO = """ Start editing: Double-Click, Enter-Key
 Commit rename: field-focusLost, Enter-Key
Cancel editing: Esc-Key, title.isEmpty
"""
  }
}

class TabTitleEditListener(val tabbedPane: JTabbedPane) : MouseAdapter(), ChangeListener, DocumentListener {
  protected val editor = JTextField()
  protected var editingIdx = -1
  protected var len = -1
  protected var dim: Dimension? = null
  protected var tabComponent: Component? = null
  protected val startEditing: Action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      editingIdx = tabbedPane.getSelectedIndex()
      tabComponent = tabbedPane.getTabComponentAt(editingIdx)
      tabbedPane.setTabComponentAt(editingIdx, editor)
      editor.setVisible(true)
      editor.setText(tabbedPane.getTitleAt(editingIdx))
      editor.selectAll()
      editor.requestFocusInWindow()
      len = editor.getText().length
      dim = editor.getPreferredSize()
      editor.setMinimumSize(dim)
    }
  }
  protected val renameTabTitle: Action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val title = editor.getText().trim { it <= ' ' }
      if (editingIdx >= 0 && !title.isEmpty()) {
        tabbedPane.setTitleAt(editingIdx, title)
      }
      cancelEditing.actionPerformed(ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, CANCEL))
    }
  }
  protected val cancelEditing: Action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (editingIdx >= 0) {
        tabbedPane.setTabComponentAt(editingIdx, tabComponent)
        editor.setVisible(false)
        editingIdx = -1
        len = -1
        tabComponent = null
        editor.setPreferredSize(null)
        tabbedPane.requestFocusInWindow()
      }
    }
  }

  init {
    editor.setBorder(BorderFactory.createEmptyBorder())
    editor.addFocusListener(object : FocusAdapter() {
      override fun focusLost(e: FocusEvent) {
        renameTabTitle.actionPerformed(ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, RENAME))
      }
    })
    val im = editor.getInputMap(JComponent.WHEN_FOCUSED)
    val am = editor.getActionMap()
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL)
    am.put(CANCEL, cancelEditing)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), RENAME)
    am.put(RENAME, renameTabTitle)
    editor.getDocument().addDocumentListener(this)
    tabbedPane.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), START)
    tabbedPane.getActionMap().put(START, startEditing)
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

  override fun changedUpdate(e: DocumentEvent) { /* not needed */ }

  override fun mouseClicked(e: MouseEvent) {
    val r = tabbedPane.getBoundsAt(tabbedPane.getSelectedIndex())
    val isDoubleClick = e.getClickCount() >= 2
    if (isDoubleClick && r.contains(e.getPoint())) {
      startEditing.actionPerformed(ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, START))
    } else {
      renameTabTitle.actionPerformed(ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, RENAME))
    }
  }

  protected fun updateTabSize() {
    editor.setPreferredSize(if (editor.getText().length > len) null else dim)
    tabbedPane.revalidate()
  }

  companion object {
    protected const val START = "start-editing"
    protected const val CANCEL = "cancel-editing"
    protected const val RENAME = "rename-tab-title"
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
