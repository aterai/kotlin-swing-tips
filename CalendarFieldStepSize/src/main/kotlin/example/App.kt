package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DateFormatter
import javax.swing.text.DefaultFormatterFactory

class MainPanel : JPanel(GridLayout(2, 1)) {
  init {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.clear(Calendar.MINUTE)
    calendar.clear(Calendar.SECOND)
    calendar.clear(Calendar.MILLISECOND)
    val d = calendar.getTime()

    val format = SimpleDateFormat("mm:ss, SSS", Locale.getDefault())
    val factory = DefaultFormatterFactory(DateFormatter(format))
    val spinner1 = JSpinner(SpinnerDateModel(d, null, null, Calendar.SECOND))
    (spinner1.getEditor() as JSpinner.DefaultEditor).getTextField().setFormatterFactory(factory)

    val stepSizeMap = hashMapOf(
        Calendar.HOUR_OF_DAY to 1,
        Calendar.MINUTE to 1,
        Calendar.SECOND to 30,
        Calendar.MILLISECOND to 500)

    val spinner2 = JSpinner(object : SpinnerDateModel(d, null, null, Calendar.SECOND) {
      override fun getPreviousValue(): Any {
        val cal = Calendar.getInstance()
        cal.setTime(getDate())
        val calendarField = getCalendarField()
        val stepSize = stepSizeMap.get(calendarField) ?: 1
        cal.add(calendarField, -stepSize)
        return cal.getTime()
      }

      override fun getNextValue(): Any {
        val cal = Calendar.getInstance()
        cal.setTime(getDate())
        val calendarField = getCalendarField()
        val stepSize = stepSizeMap.get(calendarField) ?: 1
        cal.add(calendarField, stepSize)
        return cal.getTime()
      }
    })
    (spinner2.getEditor() as JSpinner.DefaultEditor).getTextField().setFormatterFactory(factory)

    add(makeTitledPanel("Default SpinnerDateModel", spinner1))
    add(makeTitledPanel("Override SpinnerDateModel#getNextValue(...)", spinner2))
    setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, cmp: Component) = JPanel(GridBagLayout()).also {
    it.setBorder(BorderFactory.createTitledBorder(title))
    val c = GridBagConstraints()
    c.weightx = 1.0
    c.fill = GridBagConstraints.HORIZONTAL
    c.insets = Insets(5, 5, 5, 5)
    it.add(cmp, c)
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
