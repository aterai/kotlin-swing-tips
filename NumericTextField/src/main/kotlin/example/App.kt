package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.text.NumberFormat
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.DocumentFilter
import javax.swing.text.JTextComponent
import javax.swing.text.NumberFormatter
import javax.swing.text.PlainDocument

fun makeUI(): Component {
  val textField1 = JTextField("1000")
  textField1.horizontalAlignment = SwingConstants.RIGHT
  textField1.inputVerifier = IntegerInputVerifier()

  val textField2 = JTextField()
  textField2.document = IntegerDocument()
  textField2.text = "2000"

  val textField3 = JTextField()
  (textField3.document as? AbstractDocument)?.documentFilter = IntegerDocumentFilter()
  textField3.text = "3000"

  val textField4 = JFormattedTextField()
  val formatter = NumberFormatter()
  // formatter.valueClass = java.lang.Integer::class.java
  (formatter.format as? NumberFormat)?.isGroupingUsed = false
  textField4.formatterFactory = DefaultFormatterFactory(formatter, formatter, formatter)
  textField4.horizontalAlignment = SwingConstants.RIGHT
  textField4.value = 4000

  val spinner = JSpinner(SpinnerNumberModel(0, 0, Int.MAX_VALUE, 1))
  (spinner.editor as? JSpinner.NumberEditor)?.format?.isGroupingUsed = false
  spinner.value = 5000

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createTitledBorder("TextField")
  box.add(JLabel("InputVerifier"))
  box.add(textField1)
  box.add(Box.createVerticalStrut(10))
  box.add(JLabel("Custom Document"))
  box.add(textField2)
  box.add(Box.createVerticalStrut(10))
  box.add(JLabel("DocumentFilter"))
  box.add(textField3)
  box.add(Box.createVerticalStrut(10))
  box.add(JLabel("FormatterFactory"))
  box.add(textField4)
  box.add(Box.createVerticalStrut(10))

  val p = JPanel(BorderLayout(5, 5))
  p.border = BorderFactory.createTitledBorder("Spinner")
  p.add(JLabel("SpinnerNumberModel"), BorderLayout.NORTH)
  p.add(spinner)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(Box.createRigidArea(Dimension(320, 16)))
    it.add(p, BorderLayout.SOUTH)
  }
}

private class IntegerInputVerifier : InputVerifier() {
  override fun verify(c: JComponent) = runCatching {
    (c as? JTextComponent)?.text?.toInt() ?: false
  }.isSuccess
}

private class IntegerDocument : PlainDocument() {
  @Throws(BadLocationException::class)
  override fun insertString(offset: Int, str: String?, attributes: AttributeSet?) {
    if (str != null) {
      val newValue: String
      val length = length
      newValue = if (length == 0) {
        str
      } else {
        val currentContent = getText(0, length)
        val currentBuffer = StringBuilder(currentContent)
        currentBuffer.insert(offset, str)
        currentBuffer.toString()
      }
      checkInput(newValue, offset)
      super.insertString(offset, str, attributes)
    }
  }

  @Throws(BadLocationException::class)
  override fun remove(offset: Int, length: Int) {
    val currentLength = getLength()
    val currentContent = getText(0, currentLength)
    val before = currentContent.substring(0, offset)
    val after = currentContent.substring(length + offset, currentLength)
    val newValue = before + after
    checkInput(newValue, offset)
    super.remove(offset, length)
  }

  @Throws(BadLocationException::class)
  private fun checkInput(proposedValue: String, offset: Int) {
    if (proposedValue.isNotEmpty()) {
      kotlin.runCatching {
        proposedValue.toInt()
      }.onFailure {
        throw BadLocationException(proposedValue, offset)
      }
    }
  }
}

private class IntegerDocumentFilter : DocumentFilter() {
  @Throws(BadLocationException::class)
  override fun insertString(
    fb: FilterBypass,
    offset: Int,
    text: String?,
    attr: AttributeSet?
  ) {
    if (text != null) {
      replace(fb, offset, 0, text, attr)
    }
  }

  @Throws(BadLocationException::class)
  override fun remove(
    fb: FilterBypass,
    offset: Int,
    length: Int
  ) {
    replace(fb, offset, length, "", null)
  }

  @Throws(BadLocationException::class)
  override fun replace(
    fb: FilterBypass,
    offset: Int,
    length: Int,
    text: String?,
    attrs: AttributeSet?
  ) {
    val doc = fb.document
    val currentLength = doc.length
    val currentContent = doc.getText(0, currentLength)
    val before = currentContent.substring(0, offset)
    val after = currentContent.substring(length + offset, currentLength)
    val newValue = before + (text ?: "") + after
    checkInput(newValue)
    fb.replace(offset, length, text, attrs)
  }

  @Throws(BadLocationException::class)
  private fun checkInput(proposedValue: String) {
    if (proposedValue.isNotEmpty()) {
      runCatching {
        proposedValue.toInt()
      }
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
