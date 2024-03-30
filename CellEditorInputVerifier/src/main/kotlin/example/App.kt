package example

import java.awt.*
import java.text.NumberFormat
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.DocumentFilter
import javax.swing.text.JTextComponent
import javax.swing.text.NumberFormatter

fun makeUI(): Component {
  val textField1 = JTextField()
  initBorderAndAlignment(textField1)
  (textField1.document as? AbstractDocument)?.documentFilter = IntegerDocumentFilter()

  val textField2 = JTextField()
  initBorderAndAlignment(textField2)
  textField2.inputVerifier = IntegerInputVerifier()

  val textField3 = JFormattedTextField()
  initBorderAndAlignment(textField3)
  textField3.formatterFactory = NumberFormatterFactory()

  val names = arrayOf("Default", "DocumentFilter", "InputVerifier", "JFormattedTextField")
  val model = object : DefaultTableModel(names, 10) {
    override fun getColumnClass(column: Int) = Int::class.javaObjectType
  }
  val table = object : JTable(model) {
    override fun prepareEditor(
      editor: TableCellEditor,
      row: Int,
      column: Int,
    ): Component? {
      val c = super.prepareEditor(editor, row, column)
      (c as? JComponent)?.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
      return c
    }
  }
  table.columnModel.getColumn(1).cellEditor = DefaultCellEditor(textField1)
  table.columnModel.getColumn(2).cellEditor = object : DefaultCellEditor(textField2) {
    override fun stopCellEditing(): Boolean {
      val editor = component as? JComponent ?: return super.stopCellEditing()
      val isEditValid = editor.inputVerifier.verify(editor)
      editor.border = if (isEditValid) {
        BorderFactory.createEmptyBorder(1, 1, 1, 1)
      } else {
        BorderFactory.createLineBorder(Color.RED)
      }
      return isEditValid && super.stopCellEditing()
    }
  }
  table.columnModel.getColumn(3).cellEditor = object : DefaultCellEditor(textField3) {
    override fun stopCellEditing(): Boolean {
      val editor = component as? JFormattedTextField ?: return super.stopCellEditing()
      val isEditValid = editor.isEditValid
      editor.border = if (isEditValid) {
        BorderFactory.createEmptyBorder(1, 1, 1, 1)
      } else {
        BorderFactory.createLineBorder(Color.RED)
      }
      return isEditValid && super.stopCellEditing()
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initBorderAndAlignment(textField: JTextField) {
  textField.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
  textField.horizontalAlignment = SwingConstants.RIGHT
}

// Validating Text and Filtering Documents and Accessibility and the Java Access Bridge Tech Tips
// http://java.sun.com/developer/JDCTechTips/2005/tt0518.html
// Validating with Input Verifiers
private class IntegerInputVerifier : InputVerifier() {
  override fun verify(c: JComponent): Boolean {
    var verified = false
    if (c is JTextComponent) {
      val txt = c.text
      if (txt.isEmpty()) {
        return true
      }
      val iv = runCatching { Integer.parseInt(txt) }
        .onFailure { UIManager.getLookAndFeel().provideErrorFeedback(c) }
        .getOrNull()
        ?: -1
      verified = iv >= 0
      // try {
      //   val iv = Integer.parseInt(txt)
      //   verified = iv >= 0
      // } catch (ex: NumberFormatException) {
      //   UIManager.getLookAndFeel().provideErrorFeedback(c)
      // }
    }
    return verified
  }
}

// Validating Text and Filtering Documents and Accessibility and the Java Access Bridge Tech Tips
// http://java.sun.com/developer/JDCTechTips/2005/tt0518.html
// Validating with a Document Filter
private class IntegerDocumentFilter : DocumentFilter() {
  @Throws(BadLocationException::class)
  override fun insertString(
    fb: FilterBypass,
    offset: Int,
    text: String?,
    attr: AttributeSet,
  ) {
    if (text != null) {
      replace(fb, offset, 0, text, attr)
    }
  }

  @Throws(BadLocationException::class)
  override fun remove(
    fb: FilterBypass,
    offset: Int,
    length: Int,
  ) {
    replace(fb, offset, length, "", null)
  }

  @Throws(BadLocationException::class)
  override fun replace(
    fb: FilterBypass,
    offset: Int,
    length: Int,
    text: String?,
    attrs: AttributeSet?,
  ) {
    val doc = fb.document
    val currentLength = doc.length
    val currentContent = doc.getText(0, currentLength)
    val before = currentContent.substring(0, offset)
    val after = currentContent.substring(length + offset, currentLength)
    val newValue = before + (text ?: "") + after
    if (newValue.isEmpty()) {
      return
    }
    runCatching {
      Integer.parseInt(newValue)
    }.getOrNull()?.also {
      fb.replace(offset, length, text, attrs)
    }
  }
}

// How to Use Formatted Text Fields (The Java™ Tutorials > ... > Using Swing Components)
// https://docs.oracle.com/javase/tutorial/uiswing/components/formattedtextfield.html
private class NumberFormatterFactory : DefaultFormatterFactory(
  numberFormatter,
  numberFormatter,
  numberFormatter,
) {
  companion object {
    private val numberFormatter = NumberFormatter()

    init {
      numberFormatter.valueClass = Integer::class.java
      (numberFormatter.format as? NumberFormat)?.isGroupingUsed = false
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
