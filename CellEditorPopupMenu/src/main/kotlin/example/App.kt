package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.table.DefaultTableModel
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent
import javax.swing.undo.UndoManager

class MainPanel : JPanel(BorderLayout()) {
  private val columnNames = arrayOf("String", "String")
  private val data = arrayOf(
      arrayOf<Any>("Undo", "Ctrl Z"),
      arrayOf<Any>("Redo", "Ctrl Y"),
      arrayOf<Any>("AAA", "bbbbbb"),
      arrayOf<Any>("CCC", "ddddddd"))
  private val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  private val table = JTable(model)

  init {
    table.setAutoCreateRowSorter(true)
    val ce = table.getDefaultEditor(Any::class.java) as DefaultCellEditor
    val textField = ce.getComponent() as JTextComponent
    val popup = TextComponentPopupMenu(textField)
    textField.setComponentPopupMenu(popup)
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }
}

internal class TextComponentPopupMenu(tc: JTextComponent) : JPopupMenu() {
  init {
    val cutAction = DefaultEditorKit.CutAction()
    add(cutAction)
    val copyAction = DefaultEditorKit.CopyAction()
    add(copyAction)
    val pasteAction = DefaultEditorKit.PasteAction()
    add(pasteAction)
    val deleteAction = DeleteAction()
    add(deleteAction)
    addSeparator()

    val manager = UndoManager()
    val undoAction = UndoAction(manager)
    add(undoAction)

    val redoAction = RedoAction(manager)
    add(redoAction)

    tc.addAncestorListener(object : AncestorListener {
      override fun ancestorAdded(e: AncestorEvent) {
        manager.discardAllEdits()
        e.getComponent().requestFocusInWindow()
      }

      override fun ancestorMoved(e: AncestorEvent) { /* not needed */ }

      override fun ancestorRemoved(e: AncestorEvent) { /* not needed */ }
    })
    tc.getDocument().addUndoableEditListener(manager)
    tc.getActionMap().put("undo", undoAction)
    tc.getActionMap().put("redo", redoAction)
    val msk = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
    val imap = tc.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, msk), "undo")
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, msk), "redo")

    addPopupMenuListener(object : PopupMenuListener {
      override fun popupMenuCanceled(e: PopupMenuEvent) { /* not needed */ }

      override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
        undoAction.setEnabled(true)
        redoAction.setEnabled(true)
      }

      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        val tcmp = getInvoker() as? JTextComponent
        val hasSelectedText = tcmp?.getSelectedText() != null
        cutAction.setEnabled(hasSelectedText)
        copyAction.setEnabled(hasSelectedText)
        deleteAction.setEnabled(hasSelectedText)
        undoAction.setEnabled(manager.canUndo())
        redoAction.setEnabled(manager.canRedo())
      }
    })
  }
}

internal class UndoAction(private val undoManager: UndoManager) : AbstractAction("undo") {
  override fun actionPerformed(e: ActionEvent) {
    runCatching {
      undoManager.undo()
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
    }
  }
}

internal class RedoAction(private val undoManager: UndoManager) : AbstractAction("redo") {
  override fun actionPerformed(e: ActionEvent) {
    runCatching {
      undoManager.redo()
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
    }
  }
}

internal class DeleteAction : AbstractAction("delete") {
  override fun actionPerformed(e: ActionEvent) {
    val c = e.getSource() as? Component ?: return
    val pop = SwingUtilities.getUnwrappedParent(c) as? JPopupMenu ?: return
    (pop.getInvoker() as? JTextComponent)?.replaceSelection(null)
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
