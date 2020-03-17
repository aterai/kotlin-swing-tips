package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.text.NumberFormat
import java.util.Objects
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.DocumentFilter
import javax.swing.text.JTextComponent
import javax.swing.text.NumberFormatter

class MainPanel : JPanel(BorderLayout()) {
  init {
    val textField1 = JTextField()
    initBorderAndAlignment(textField1)
    (textField1.getDocument() as? AbstractDocument)?.setDocumentFilter(IntegerDocumentFilter())

    val textField2 = JTextField()
    initBorderAndAlignment(textField2)
    textField2.setInputVerifier(IntegerInputVerifier())

    val textField3 = JFormattedTextField()
    initBorderAndAlignment(textField3)
    textField3.setFormatterFactory(NumberFormatterFactory())

    val columnNames = arrayOf("Default", "DocumentFilter", "InputVerifier", "JFormattedTextField")
    val model = object : DefaultTableModel(columnNames, 10) {
      override fun getColumnClass(column: Int) = Integer::class.java
    }
    val table = object : JTable(model) {
      override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
        val c = super.prepareEditor(editor, row, column)
        (c as? JComponent)?.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
        return c
      }
    }
    table.getColumnModel().getColumn(1).setCellEditor(DefaultCellEditor(textField1))
    table.getColumnModel().getColumn(2).setCellEditor(object : DefaultCellEditor(textField2) {
      override fun stopCellEditing(): Boolean {
        val editor = getComponent() as? JComponent ?: return super.stopCellEditing()
        val isEditValid = editor.getInputVerifier().verify(editor)
        editor.setBorder(when {
          isEditValid -> BorderFactory.createEmptyBorder(1, 1, 1, 1)
          else -> BorderFactory.createLineBorder(Color.RED)
        })
        return isEditValid && super.stopCellEditing()
      }
    })
    table.getColumnModel().getColumn(3).setCellEditor(object : DefaultCellEditor(textField3) {
      override fun stopCellEditing(): Boolean {
        val editor = getComponent() as? JFormattedTextField ?: return super.stopCellEditing()
        val isEditValid = editor.isEditValid()
        editor.setBorder(when {
          isEditValid -> BorderFactory.createEmptyBorder(1, 1, 1, 1)
          else -> BorderFactory.createLineBorder(Color.RED)
        })
        return isEditValid && super.stopCellEditing()
      }
    })

    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }

  private fun initBorderAndAlignment(textField: JTextField) {
    textField.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
    textField.setHorizontalAlignment(SwingConstants.RIGHT)
  }
}

// Validating Text and Filtering Documents and Accessibility and the Java Access Bridge Tech Tips
// http://java.sun.com/developer/JDCTechTips/2005/tt0518.html
// Validating with Input Verifiers
internal class IntegerInputVerifier : InputVerifier() {
  override fun verify(c: JComponent): Boolean {
    var verified = false
    if (c is JTextComponent) {
      val txt = c.getText()
      if (txt.isEmpty()) {
        return true
      }
      val iv = runCatching { Integer.parseInt(txt) }
          .onFailure { UIManager.getLookAndFeel().provideErrorFeedback(c) }
          .getOrNull() ?: -1
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
internal class IntegerDocumentFilter : DocumentFilter() {
  @Throws(BadLocationException::class)
  override fun insertString(fb: DocumentFilter.FilterBypass, offset: Int, text: String?, attr: AttributeSet) {
    if (text != null) {
      replace(fb, offset, 0, text, attr)
    }
  }

  @Throws(BadLocationException::class)
  override fun remove(fb: DocumentFilter.FilterBypass, offset: Int, length: Int) {
    replace(fb, offset, length, "", null)
  }

  @Throws(BadLocationException::class)
  override fun replace(fb: DocumentFilter.FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet?) {
    val doc = fb.getDocument()
    val currentLength = doc.getLength()
    val currentContent = doc.getText(0, currentLength)
    val before = currentContent.substring(0, offset)
    val after = currentContent.substring(length + offset, currentLength)
    val newValue = before + Objects.toString(text, "") + after
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

// How to Use Formatted Text Fields (The Java™ Tutorials > Creating a GUI With JFC/Swing > Using Swing Components)
// https://docs.oracle.com/javase/tutorial/uiswing/components/formattedtextfield.html
class NumberFormatterFactory : DefaultFormatterFactory(numberFormatter, numberFormatter, numberFormatter) {
  companion object {
    private val numberFormatter = NumberFormatter()

    init {
      numberFormatter.setValueClass(Integer::class.java)
      (numberFormatter.getFormat() as? NumberFormat)?.setGroupingUsed(false)
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
