package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.geom.RoundRectangle2D
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.MaskFormatter

fun createUI(): Component {
  val c1 = TimePickerSingleField().createComponent()
  val c2 = TimePickerSplitField().createComponent()
  return JPanel().also {
    it.add(c1)
    it.add(c2)
    it.setBorder(BorderFactory.createEmptyBorder(20, 2, 20, 2))
    it.preferredSize = Dimension(320, 240)
  }
}

// A time picker made of two separate hour/minute fields,
// each with its own up/down spinner buttons.
private class TimePickerSplitField {
  fun createComponent(): JPanel {
    val hourField = makeNumberField(12, 1, 0, 23)
    val minuteField = makeNumberField(30, 1, 0, 59)

    val upButtonPanel = JPanel(GridLayout(1, 2))
    upButtonPanel.add(makeCenteredBox(makeArrowButton(hourField, 1)))
    upButtonPanel.add(makeCenteredBox(makeArrowButton(minuteField, 1)))

    val downButtonPanel = JPanel(GridLayout(1, 2))
    downButtonPanel.add(makeCenteredBox(makeArrowButton(hourField, -1)))
    downButtonPanel.add(makeCenteredBox(makeArrowButton(minuteField, -1)))

    val panel = JPanel(BorderLayout(5, 5))
    panel.setOpaque(false)
    panel.add(upButtonPanel, BorderLayout.NORTH)
    panel.add(makeTimeFieldPanel(hourField, minuteField))
    panel.add(downButtonPanel, BorderLayout.SOUTH)
    return panel
  }

  // Creates an up/down button that moves the field by one step,
  // repeating while the button is held down.
  private fun makeArrowButton(
    field: RoundFormattedTextField,
    direction: Int,
  ): JButton {
    val arrowLabel = if (direction > 0) "⏶" else "⏷"
    val button = JButton(arrowLabel)
    button.setFocusable(false)
    val handler = AutoRepeatHandler { field.adjustValue(direction) }
    button.addActionListener(handler)
    button.addMouseListener(handler)
    return button
  }

  private fun makeCenteredBox(button: JButton): Box {
    val box = Box.createHorizontalBox()
    box.add(Box.createHorizontalGlue())
    box.add(button)
    box.add(Box.createHorizontalGlue())
    return box
  }

  private fun makeTimeFieldPanel(
    hourField: JTextField,
    minuteField: JTextField,
  ): JPanel {
    val panel: JPanel = RoundPanel(8)
    panel.setLayout(BoxLayout(panel, BoxLayout.X_AXIS))
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8))
    panel.setOpaque(false)
    panel.setBackground(PANEL_BACKGROUND)
    panel.add(Box.createHorizontalGlue())
    panel.add(hourField)
    val colon = JLabel(":")
    colon.setFont(colon.getFont().deriveFont(Font.BOLD, 42f))
    colon.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 5))
    panel.add(colon)
    panel.add(minuteField)
    panel.add(Box.createHorizontalGlue())
    return panel
  }

  private fun makeNumberField(
    value: Int,
    step: Int,
    min: Int,
    max: Int,
  ): RoundFormattedTextField {
    val field = RoundFormattedTextField(value, step, min, max)
    runCatching {
      // "##" restricts input to exactly two digits (e.g. "07", "23").
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

  companion object {
    // Background color of the rounded panel that wraps the hour/minute fields.
    private val PANEL_BACKGROUND = Color(0xDE_DE_DE)
  }
}

// A JPanel that paints itself as a filled rounded rectangle using its background color.
private class RoundPanel(
  private val arc: Int,
) : JPanel() {
  override fun paintComponent(g: Graphics) {
    paintRoundRect(g, this, arc)
    super.paintComponent(g)
  }

  companion object {
    // Fills the component bounds with its background color
    // and outlines it with a darker shade of the same color.
    fun paintRoundRect(
      g: Graphics,
      c: Component,
      arc: Int,
    ) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      val w = c.width.toDouble()
      val h = c.height.toDouble()
      val a = arc.toDouble()
      g2.color = c.background
      g2.fill(RoundRectangle2D.Double(0.0, 0.0, w, h, a, a))
      g2.color = c.background.darker()
      g2.draw(RoundRectangle2D.Double(0.0, 0.0, w - 1.0, h - 1.0, a, a))
      g2.dispose()
    }
  }
}

// A two-digit numeric field with a rounded, focus-highlighted background
// and mouse-wheel support. The value wraps around within [min, max].
private class RoundFormattedTextField(
  value: Int,
  private val step: Int,
  private val min: Int,
  private val max: Int,
) : JFormattedTextField("%02d".format(value)) {
  private var handler: Handler? = null

  override fun updateUI() {
    removeFocusListener(handler)
    removeMouseWheelListener(handler)
    super.updateUI()
    setOpaque(false)
    setBackground(FIELD_BACKGROUND)
    // Dimmed until the field gains the focus; see Handler#focusGained
    setForeground(Color.DARK_GRAY)
    setSelectionColor(TRANSPARENT)
    setSelectedTextColor(UIManager.getColor("TextField.foreground"))
    setBorder(BorderFactory.createEmptyBorder())
    setCaret(object : DefaultCaret() {
      override fun isVisible() = false
    })
    setCursor(Cursor.getDefaultCursor())
    handler = Handler().also {
      addFocusListener(it)
      addMouseWheelListener(it)
    }
  }

  // Moves the value by the given number of steps, wrapping around
  // within [min, max] instead of clamping (e.g. 23 + 1 -> 0).
  fun adjustValue(steps: Int) {
    requestFocusInWindow()
    val range = max - min + 1
    val value = getText().toInt()
    val next = Math.floorMod(value - min + steps * step, range) + min
    text = "%02d".format(next)
  }

  override fun paintComponent(g: Graphics) {
    if (hasFocus()) {
      RoundPanel.paintRoundRect(g, this, ARC)
    }
    super.paintComponent(g)
  }

  private inner class Handler :
    FocusListener,
    MouseWheelListener {
    override fun focusGained(e: FocusEvent) {
      setForeground(UIManager.getColor("TextField.foreground"))
    }

    override fun focusLost(e: FocusEvent) {
      setForeground(Color.DARK_GRAY)
    }

    override fun mouseWheelMoved(e: MouseWheelEvent) {
      // Rotating the wheel away from the user (negative) increases the value
      adjustValue(-e.getWheelRotation())
    }
  }

  companion object {
    // Background color used while the field is focused.
    private val FIELD_BACKGROUND = Color(0xCE_CE_CE)

    // Fully transparent so the selection itself is invisible;
    // the focus highlight is drawn instead.
    private val TRANSPARENT = Color(0x0, true)
    private const val ARC = 8
  }
}

// Runs an action when a button is clicked and keeps repeating it
// while the button is held down, like the arrow buttons of a JSpinner.
private class AutoRepeatHandler(
  private val action: Runnable,
) : MouseAdapter(),
  ActionListener {
  private val autoRepeatTimer = Timer(60, this)
  private var arrowButton: AbstractButton? = null

  init {
    autoRepeatTimer.setInitialDelay(300)
  }

  override fun actionPerformed(e: ActionEvent) {
    // The button itself fires once on release; the timer fires while held.
    val released =
      e.getSource() is Timer && arrowButton?.getModel()?.isPressed != true
    if (released) {
      // Safety net: stop repeating if the button was released
      // without this handler receiving mouseReleased.
      autoRepeatTimer.stop()
    } else {
      action.run()
    }
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    if (SwingUtilities.isLeftMouseButton(e) && c.isEnabled && c is AbstractButton) {
      arrowButton = c
      autoRepeatTimer.start()
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    autoRepeatTimer.stop()
  }

  override fun mouseExited(e: MouseEvent) {
    autoRepeatTimer.stop()
  }
}

// A single "HH:mm" field where the mouse wheel adjusts
// the hour or minute depending on the pointer position.
private class TimePickerSingleField {
  private var currentTime = LocalTime.of(12, 30)

  fun createComponent(): JFormattedTextField {
    val field = makeMaskedField("##:##")
    field.setFont(Font("Monospaced", Font.BOLD, 42))
    field.setHorizontalAlignment(JTextField.CENTER)
    field.isEditable = false
    field.text = currentTime.format(TIME_FORMATTER)
    field.addMouseWheelListener { e ->
      // Rotating the wheel away from the user (negative) increases the value
      val steps = -e.getWheelRotation().toLong()
      val isHourSide = field.viewToModel2D(e.getPoint()) <= HOUR_END_INDEX
      // Unlike TimePickerSplitField, the minutes carry over into the hours (12:59 -> 13:00)
      currentTime = if (isHourSide) {
        currentTime.plusHours(steps)
      } else {
        currentTime.plusMinutes(steps)
      }
      field.text = currentTime.format(TIME_FORMATTER)
    }
    return field
  }

  private fun makeMaskedField(pattern: String) = runCatching {
    val mask = MaskFormatter(pattern).also {
      it.placeholderCharacter = '0'
    }
    JFormattedTextField(mask)
  }.getOrNull() ?: JFormattedTextField()

  companion object {
    // Index of the colon in the "HH:mm" mask: caret positions 0-2 are over the hour digits.
    private const val HOUR_END_INDEX = 2
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
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
      contentPane.add(createUI())
      minimumSize = Dimension(256, 200)
      isResizable = false
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
