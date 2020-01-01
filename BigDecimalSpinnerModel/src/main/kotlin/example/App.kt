package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.math.BigDecimal
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.abs

class MainPanel : JPanel(BorderLayout()) {
  init {
    val box = Box.createVerticalBox()
    box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    box.add(JLabel("SpinnerNumberModel(double, ...)"))
    box.add(Box.createVerticalStrut(2))
    box.add(JPanel(GridLayout(1, 2, 5, 5)).also {
      it.add(JSpinner(SpinnerNumberModel(2.01, 2.00, 3.02, .01)))
      it.add(JSpinner(SpinnerNumberModel(29.7, 29.6, 30.2, .1)))
    })
    box.add(Box.createVerticalStrut(5))
    box.add(JLabel("BigDecimalSpinnerModel"))
    box.add(Box.createVerticalStrut(2))
    box.add(JPanel(GridLayout(1, 2, 5, 5)).also {
      it.add(JSpinner(BigDecimalSpinnerModel(2.01, 2.00, 3.02, .01)))
      it.add(JSpinner(BigDecimalSpinnerModel(29.7, 29.6, 30.2, .1)))
    })

    // TEST:
    val d = 29.7 - 29.6 - .1
    val str1 = String.format("%f-%f-%f>=0:%b%n", 29.7, 29.6, .1, d >= .0)
    val str2 = String.format("abs(%f-%f-%f)<1.0e-14:%b%n", 29.7, 29.6, .1, abs(d) < 1.0e-14)
    val str3 = String.format("abs(%f-%f-%f)<1.0e-15:%b%n", 29.7, 29.6, .1, abs(d) < 1.0e-15)
    add(box, BorderLayout.NORTH)
    add(JScrollPane(JTextArea(str1 + str2 + str3)))
    preferredSize = Dimension(320, 240)
  }
}

class BigDecimalSpinnerModel(
  value: Double,
  minimum: Double,
  maximum: Double,
  stepSize: Double
) : SpinnerNumberModel(value, minimum, maximum, stepSize) {
  override fun getPreviousValue(): Any? {
    return incrValue2(-1)
  }

  override fun getNextValue(): Any? {
    return incrValue2(+1)
  }

  private fun incrValue2(dir: Int): Number? {
    val value = BigDecimal.valueOf(number as Double)
    val stepSize = BigDecimal.valueOf(stepSize as Double)
    val newValue = if (dir > 0) value.add(stepSize) else value.subtract(stepSize)
    val maximum = BigDecimal.valueOf(maximum as Double)
    if (maximum < newValue) {
      return null
    }
    val minimum = BigDecimal.valueOf(minimum as Double)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
