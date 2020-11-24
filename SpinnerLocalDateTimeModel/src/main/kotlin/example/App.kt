package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.chrono.ChronoLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalUnit
import java.util.Calendar
import java.util.Date
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.JSpinner.DateEditor
import javax.swing.JSpinner.DefaultEditor
import javax.swing.text.DefaultFormatter
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.InternationalFormatter

fun makeUI(): Component {
  val cal = Calendar.getInstance()
  cal.clear(Calendar.MILLISECOND)
  cal.clear(Calendar.SECOND)
  cal.clear(Calendar.MINUTE)
  cal[Calendar.HOUR_OF_DAY] = 0
  val date = cal.time
  cal.add(Calendar.DATE, -2)
  val start = cal.time
  cal.add(Calendar.DATE, 9)
  val end = cal.time
  println(date)
  println(start)
  println(end)

  val dateFormat = "yyyy/MM/dd"
  val spinner0 = JSpinner(SpinnerDateModel(date, start, end, Calendar.DAY_OF_MONTH))
  spinner0.editor = DateEditor(spinner0, dateFormat)
  val d = LocalDateTime.now(ZoneId.systemDefault())
  val s = d.minus(2, ChronoUnit.DAYS)
  val e = d.plus(7, ChronoUnit.DAYS)
  println(d)
  println(s)
  println(e)
  val spinner1 = JSpinner(SpinnerDateModel(toDate(d), toDate(s), toDate(e), Calendar.DAY_OF_MONTH))
  spinner1.editor = DateEditor(spinner1, dateFormat)

  val spinner2 = JSpinner(SpinnerLocalDateTimeModel(d, s, e, ChronoUnit.DAYS))
  spinner2.editor = LocalDateTimeEditor(spinner2, dateFormat)

  return JPanel(GridLayout(0, 1)).also {
    it.add(makeTitledPanel("SpinnerDateModel", spinner0))
    it.add(makeTitledPanel("SpinnerDateModel / toInstant", spinner1))
    it.add(makeTitledPanel("SpinnerLocalDateTimeModel", spinner2))
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun toDate(localDateTime: LocalDateTime) =
  Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())

private fun makeTitledPanel(title: String, cmp: Component) = JPanel(GridBagLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  it.add(cmp, c)
}

private class SpinnerLocalDateTimeModel(
  value: ChronoLocalDateTime<*>?,
  start: Comparable<ChronoLocalDateTime<*>?>,
  end: Comparable<ChronoLocalDateTime<*>?>,
  temporalUnit: TemporalUnit
) : AbstractSpinnerModel() {
  @Transient
  var start: Comparable<ChronoLocalDateTime<*>>
    private set

  @Transient
  var end: Comparable<ChronoLocalDateTime<*>>
    private set

  @Transient
  var localDateTime: ChronoLocalDateTime<*>
    private set

  @Transient
  private var temporalUnit: TemporalUnit

  init {
    localDateTime = value ?: throw IllegalArgumentException("value is null")
    this.start = start
    this.end = end
    this.temporalUnit = temporalUnit
  }

//  fun setStart(start: Comparable<ChronoLocalDateTime<*>?>) {
//    if (start != this.start) {
//      this.start = start
//      fireStateChanged()
//    }
//  }
//
//  fun setEnd(end: Comparable<ChronoLocalDateTime<*>?>) {
//    if (end != this.end) {
//      this.end = end
//      fireStateChanged()
//    }
//  }
//
//  fun setTemporalUnit(temporalUnit: TemporalUnit) {
//    if (temporalUnit !== this.temporalUnit) {
//      this.temporalUnit = temporalUnit
//      fireStateChanged()
//    }
//  }
//
//  fun getTemporalUnit() = temporalUnit

  override fun getNextValue(): Any? {
    val next = localDateTime.plus(1, temporalUnit)
    return if (end >= next) next else null
  }

  override fun getPreviousValue(): Any? {
    val prev = localDateTime.minus(1, temporalUnit)
    return if (start <= prev) prev else null
  }

  override fun getValue() = localDateTime

  override fun setValue(value: Any) {
    require(value is ChronoLocalDateTime<*>) { "illegal value" }
    if (value != localDateTime) {
      localDateTime = value
      fireStateChanged()
    }
  }
}

private class LocalDateTimeEditor(spinner: JSpinner, dateFormatPattern: String?) : DefaultEditor(spinner) {
  @Transient
  val dateTimeFormatter: DateTimeFormatter

  val model: SpinnerLocalDateTimeModel

  inner class LocalDateTimeFormatter : InternationalFormatter(dateTimeFormatter.toFormat()) {
    override fun valueToString(value: Any): String =
      (value as? TemporalAccessor)?.let { dateTimeFormatter.format(it) } ?: ""

    @Throws(ParseException::class)
    override fun stringToValue(text: String): Any {
      val m = model
      return runCatching {
        val ta = dateTimeFormatter.parse(text)
        var value = m.localDateTime
        for (field in ChronoField.values()) {
          if (field.isSupportedBy(value) && ta.isSupported(field)) {
            value = field.adjustInto(value, ta.getLong(field))
          }
        }
        val min = m.start
        val max = m.end
        if (min > value || max < value) {
          throw ParseException("$text is out of range", 0)
        }
        value
      }
    }
  }

  init {
    require(spinner.model is SpinnerLocalDateTimeModel) { "model not a SpinnerLocalDateTimeModel" }
    dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormatPattern)
    model = spinner.model as SpinnerLocalDateTimeModel
    val formatter: DefaultFormatter = LocalDateTimeFormatter()
    EventQueue.invokeLater {
      formatter.valueClass = LocalDateTime::class.java
      val ftf = textField
      runCatching {
        val maxString = formatter.valueToString(model.start)
        val minString = formatter.valueToString(model.end)
        ftf.columns = maxString.length.coerceAtLeast(minString.length)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(ftf)
      }
      ftf.horizontalAlignment = SwingConstants.LEFT
      ftf.isEditable = true
      ftf.formatterFactory = DefaultFormatterFactory(formatter)
    }
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
