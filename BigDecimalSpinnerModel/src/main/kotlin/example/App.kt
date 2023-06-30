package example

import java.awt.*
import java.math.BigDecimal
import javax.swing.*
import kotlin.math.abs

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(JLabel("SpinnerNumberModel(double, ...)"))
  box.add(Box.createVerticalStrut(2))

  val p1 = JPanel(GridLayout(1, 2, 5, 5)).also {
    it.add(JSpinner(SpinnerNumberModel(2.01, 2.00, 3.02, .01)))
    it.add(JSpinner(SpinnerNumberModel(29.7, 29.6, 30.2, .1)))
  }
  box.add(p1)
  box.add(Box.createVerticalStrut(5))

  box.add(JLabel("BigDecimalSpinnerModel"))
  box.add(Box.createVerticalStrut(2))
  val p2 = JPanel(GridLayout(1, 2, 5, 5)).also {
    it.add(JSpinner(BigDecimalSpinnerModel(2.01, 2.00, 3.02, .01)))
    it.add(JSpinner(BigDecimalSpinnerModel(29.7, 29.6, 30.2, .1)))
  }
  box.add(p2)

  // TEST:
  val d = 29.7 - 29.6 - .1
  val str1 = "%f-%f-%f>=0:%b%n".format(29.7, 29.6, .1, d >= .0)
  val str2 = "abs(%f-%f-%f)<1.0e-14:%b%n".format(29.7, 29.6, .1, abs(d) < 1.0e-14)
  val str3 = "abs(%f-%f-%f)<1.0e-15:%b%n".format(29.7, 29.6, .1, abs(d) < 1.0e-15)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea(str1 + str2 + str3)))
    it.preferredSize = Dimension(320, 240)
  }
}

private class BigDecimalSpinnerModel(
  value: Double,
  minimum: Double,
  maximum: Double,
  stepSize: Double
) : SpinnerNumberModel(value, minimum, maximum, stepSize) {
  override fun getPreviousValue() = incrValue2(-1)

  override fun getNextValue() = incrValue2(+1)

  private fun incrValue2(dir: Int): Number? {
    val value = BigDecimal.valueOf(number.toDouble())
    val stepSize = BigDecimal.valueOf(stepSize.toDouble())
    val newValue = if (dir > 0) value.add(stepSize) else value.subtract(stepSize)
    val maximum = BigDecimal.valueOf(maximum as? Double ?: 0.0)
    if (maximum < newValue) {
      return null
    }
    val minimum = BigDecimal.valueOf(minimum as? Double ?: 0.0)
    return if (minimum > newValue) null else newValue
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
