package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultEditorKit
import javax.swing.text.DocumentFilter
import javax.swing.text.TextAction

fun makeUI(): Component {
  val field = JTextField(12)
  (field.document as AbstractDocument).documentFilter = SizeFilter()
  // ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentSizeFilter(5));
  val am = field.actionMap
  var key = DefaultEditorKit.deletePrevCharAction // "delete-previous";
  am.put(key, SilentDeleteTextAction(key, am[key]))
  key = DefaultEditorKit.deleteNextCharAction // "delete-next";
  am.put(key, SilentDeleteTextAction(key, am[key]))
  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("Default", JTextField()))
    it.add(makeTitledPanel("Override delete-previous, delete-next beep", field))
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, cmp: Component): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  p.add(cmp, c)
  return p
}

private class SilentDeleteTextAction(name: String?, private val deleteAction: Action) : TextAction(name) {
  override fun actionPerformed(e: ActionEvent) {
    val target = getTextComponent(e)
    if (target != null && target.isEditable) {
      val caret = target.caret
      val dot = caret.dot
      val mark = caret.mark
      if (DefaultEditorKit.deletePrevCharAction == getValue(Action.NAME)) {
        // @see javax/swing/text/DefaultEditorKit.java DeletePrevCharAction
        if (dot == 0 && mark == 0) {
          return
        }
      } else {
        // @see javax/swing/text/DefaultEditorKit.java DeleteNextCharAction
        val doc = target.document
        if (dot == mark && doc.length == dot) {
          return
        }
      }
    }
    deleteAction.actionPerformed(e)
  }
}

private class SizeFilter : DocumentFilter() {
  @Throws(BadLocationException::class)
  override fun insertString(
    fb: FilterBypass,
    offset: Int,
    text: String,
    attr: AttributeSet?
  ) {
    val len = fb.document.length
    if (len + text.length > MAX) {
      Toolkit.getDefaultToolkit().beep()
      return
    }
    fb.insertString(offset, text, attr)
  }

  @Throws(BadLocationException::class)
  override fun remove(fb: FilterBypass, offset: Int, length: Int) {
    fb.remove(offset, length)
  }

  @Throws(BadLocationException::class)
  override fun replace(
    fb: FilterBypass,
    offset: Int,
    length: Int,
    text: String,
    attrs: AttributeSet?
  ) {
    val len = fb.document.length
    if (len - length + text.length > MAX) {
      Toolkit.getDefaultToolkit().beep()
      return
    }
    fb.replace(offset, length, text, attrs)
  }

  companion object {
    private const val MAX = 5
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
