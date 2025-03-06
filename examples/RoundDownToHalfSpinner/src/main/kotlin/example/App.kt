package example

import java.awt.*
import java.math.BigDecimal
import java.math.RoundingMode
import javax.swing.*
import javax.swing.JSpinner.DefaultEditor
import javax.swing.text.DefaultFormatter
import javax.swing.text.DefaultFormatterFactory

private val textArea = JTextArea()

fun makeUI(): Component {
  val spinner0 = JSpinner(SpinnerNumberModel(8.85, 8.0, 72.0, .5))
  val spinner1 = JSpinner(RoundToHalfSpinnerModel(8.85, 8.0, 72.0, .5))
  val spinner2 = makeSpinner(makeDownFormatter())
  val spinner3 = makeSpinner(makeRoundingFormatter())
  val p = JPanel(GridLayout(0, 2))
  p.add(makeTitledPanel("Default, stepSize: 0.5", spinner0))
  p.add(makeTitledPanel("Override SpinnerNumberModel", spinner1))
  p.add(makeTitledPanel("Round down to half Formatter", spinner2))
  p.add(makeTitledPanel("Rounding to half Formatter", spinner3))
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSpinner(formatter: DefaultFormatter): JSpinner {
  val model = SpinnerNumberModel(8.85, 8.0, 72.0, .5)
  val spinner = JSpinner(model)
  (spinner.editor as? DefaultEditor)?.also {
    it.textField.formatterFactory = DefaultFormatterFactory(formatter)
    info(formatter, model)
  }
  return spinner
}

private fun info(formatter: DefaultFormatter, model: SpinnerNumberModel) {
  runCatching {
    val v1 = model.number.toString()
    val v2 = formatter.stringToValue(v1)
    textArea.append("%s -> %s%n".format(v1, v2))
  }.onFailure {
    textArea.append(it.message + "\n")
  }
}

private fun makeDownFormatter() = object : DefaultFormatter() {
  override fun stringToValue(text: String) =
    roundToDown(BigDecimal(text))
      .toDouble()

  override fun valueToString(value: Any) =
    roundToDown(BigDecimal.valueOf(value as? Double ?: 0.0))
      .toString()
}

private fun roundToDown(value: BigDecimal) =
  value
    .multiply(BigDecimal.valueOf(2))
    .setScale(0, RoundingMode.DOWN)
    .multiply(BigDecimal.valueOf(.5))

private fun makeRoundingFormatter() = object : DefaultFormatter() {
  override fun stringToValue(text: String) =
    roundingTo(BigDecimal(text))
      .toDouble()

  override fun valueToString(value: Any) =
    roundingTo(BigDecimal.valueOf(value as? Double ?: 0.0))
      .toString()
}

private fun roundingTo(value: BigDecimal) =
  value
    .multiply(BigDecimal.valueOf(2))
    .setScale(0, RoundingMode.HALF_UP)
    .multiply(BigDecimal.valueOf(.5))

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

private class RoundToHalfSpinnerModel(
  value: Double,
  min: Double,
  max: Double,
  step: Double,
) : SpinnerNumberModel(roundDownToHalf(value), min, max, step) {
  override fun setValue(value: Any) {
    require(value is Double) { "illegal value" }
    val v = roundDownToHalf(value)
    if (v != getValue()) {
      super.setValue(v)
      fireStateChanged()
    }
  }

  companion object {
    private fun roundDownToHalf(value: Double) = BigDecimal
      .valueOf(value)
      .multiply(BigDecimal.valueOf(2))
      .setScale(0, RoundingMode.DOWN)
      .multiply(BigDecimal.valueOf(.5))
      .toDouble()
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
