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
    RoundToHalfSpinnerModel(INITIAL_VALUE, MIN_VALUE, MAX_VALUE, STEP_SIZE),
    null,
  )
  val downFmtSpinner = createSpinner(
    SpinnerNumberModel(INITIAL_VALUE, MIN_VALUE, MAX_VALUE, STEP_SIZE),
    createHalfFormatter(RoundingMode.DOWN),
  )
  val halfUpFmtSpinner = createSpinner(
    SpinnerNumberModel(INITIAL_VALUE, MIN_VALUE, MAX_VALUE, STEP_SIZE),
    createHalfFormatter(RoundingMode.HALF_UP),
  )

  val p = JPanel(GridLayout(0, 2, 5, 5))
  p.add(createTitledPanel("Default, stepSize: 0.5", defaultSpinner))
  p.add(createTitledPanel("Override SpinnerNumberModel", downModelSpinner))
  p.add(createTitledPanel("Round down to half Formatter", downFmtSpinner))
  p.add(createTitledPanel("Round to half Formatter", halfUpFmtSpinner))

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
    info(formatter, model)
  }
  return spinner
}

private fun info(formatter: DefaultFormatter, model: SpinnerNumberModel) {
  runCatching {
    val valueText = model.number.toString()
    val roundedValue = formatter.stringToValue(valueText)
    textArea.append("%s -> %s%n".format(valueText, roundedValue))
  }.onFailure {
    textArea.append(it.message + "\n")
  }
}

private fun createHalfFormatter(roundingMode: RoundingMode?): DefaultFormatter {
  return object : DefaultFormatter() {
    override fun stringToValue(
      text: String,
    ): Any = roundToHalf(BigDecimal(text), roundingMode).toDouble()

    @Throws(ParseException::class)
    override fun valueToString(value: Any?): String {
      if (value !is Number) {
        throw ParseException("value is not a Number: $value", 0)
      }
      val doubleValue = value.toDouble()
      return roundToHalf(BigDecimal.valueOf(doubleValue), roundingMode).toString()
    }
  }
}

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

private class RoundToHalfSpinnerModel(
  value: Double,
  min: Double,
  max: Double,
  step: Double,
) : SpinnerNumberModel(roundDownToHalf(value), min, max, step) {
  override fun setValue(value: Any) {
    val number = requireNumber(value)
    val roundedValue = roundDownToHalf(number.toDouble())
    if (roundedValue != getValue()) {
      super.setValue(roundedValue)
      fireStateChanged()
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

    fun roundToHalf(
      value: BigDecimal,
      roundingMode: RoundingMode?,
    ): BigDecimal = value
      .multiply(BigDecimal.valueOf(2))
      .setScale(0, roundingMode)
      .multiply(BigDecimal.valueOf(0.5))
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
