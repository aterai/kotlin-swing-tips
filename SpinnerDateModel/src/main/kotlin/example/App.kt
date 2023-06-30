package example

import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.util.Calendar
import java.util.Date
import javax.swing.*
import javax.swing.JSpinner.DateEditor

fun makeUI(): Component {
  val dateFormat = "yyyy/MM/dd"
  val date = Date()
  val spinner1 = JSpinner(SpinnerDateModel(date, date, null, Calendar.DAY_OF_MONTH))
  spinner1.editor = DateEditor(spinner1, dateFormat)

  val today = Calendar.getInstance()
  today.clear(Calendar.MILLISECOND)
  today.clear(Calendar.SECOND)
  today.clear(Calendar.MINUTE)
  today[Calendar.HOUR_OF_DAY] = 0

  val start = today.time
  val log = JTextArea()
  log.append("$date\n")
  log.append("$start\n")

  val spinner2 = JSpinner(SpinnerDateModel(date, start, null, Calendar.DAY_OF_MONTH))
  spinner2.editor = DateEditor(spinner2, dateFormat)

  val spinner3 = JSpinner(SpinnerDateModel(date, start, null, Calendar.DAY_OF_MONTH))
  val editor = DateEditor(spinner3, dateFormat)
  spinner3.editor = editor
  val fl3 = object : FocusAdapter() {
    override fun focusGained(e: FocusEvent) {
      EventQueue.invokeLater {
        val i = dateFormat.lastIndexOf("dd")
        editor.textField.select(i, i + 2)
      }
    }
  }
  editor.textField.addFocusListener(fl3)

  val p = JPanel(GridLayout(3, 1)).also {
    it.add(makeTitledPanel("Calendar.DAY_OF_MONTH", spinner1))
    it.add(makeTitledPanel("min: set(Calendar.HOUR_OF_DAY, 0)", spinner2))
    it.add(makeTitledPanel("JSpinner.DateEditor + FocusListener", spinner3))
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
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
