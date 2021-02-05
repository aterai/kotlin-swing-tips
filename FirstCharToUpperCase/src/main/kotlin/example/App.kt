package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val field = JTextField()
  (field.document as? AbstractDocument)?.documentFilter = FirstCharToUpperCaseDocumentFilter(field)
  field.text = "abc def ghi jkl mno"
  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("Default", JTextField("abc def ghi jkl mno")))
    it.add(makeTitledPanel("FirstCharToUpperCase", field))
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

private class FirstCharToUpperCaseDocumentFilter(private val textField: JTextComponent) : DocumentFilter() {
  @Throws(BadLocationException::class)
  override fun remove(fb: FilterBypass, offset: Int, length: Int) {
    val doc = fb.document
    if (offset == 0 && doc.length - length > 0) {
      fb.replace(length, 1, doc.getText(length, 1).toUpperCase(Locale.ENGLISH), null)
      textField.caretPosition = offset
    }
    fb.remove(offset, length)
  }

  @Throws(BadLocationException::class)
  override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: AttributeSet?) {
    val str = if (offset == 0 && text?.isNotEmpty() == true) {
      text.substring(0, 1).toUpperCase(Locale.ENGLISH) + text.substring(1)
    } else {
      text
    }
    fb.replace(offset, length, str, attrs)
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
