package example

import java.awt.*
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.*

fun makeUI(): Component {
  val locale = Locale.ENGLISH // Locale.getDefault()
  val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
  val weeks = DayOfWeek.entries
    .toTypedArray()
    .indices
    .map { firstDayOfWeek.plus(it.toLong()) }
    .map { it.getDisplayName(TextStyle.SHORT_STANDALONE, locale) }

  val spinner01 = JSpinner()
  spinner01.model = SpinnerNumberModel(20, 0, 59, 1)

  val spinner02 = JSpinner()
  spinner02.model = SpinnerListModel(weeks)

  val spinner03 = JSpinner()
  spinner03.model = object : SpinnerNumberModel(20, 0, 59, 1) {
    override fun getNextValue() = super.getNextValue() ?: minimum

    override fun getPreviousValue() = super.getPreviousValue() ?: maximum
  }

  val spinner04 = JSpinner()
  spinner04.model = object : SpinnerListModel(weeks) {
    override fun getNextValue() = super.getNextValue() ?: list.first()

    override fun getPreviousValue() = super.getPreviousValue() ?: list.last()
  }

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("default model", spinner01, spinner02))
    it.add(makeTitledPanel("cycling model", spinner03, spinner04))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  vararg list: Component,
): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  c.weightx = 1.0
  c.gridx = GridBagConstraints.REMAINDER
  list.forEach { p.add(it, c) }
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
