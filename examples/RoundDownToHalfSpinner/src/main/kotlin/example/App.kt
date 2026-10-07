package example

import java.awt.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.ParseException
import javax.swing.*
import javax.swing.JSpinner.DefaultEditor
import javax.swing.text.DefaultFormatter
import javax.swing.text.DefaultFormatterFactory

private const val INITIAL_VALUE = 8.85
private const val MIN_VALUE = 8.0
private const val MAX_VALUE = 72.0
private const val STEP_SIZE = 0.5

private val textArea = JTextArea()

fun createUI(): Component {
  val defaultSpinner = createSpinner(
    SpinnerNumberModel(INITIAL_VALUE, MIN_VALUE, MAX_VALUE, STEP_SIZE),
    null,
  )
  val downModelSpinner = createSpinner(
    RoundDownToHalfSpinnerModel(INITIAL_VALUE, MIN_VALUE, MAX_VALUE, STEP_SIZE),
    null,
  )
  val downFormatSpinner = createSpinner(
    SpinnerNumberModel(INITIAL_VALUE, MIN_VALUE, MAX_VALUE, STEP_SIZE),
    HalfFormatter(RoundingMode.DOWN, MIN_VALUE, MAX_VALUE),
  )
  val halfUpSpinner = createSpinner(
    SpinnerNumberModel(INITIAL_VALUE, MIN_VALUE, MAX_VALUE, STEP_SIZE),
    HalfFormatter(RoundingMode.HALF_UP, MIN_VALUE, MAX_VALUE),
  )

  val p = JPanel(GridLayout(0, 2, 5, 5))
  p.add(createTitledPanel("Default, stepSize: 0.5", defaultSpinner))
  p.add(createTitledPanel("Override SpinnerNumberModel", downModelSpinner))
  p.add(createTitledPanel("Round down to half Formatter", downFormatSpinner))
  p.add(createTitledPanel("Round to half Formatter", halfUpSpinner))

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createSpinner(
  model: SpinnerNumberModel,
  formatter: DefaultFormatter?,
): JSpinner {
  val spinner = JSpinner(model)
  val editor = spinner.editor
  if (formatter != null && editor is DefaultEditor) {
    editor.textField.setFormatterFactory(DefaultFormatterFactory(formatter))
    appendRoundedValue(formatter, model)
  }
  return spinner
}

private fun appendRoundedValue(formatter: DefaultFormatter, model: SpinnerNumberModel) {
  runCatching {
    val valueText = model.number.toString()
    val roundedValue = formatter.stringToValue(valueText)
    textArea.append("%s -> %s%n".format(valueText, roundedValue))
  }.onFailure {
    textArea.append(it.message + "\n")
  }
}

// Round to a multiple of 0.5: double the value, round it to an integer
// with the given RoundingMode, and then halve it.
private fun roundToHalf(value: BigDecimal, roundingMode: RoundingMode?) = value
  .multiply(BigDecimal.valueOf(2))
  .setScale(0, roundingMode)
  .multiply(BigDecimal.valueOf(0.5))

private fun createTitledPanel(title: String, cmp: Component): Component {
  val panel = JPanel(GridBagLayout())
  panel.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  panel.add(cmp, c)
  return panel
}

private class HalfFormatter(
  private val roundingMode: RoundingMode?,
  private val minimum: Double,
  private val maximum: Double,
) : DefaultFormatter() {
  init {
    // DefaultFormatter overwrites typed characters by default, unlike NumberFormatter
    overwriteMode = false
  }

  @Throws(ParseException::class)
  override fun stringToValue(text: String): Any {
    val value = runCatching {
      BigDecimal(text.trim())
    }.getOrElse {
      // DefaultFormatter must report invalid text as a ParseException
      // so that JFormattedTextField can revert the edit
      throw ParseException("Invalid number: $text", 0).also { pe -> pe.initCause(it) }
    }
    val rounded = roundToHalf(value, roundingMode).toDouble()
    // This formatter replaces the NumberEditor's one, which checks the bounds of the model
    if (rounded < minimum || rounded > maximum) {
      throw ParseException("Out of range: $text", 0)
    }
    return rounded
  }

  @Throws(ParseException::class)
  override fun valueToString(value: Any?): String {
    if (value !is Number) {
      throw ParseException("value is not a Number: $value", 0)
    }
    val doubleValue = value.toDouble()
    return roundToHalf(BigDecimal.valueOf(doubleValue), roundingMode).toString()
  }
}

private class RoundDownToHalfSpinnerModel(
  value: Double,
  min: Double,
  max: Double,
  step: Double,
) : SpinnerNumberModel(roundDownToHalf(value), min, max, step) {
  override fun setValue(value: Any) {
    val roundedValue = roundDownToHalf(requireNumber(value).toDouble())
    if (roundedValue == getValue()) {
      if (roundedValue != value) {
        // The value is unchanged, but the editor still displays the unrounded text
        // (e.g. 8.85 when the value is 8.5), so notify it to redisplay the current value
        fireStateChanged()
      }
    } else {
      // SpinnerNumberModel#setValue(...) fires a ChangeEvent by itself
      super.setValue(roundedValue)
    }
  }

  companion object {
    private fun requireNumber(value: Any): Number {
      if (value is Number) {
        return value
      }
      throw IllegalArgumentException("Value must be a Number: $value")
    }

    private fun roundDownToHalf(value: Double) = roundToHalf(
      BigDecimal.valueOf(value),
      RoundingMode.DOWN,
    ).toDouble()
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
