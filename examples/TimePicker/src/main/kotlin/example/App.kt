package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.JTextComponent
import javax.swing.text.MaskFormatter

fun makeUI(): Component {
  val c1 = TimePickerSingleField().createPickerPanel()
  val c2 = TimePickerSplitField().createPickerPanel()
  return JPanel().also {
    it.add(c1)
    it.add(c2)
    it.setBorder(BorderFactory.createEmptyBorder(20, 2, 20, 2))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TimePickerSplitField {
  fun createPickerPanel(): JPanel {
    val fieldHour = makeNumberField(12, 1, 0, 23)
    val fieldMinute = makeNumberField(30, 1, 0, 59)

    val pnlUp = JPanel(GridLayout(1, 2))
    pnlUp.add(makeCenteredBox(makeArrowButton(fieldHour, 1, 0, 23)))
    pnlUp.add(makeCenteredBox(makeArrowButton(fieldMinute, 1, 0, 59)))

    val pnlDown = JPanel(GridLayout(1, 2))
    pnlDown.add(makeCenteredBox(makeArrowButton(fieldHour, -1, 0, 23)))
    pnlDown.add(makeCenteredBox(makeArrowButton(fieldMinute, -1, 0, 59)))

    val panel = JPanel(BorderLayout(5, 5))
    panel.setOpaque(false)
    panel.add(pnlUp, BorderLayout.NORTH)
    panel.add(makeTimeFieldPanel(fieldHour, fieldMinute))
    panel.add(pnlDown, BorderLayout.SOUTH)
    return panel
  }

  fun makeArrowButton(field: JTextField, delta: Int, min: Int, max: Int): JButton {
    val txt = if (delta > 0) "⏶" else "⏷"
    val button = JButton(txt)
    button.setFocusable(false)
    val handler = AutoRepeatHandler(field, delta, min, max)
    button.addActionListener(handler)
    button.addMouseListener(handler)
    return button
  }

  private fun makeCenteredBox(button: JButton?): Box {
    val box = Box.createHorizontalBox()
    box.add(Box.createHorizontalGlue())
    box.add(button)
    box.add(Box.createHorizontalGlue())
    return box
  }

  private fun makeTimeFieldPanel(hour: JTextField?, minute: JTextField?): JPanel {
    val panel: JPanel = RoundPanel(8)
    panel.setLayout(BoxLayout(panel, BoxLayout.X_AXIS))
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8))
    panel.setOpaque(false)
    panel.setBackground(Color(0xDEDEDE))
    panel.add(Box.createHorizontalGlue())
    panel.add(hour)
    val colon = JLabel(":")
    colon.setFont(colon.getFont().deriveFont(Font.BOLD, 42f))
    colon.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 5))
    panel.add(colon)
    panel.add(minute)
    panel.add(Box.createHorizontalGlue())
    return panel
  }

  fun makeNumberField(
    value: Int,
    step: Int,
    min: Int,
    max: Int,
  ): JFormattedTextField {
    val field = RoundFormattedTextField(value, step, min, max)
    runCatching {
      val mask = MaskFormatter("##")
      mask.placeholderCharacter = '0'
      field.setFormatterFactory(DefaultFormatterFactory(mask))
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(field)
    }
    field.setFont(field.getFont().deriveFont(Font.BOLD, 42f))
    field.setHorizontalAlignment(JTextField.CENTER)
    field.setColumns(2)
    return field
  }
}

private class RoundPanel(
  private val radius: Int,
) : JPanel() {
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = getBackground()
    val dw = getWidth().toDouble()
    val dh = getHeight().toDouble()
    val da = radius.toDouble()
    g2.fill(RoundRectangle2D.Double(0.0, 0.0, dw, dh, da, da))
    g2.color = getBackground().darker()
    g2.draw(RoundRectangle2D.Double(0.0, 0.0, dw - 1, dh - 1, da, da))
    g2.dispose()
    super.paintComponent(g)
  }
}

private class RoundFormattedTextField(
  value: Int,
  step: Int,
  min: Int,
  max: Int,
) : JFormattedTextField("%02d".format(value)) {
  private var listener: FocusListener? = null

  init {
    // setText(String.format("%02d", value));
    addMouseWheelListener { e ->
      val delta = if (e.getWheelRotation() < 0) 1 else -1
      val c = e.component
      if (c is JTextComponent) {
        AutoRepeatHandler.adjust(c, delta * step, min, max)
      }
    }
  }

  override fun updateUI() {
    removeFocusListener(listener)
    super.updateUI()
    setFocusable(true)
    setOpaque(false)
    setBackground(Color(0xCE_CE_CE))
    setSelectionColor(Color(0x0, true))
    setSelectedTextColor(getForeground())
    setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0))
    setFont(getFont().deriveFont(Font.BOLD, 42f))
    setCaret(object : DefaultCaret() {
      override fun isVisible() = false
    })
    setCursor(Cursor(Cursor.DEFAULT_CURSOR))
    listener = object : FocusListener {
      override fun focusGained(e: FocusEvent) {
        val c = e.component
        c.setForeground(UIManager.getColor("TextField.foreground"))
      }

      override fun focusLost(e: FocusEvent) {
        e.component.setForeground(Color.DARK_GRAY)
      }
    }
    addFocusListener(listener)
  }

  override fun paintComponent(g: Graphics) {
    if (hasFocus()) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.color = getBackground()
      val dw = getWidth().toDouble()
      val dh = getHeight().toDouble()
      g2.fill(RoundRectangle2D.Double(0.0, 0.0, dw, dh, 8.0, 8.0))
      g2.color = getBackground().darker()
      g2.draw(RoundRectangle2D.Double(0.0, 0.0, dw - 1, dh - 1, 8.0, 8.0))
      g2.dispose()
    }
    super.paintComponent(g)
  }
}

private class AutoRepeatHandler(
  private val view: JTextComponent,
  private val delta: Int,
  private val min: Int,
  private val max: Int,
) : MouseAdapter(),
  ActionListener {
  private val autoRepeatTimer = Timer(60, this)
  private var arrowButton: JButton? = null

  init {
    autoRepeatTimer.setInitialDelay(300)
  }

  override fun actionPerformed(e: ActionEvent) {
    val o = e.getSource()
    if (o is Timer) {
      val released = arrowButton?.getModel()?.isPressed != true
      if (released && autoRepeatTimer.isRunning) {
        autoRepeatTimer.stop()
      }
    } else if (o is JButton) {
      arrowButton = o
    }
    adjust(view, delta, min, max)
  }

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e) && e.component.isEnabled) {
      autoRepeatTimer.start()
    }
  }

  override fun mouseReleased(e: MouseEvent?) {
    autoRepeatTimer.stop()
  }

  override fun mouseExited(e: MouseEvent?) {
    if (autoRepeatTimer.isRunning) {
      autoRepeatTimer.stop()
    }
  }

  companion object {
    fun adjust(field: JTextComponent, delta: Int, min: Int, max: Int) {
      field.requestFocusInWindow()
      val range = max - min + 1
      var value = field.getText().toInt()
      value = (value - min + delta) % range
      if (value < 0) {
        value += range
      }
      value += min
      field.text = "%02d".format(value)
    }
  }
}

private class TimePickerSingleField {
  private var timeField: JFormattedTextField? = null
  private var currentTime = LocalTime.of(12, 30)
  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

  fun createPickerPanel(): Component {
    val field = runCatching {
      val mask = MaskFormatter("##:##").also {
        it.placeholderCharacter = '0'
      }
      JFormattedTextField(mask)
    }.getOrNull() ?: JFormattedTextField()
    field.setFont(Font("Monospaced", Font.BOLD, 42))
    field.setHorizontalAlignment(JTextField.CENTER)
    field.isEditable = false
    field.setFocusable(true)
    updateDisplay()
    field.addMouseWheelListener { e ->
      val isUp = e.getWheelRotation() < 0
      val isHourSide = field.viewToModel(e.getPoint()) <= 2
      adjustTime(isHourSide, isUp)
    }
    timeField = field
    return field
  }

  private fun adjustTime(isHour: Boolean, isUp: Boolean) {
    currentTime = if (isHour) {
      if (isUp) currentTime.plusHours(1) else currentTime.minusHours(1)
    } else {
      if (isUp) currentTime.plusMinutes(1) else currentTime.minusMinutes(1)
    }
    updateDisplay()
  }

  private fun updateDisplay() {
    timeField?.text = currentTime.format(timeFormatter)
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
      minimumSize = Dimension(256, 200)
      isResizable = false
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
