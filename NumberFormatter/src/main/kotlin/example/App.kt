package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.text.DecimalFormat
import java.text.ParseException
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.JSpinner.DefaultEditor
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultFormatter
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter

fun makeUI(): Component {
  val spinner = JSpinner(makeSpinnerNumberModel())
  (spinner.editor as? DefaultEditor)?.also {
    val formatter = it.textField.formatter
    if (formatter is DefaultFormatter) {
      formatter.allowsInvalid = false
    }
  }
  return JPanel(GridLayout(3, 1)).also {
    it.add(makeTitledPanel("Default", JSpinner(makeSpinnerNumberModel())))
    it.add(makeTitledPanel("NumberFormatter#setAllowsInvalid(false)", spinner))
    it.add(makeTitledPanel("BackgroundColor", WarningSpinner(makeSpinnerNumberModel())))
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSpinnerNumberModel() =
  SpinnerNumberModel(10.toLong(), 0.toLong(), 99_999.toLong(), 1.toLong())

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

private class WarningSpinner(model: SpinnerNumberModel) : JSpinner(model) {
  init {
    (editor as? DefaultEditor)?.also {
      val ftf = it.textField
      ftf.formatterFactory = makeFFactory(model)
      val dl = object : DocumentListener {
        private val errorBackground = Color(0xFF_C8_C8)
        override fun changedUpdate(e: DocumentEvent) {
          updateEditValid()
        }

        override fun insertUpdate(e: DocumentEvent) {
          updateEditValid()
        }

        override fun removeUpdate(e: DocumentEvent) {
          updateEditValid()
        }

        private fun updateEditValid() {
          EventQueue.invokeLater {
            ftf.background = if (ftf.isEditValid) Color.WHITE else errorBackground
          }
        }
      }
      ftf.document.addDocumentListener(dl)
    }
  }

  companion object {
    private fun makeFFactory(m: SpinnerNumberModel): DefaultFormatterFactory {
      val format = DecimalFormat("####0")
      val editFormatter = object : NumberFormatter(format) {
        @Throws(ParseException::class)
        override fun stringToValue(text: String): Any {
          runCatching {
            text.toLong()
          }
          val lv = format.parse(text)
          if (lv is Long) {
            if (lv !in m.minimum..m.maximum) {
              throw ParseException("out of bounds", 0)
            }
            return lv
          }
          throw ParseException("not Long", 0)
        }
      }
      editFormatter.valueClass = Long::class.java
      val displayFormatter = NumberFormatter(format)
      return DefaultFormatterFactory(displayFormatter, displayFormatter, editFormatter)
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
