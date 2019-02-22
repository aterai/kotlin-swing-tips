package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(GridLayout(2, 1)) {
  init {
    val locale = Locale.ENGLISH // Locale.getDefault();
    val firstDayOfWeek = WeekFields.of(locale).getFirstDayOfWeek()
    val weeks = (0 until DayOfWeek.values().size).toList()
        .map { firstDayOfWeek.plus(it.toLong()) }
        .map { it.getDisplayName(TextStyle.SHORT_STANDALONE, locale) }

    val spinner01 = JSpinner()
    spinner01.setModel(SpinnerNumberModel(20, 0, 59, 1))

    val spinner02 = JSpinner()
    spinner02.setModel(SpinnerListModel(weeks))

    val spinner03 = JSpinner()
    spinner03.setModel(object : SpinnerNumberModel(20, 0, 59, 1) {
      override fun getNextValue() = super.getNextValue() ?: getMinimum()

      override fun getPreviousValue() = super.getPreviousValue() ?: getMaximum()
    })

    val spinner04 = JSpinner()
    spinner04.setModel(object : SpinnerListModel(weeks) {
      override fun getNextValue() = super.getNextValue() ?: getList().first()

      override fun getPreviousValue() = super.getPreviousValue() ?: getList().last()
    })
    add(makeTitledPanel("default model", spinner01, spinner02))
    add(makeTitledPanel("cycling model", spinner03, spinner04))
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, vararg list: Component): Component {
    val p = JPanel(GridBagLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    val c = GridBagConstraints()
    c.fill = GridBagConstraints.HORIZONTAL
    c.insets = Insets(5, 5, 5, 5)
    c.weightx = 1.0
    c.gridx = GridBagConstraints.REMAINDER
    list.forEach { p.add(it, c) }
    return p
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
