package example

import java.awt.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.swing.*
import javax.swing.JSpinner.DefaultEditor
import javax.swing.text.DateFormatter
import javax.swing.text.DefaultFormatterFactory

fun makeUI(): Component {
  val c = Calendar.getInstance()
  c[Calendar.HOUR_OF_DAY] = 0
  c.clear(Calendar.MINUTE)
  c.clear(Calendar.SECOND)
  c.clear(Calendar.MILLISECOND)

  val d = c.time
  val format = SimpleDateFormat("mm:ss", Locale.getDefault())
  val factory = DefaultFormatterFactory(DateFormatter(format))
  val spinner1 = JSpinner(SpinnerDateModel(d, null, null, Calendar.SECOND))
  (spinner1.editor as? DefaultEditor)?.textField?.formatterFactory = factory

  val spinner2 = JSpinner(object : SpinnerDateModel(d, null, null, Calendar.SECOND) {
    override fun setCalendarField(calendarField: Int) {
      // https://docs.oracle.com/javase/8/docs/api/javax/swing/SpinnerDateModel.html#setCalendarField-int-
      // If you only want one field to spin you can subclass and ignore the setCalendarField calls.
    }
  })
  (spinner2.editor as? DefaultEditor)?.textField?.formatterFactory = factory

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("Default SpinnerDateModel", spinner1))
    it.add(makeTitledPanel("Override SpinnerDateModel#setCalendarField(...)", spinner2))
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

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
