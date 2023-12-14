package example

import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val copyKey = "copy"

  val combo1 = JComboBox(makeModel(5))
  val copy = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      (e.source as? JComboBox<*>)?.selectedItem?.also {
        val contents = StringSelection(it.toString())
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(contents, null)
        // println(it)
      }
    }
  }
  val am = combo1.actionMap
  am.put(copyKey, copy)
  val modifiers = InputEvent.CTRL_DOWN_MASK
  val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, modifiers)

  val im = combo1.getInputMap(JComponent.WHEN_FOCUSED)
  im.put(keyStroke, copyKey)
  val popup = JPopupMenu()
  popup.add(copyKey).addActionListener { e ->
    val o = popup.invoker
    val c = o as? JComboBox<*> ?: SwingUtilities.getAncestorOfClass(JComboBox::class.java, o)
    (c as? JComboBox<*>)?.also {
      val a = it.actionMap.get(copyKey)
      a.actionPerformed(ActionEvent(it, e.id, e.actionCommand))
      // val keyEvent = KeyEvent(c, 0, 0, 0, 0, 'C')
      // SwingUtilities.notifyAction(a, keyStroke, keyEvent, it, modifiers)
    }
  }
  combo1.componentPopupMenu = popup

  val combo2 = JComboBox(makeModel(10))
  combo2.isEditable = true
  (combo2.editor.editorComponent as? JTextField)?.componentPopupMenu = TextFieldPopupMenu()

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("Default:", JComboBox(makeModel(0))))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Editable: false, JPopupMenu, Ctrl+C", combo1))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Editable: true, JPopupMenu, Ctrl+C", combo2))
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val textArea = JTextArea()
  textArea.componentPopupMenu = TextFieldPopupMenu()

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private fun makeModel(start: Int) = DefaultComboBoxModel<String>().also {
  for (i in start until start + 5) {
    it.addElement("item: $i")
  }
}

private class TextFieldPopupMenu : JPopupMenu() {
  // private val pasteAction = DefaultEditorKit.PasteAction()
  private val cutAction = DefaultEditorKit.CutAction()
  private val copyAction = DefaultEditorKit.CopyAction()
  private val deleteAction = object : AbstractAction("delete") {
    override fun actionPerformed(e: ActionEvent) {
      (invoker as? JTextComponent)?.replaceSelection(null)
    }
  }

  init {
    add(cutAction)
    add(copyAction)
    add(DefaultEditorKit.PasteAction())
    add(deleteAction)
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JTextComponent) {
      val hasSelectedText = c.selectedText != null
      cutAction.isEnabled = hasSelectedText
      copyAction.isEnabled = hasSelectedText
      deleteAction.isEnabled = hasSelectedText
      super.show(c, x, y)
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
