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

fun makeUI(): Component {
  val columnNames = arrayOf("String", "String")
  val data = arrayOf(
    arrayOf("Undo", "Ctrl Z"),
    arrayOf("Redo", "Ctrl Y"),
    arrayOf("AAA", "bbb bbb"),
    arrayOf("CCC", "ddd ddd"))
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  val editor = table.getDefaultEditor(Any::class.java)
  ((editor as? DefaultCellEditor)?.component as? JTextComponent)?.also {
    it.componentPopupMenu = TextComponentPopupMenu(it)
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TextComponentPopupMenu(tc: JTextComponent) : JPopupMenu() {
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
        e.component.requestFocusInWindow()
      }

      override fun ancestorMoved(e: AncestorEvent) {
        /* not needed */
      }

      override fun ancestorRemoved(e: AncestorEvent) {
        /* not needed */
      }
    })
    tc.document.addUndoableEditListener(manager)
    tc.actionMap.put("undo", undoAction)
    tc.actionMap.put("redo", redoAction)
    val msk = Toolkit.getDefaultToolkit().menuShortcutKeyMask
    val im = tc.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, msk), "undo")
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, msk), "redo")

    addPopupMenuListener(object : PopupMenuListener {
      override fun popupMenuCanceled(e: PopupMenuEvent) {
        /* not needed */
      }

      override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
        undoAction.isEnabled = true
        redoAction.isEnabled = true
      }

      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        val textComponent = invoker as? JTextComponent
        val hasSelectedText = textComponent?.selectedText != null
        cutAction.isEnabled = hasSelectedText
        copyAction.isEnabled = hasSelectedText
        deleteAction.isEnabled = hasSelectedText
        undoAction.isEnabled = manager.canUndo()
        redoAction.isEnabled = manager.canRedo()
      }
    })
  }
}

private class UndoAction(private val undoManager: UndoManager) : AbstractAction("undo") {
  override fun actionPerformed(e: ActionEvent) {
    runCatching {
      undoManager.undo()
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
    }
  }
}

private class RedoAction(private val undoManager: UndoManager) : AbstractAction("redo") {
  override fun actionPerformed(e: ActionEvent) {
    runCatching {
      undoManager.redo()
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
    }
  }
}

private class DeleteAction : AbstractAction("delete") {
  override fun actionPerformed(e: ActionEvent) {
    val c = e.source as? Component ?: return
    val pop = SwingUtilities.getUnwrappedParent(c) as? JPopupMenu ?: return
    (pop.invoker as? JTextComponent)?.replaceSelection(null)
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
