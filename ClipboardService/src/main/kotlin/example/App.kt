package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.jnlp.ClipboardService
import javax.jnlp.ServiceManager
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultEditorKit.CopyAction
import javax.swing.text.DefaultEditorKit.CutAction
import javax.swing.text.DefaultEditorKit.PasteAction
import javax.swing.text.JTextComponent
import javax.swing.undo.UndoManager

private val clipboardService: ClipboardService?
  get() = runCatching {
    ServiceManager.lookup("javax.jnlp.ClipboardService") as? ClipboardService
  }.getOrNull()

fun makeUI(): Component {
  val cs: ClipboardService? = clipboardService
  val textArea = object : JTextArea() {
    override fun copy() {
      if (cs != null) {
        cs.contents = StringSelection(selectedText)
      } else {
        super.copy()
      }
    }

    override fun cut() {
      if (cs != null) {
        cs.contents = StringSelection(selectedText)
      } else {
        super.cut()
      }
    }

    override fun paste() {
      if (cs != null) {
        val tr = cs.contents
        if (tr.isDataFlavorSupported(DataFlavor.stringFlavor)) {
          transferHandler.importData(this, tr)
        }
      } else {
        super.paste()
      }
    }
  }
  textArea.componentPopupMenu = TextComponentPopupMenu(textArea)

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("ClipboardService", JScrollPane(textArea)))
    it.add(makeTitledPanel("Default", JScrollPane(JTextArea())))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class TextComponentPopupMenu(textComponent: JTextComponent) : JPopupMenu() {
  private val cutAction = CutAction()
  private val copyAction = CopyAction()
  private val deleteAction = object : AbstractAction("delete") {
    override fun actionPerformed(e: ActionEvent) {
      (invoker as? JTextComponent)?.replaceSelection(null)
    }
  }

  init {
    add(cutAction)
    add(copyAction)
    add(PasteAction())
    add(deleteAction)
    addSeparator()
    val manager = UndoManager()
    val undoAction = UndoAction(manager)
    add(undoAction)
    val redoAction = RedoAction(manager)
    add(redoAction)
    textComponent.document.addUndoableEditListener(manager)
    textComponent.actionMap.put("undo", undoAction)
    textComponent.actionMap.put("redo", redoAction)
    val im = textComponent.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    val msk = Toolkit.getDefaultToolkit().menuShortcutKeyMask
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, msk), "undo")
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, msk), "redo")
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTextComponent) {
      val hasSelectedText = c.selectedText != null
      cutAction.isEnabled = hasSelectedText
      copyAction.isEnabled = hasSelectedText
      deleteAction.isEnabled = hasSelectedText
      super.show(c, x, y)
    }
  }
}

private class UndoAction(private val undoManager: UndoManager) : AbstractAction("undo") {
  override fun actionPerformed(e: ActionEvent) {
    runCatching {
      undoManager.undo()
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
    }
  }
}

private class RedoAction(private val undoManager: UndoManager) : AbstractAction("redo") {
  override fun actionPerformed(e: ActionEvent) {
    runCatching {
      undoManager.redo()
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
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
