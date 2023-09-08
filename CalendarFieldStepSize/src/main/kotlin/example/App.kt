package example

import java.awt.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.swing.*
import javax.swing.text.DateFormatter
import javax.swing.text.DefaultFormatterFactory

fun makeUI(): Component {
  val calendar = Calendar.getInstance()
  calendar.set(Calendar.HOUR_OF_DAY, 0)
  calendar.clear(Calendar.MINUTE)
  calendar.clear(Calendar.SECOND)
  calendar.clear(Calendar.MILLISECOND)
  val d = calendar.time

  val format = SimpleDateFormat("mm:ss, SSS", Locale.getDefault())
  val factory = DefaultFormatterFactory(DateFormatter(format))
  val spinner1 = JSpinner(SpinnerDateModel(d, null, null, Calendar.SECOND))
  (spinner1.editor as? JSpinner.DefaultEditor)?.textField?.formatterFactory = factory

  val stepSizeMap = hashMapOf(
    Calendar.HOUR_OF_DAY to 1,
    Calendar.MINUTE to 1,
    Calendar.SECOND to 30,
    Calendar.MILLISECOND to 500,
  )

  val m2 = object : SpinnerDateModel(d, null, null, Calendar.SECOND) {
    override fun getPreviousValue(): Any {
      val cal = Calendar.getInstance()
      cal.time = date
      val calendarField = calendarField
      val stepSize = stepSizeMap[calendarField] ?: 1
      cal.add(calendarField, -stepSize)
      return cal.time
    }

    override fun getNextValue(): Any {
      val cal = Calendar.getInstance()
      cal.time = date
      val calendarField = calendarField
      val stepSize = stepSizeMap[calendarField] ?: 1
      cal.add(calendarField, stepSize)
      return cal.time
    }
  }
  val spinner2 = JSpinner(m2)
  (spinner2.editor as? JSpinner.DefaultEditor)?.textField?.formatterFactory = factory

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("Default SpinnerDateModel", spinner1))
    it.add(makeTitledPanel("Override SpinnerDateModel#getNextValue(...)", spinner2))
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, cmp: Component) = JPanel(GridBagLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  it.add(cmp, c)
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
