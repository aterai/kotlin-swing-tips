package example

import example.DialGeometry.getStepCount
import example.DialGeometry.normalizeAngle
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.text.DateFormatSymbols
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.text.MaskFormatter
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val hourPicker = TimePicker()
val minutePicker = TimePicker(TimePicker.MINUTE_LABELS, Math.PI / 30.0)
val amPmStrings: Array<out String> = DateFormatSymbols.getInstance().amPmStrings
val amButton = JRadioButton(amPmStrings[0])
val pmButton = JRadioButton(amPmStrings[1])
val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
var currentTime: LocalTime = LocalTime.now(ZoneId.systemDefault())
var timeField: JFormattedTextField? = null
var isSyncing = false

fun createUI(): Component {
  timeField = runCatching {
    val mask = MaskFormatter("##:##")
    mask.placeholderCharacter = '0'
    JFormattedTextField(mask)
  }.getOrNull() ?: JFormattedTextField()
  timeField?.also {
    it.setFont(it.getFont().deriveFont(Font.BOLD, 32f))
    it.setHorizontalAlignment(JTextField.CENTER)
    it.setFocusable(true)
  }

  val p = JPanel(GridLayout(1, 2))
  p.add(hourPicker)
  p.add(minutePicker)

  val box = Box.createHorizontalBox()
  box.add(timeField)
  box.add(createAmPmBox())

  hourPicker.addChangeListener {
    onHourOrAmPmChanged()
  }
  minutePicker.addChangeListener {
    applyTime(currentTime.withMinute(minutePicker.getSelectedIndex()))
  }
  val amPmListener = ActionListener {
    onHourOrAmPmChanged()
  }
  amButton.addActionListener(amPmListener)
  pmButton.addActionListener(amPmListener)
  timeField?.addPropertyChangeListener("value") {
    onTimeFieldValueChanged()
  }

  applyTime(currentTime)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createAmPmBox(): Container {
  val p = JPanel(GridLayout(2, 1))
  p.add(amButton)
  p.add(pmButton)
  p.setBackground(timeField?.getBackground())
  val buttonGroup = ButtonGroup()
  for (radioButton in listOf(amButton, pmButton)) {
    buttonGroup.add(radioButton)
    radioButton.setOpaque(false)
  }
  return p
}

private fun onHourOrAmPmChanged() {
  val hour24 = hourPicker.getSelectedIndex() + (if (pmButton.isSelected) 12 else 0)
  applyTime(currentTime.withHour(hour24))
}

private fun onTimeFieldValueChanged() {
  if (!isSyncing) {
    runCatching {
      applyTime(LocalTime.parse(timeField?.value?.toString() ?: "", timeFormatter))
    }.onFailure {
      timeField?.setValue(currentTime.format(timeFormatter))
    }
  }
}

private fun applyTime(time: LocalTime) {
  if (!isSyncing) {
    isSyncing = true
    try {
      currentTime = time
      hourPicker.setSelectedIndex(time.hour % 12)
      minutePicker.setSelectedIndex(time.minute)
      (if (time.hour < 12) amButton else pmButton).setSelected(true)
      timeField!!.setValue(time.format(timeFormatter))
    } finally {
      isSyncing = !isSyncing
    }
  }
}

class TimePicker(
  labels: Array<String> = HOUR_LABELS,
  private val stepAngle: Double = Math.PI / 6.0,
) : JPanel() {
  private val labels = labels.clone()
  private var listener: MouseAdapter? = null
  private var rotation = Math.toRadians(90.0)
  private var dragStartAngle = 0.0
  private var dragStartIndex = 0
  private var handleHovered = false
  private val handleShape = Ellipse2D.Double()

  init {
    EventQueue.invokeLater { updateHandleShape(rotation) }
  }

  override fun updateUI() {
    removeMouseListener(listener)
    removeMouseMotionListener(listener)
    super.updateUI()
    listener = DragMouseListener()
    addMouseListener(listener)
    addMouseMotionListener(listener)
  }

  override fun getPreferredSize(): Dimension = Dimension(200, 200)

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_STROKE_CONTROL,
      RenderingHints.VALUE_STROKE_PURE,
    )
    val rect = SwingUtilities.calculateInnerArea(this, null)
    g2.color = Color.DARK_GRAY
    g2.fill(rect)

    g2.translate(rect.centerX, rect.centerY)
    val radius = DialGeometry.getRadius(rect, MARGIN)

    // Drawing the clock numbers
    DialGeometry.paintClockNumbers(g2, radius, *labels)

    val borderColor = if (handleHovered) HOVER_BORDER_CLR else BORDER_CLR
    val edge = this.handCircleEdgePoint
    g2.color = borderColor
    g2.draw(Line2D.Double(0.0, 0.0, edge.x, edge.y))
    g2.fill(Ellipse2D.Double(-2.0, -2.0, 4.0, 4.0))

    g2.color = Color(0x64_AA_AA_FF, true)
    g2.fill(handleShape)
    g2.color = borderColor
    g2.draw(handleShape)

    g2.dispose()
  }

  override fun doLayout() {
    super.doLayout()
    updateHandleShape(rotation)
  }

  fun getSelectedIndex(): Int {
    val stepCount = getStepCount(stepAngle)
    val index = (normalizeAngle(rotation) / stepAngle).roundToInt()
    return index % stepCount
  }

  fun setSelectedIndex(index: Int) {
    val stepCount = getStepCount(stepAngle)
    val normalizedIndex = ((index % stepCount) + stepCount) % stepCount
    if (normalizedIndex != getSelectedIndex()) {
      rotation = normalizedIndex * stepAngle
      updateHandleShape(rotation)
      repaint()
      fireStateChanged()
    }
  }

  fun addChangeListener(l: ChangeListener?) {
    listenerList.add(ChangeListener::class.java, l)
  }

  fun removeChangeListener(l: ChangeListener?) {
    listenerList.remove(ChangeListener::class.java, l)
  }

  fun fireStateChanged() {
    val event = ChangeEvent(this)
    for (l in listenerList.getListeners(ChangeListener::class.java)) {
      l.stateChanged(event)
    }
  }

  private val handCircleEdgePoint: Point2D
    get() {
      val cx = handleShape.centerX
      val cy = handleShape.centerY
      val dist = hypot(cx, cy)
      val r = handleShape.width / 2.0
      val pt: Point2D = Point2D.Double(0.0, 0.0)
      if (dist > r) {
        val scale = (dist - r) / dist
        pt.setLocation(cx * scale, cy * scale)
      }
      return pt
    }

  private fun updateHandleShape(angle: Double) {
    val rect = SwingUtilities.calculateInnerArea(this, null)
    val radius = DialGeometry.getRadius(rect, MARGIN)
    val handSize = radius * HANDLE_SIZE_RATIO
    val s: Shape = Ellipse2D.Double(
      -handSize / 2.0,
      -handSize / 2.0,
      handSize,
      handSize,
    )
    val at = AffineTransform.getRotateInstance(angle)
    val distance = DialGeometry.getHandleDistance(radius, HANDLE_SIZE_RATIO)
    val transformedShape = DialGeometry.createShapeAtPolarPosition(s, at, distance)
    handleShape.frame = transformedShape.bounds2D
  }

  private inner class DragMouseListener : MouseAdapter() {
    override fun mouseMoved(e: MouseEvent) {
      val c = e.component
      if (c is JComponent) {
        val pt = DialGeometry.toCenterRelativePoint(c, e.getPoint())
        handleHovered = handleShape.contains(pt)
      }
      c.repaint()
    }

    override fun mouseReleased(e: MouseEvent) {
      if (handleHovered) {
        rotation = DialGeometry.snapToNearestStep(rotation, stepAngle)
        updateHandleShape(rotation)
        if (getSelectedIndex() != dragStartIndex) {
          fireStateChanged()
        }
      }
      handleHovered = false
      e.component.repaint()
    }

    override fun mousePressed(e: MouseEvent) {
      val c = e.component as? JComponent ?: return
      val pt = DialGeometry.toCenterRelativePoint(c, e.getPoint())
      if (handleShape.contains(pt)) {
        handleHovered = true
        dragStartIndex = getSelectedIndex()
        dragStartAngle = rotation - atan2(pt.y, pt.x)
        c.repaint()
      }
    }

    override fun mouseDragged(e: MouseEvent) {
      val c = e.component
      if (handleHovered && c is JComponent) {
        val pt = DialGeometry.toCenterRelativePoint(c, e.getPoint())
        rotation = dragStartAngle + atan2(pt.y, pt.x)
        updateHandleShape(rotation)
        c.repaint()
        fireStateChanged()
      }
    }
  }

  companion object {
    val HOUR_LABELS = arrayOf(
      "12",
      "1",
      "2",
      "3",
      "4",
      "5",
      "6",
      "7",
      "8",
      "9",
      "10",
      "11",
    )

    val MINUTE_LABELS = arrayOf(
      "0",
      "5",
      "10",
      "15",
      "20",
      "25",
      "30",
      "35",
      "40",
      "45",
      "50",
      "55",
    )

    const val HANDLE_SIZE_RATIO = .25
    const val MARGIN = 10.0
    val BORDER_CLR: Color = Color.LIGHT_GRAY
    val HOVER_BORDER_CLR: Color = Color.WHITE
  }
}

private object DialGeometry {
  private const val FONT_RATIO = .18f

  fun getRadius(
    rect: Rectangle,
    margin: Double,
  ) = min(rect.width, rect.height) / 2.0 - margin

  fun getHandleDistance(radius: Double, ratio: Double) = radius * (1.0 - ratio / 2.0)

  fun normalizeAngle(angle: Double): Double {
    val twoPi = 2.0 * Math.PI
    val a = angle % twoPi
    return if (a < 0.0) a + twoPi else a
  }

  fun snapToNearestStep(angle: Double, stepAngle: Double): Double {
    val stepped = (angle / stepAngle).roundToInt() * stepAngle
    return normalizeAngle(stepped)
  }

  fun getStepCount(stepAngle: Double) = (2.0 * Math.PI / stepAngle).roundToInt()

  fun createShapeAtPolarPosition(
    s: Shape,
    at: AffineTransform,
    distance: Double,
  ): Shape {
    val r = s.bounds2D
    val ptSrc: Point2D = Point2D.Double(0.0, -distance)
    val pt = at.transform(ptSrc, null)
    val dx = pt.x - r.centerX
    val dy = pt.y - r.centerY
    return AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(s)
  }

  fun paintClockNumbers(g2: Graphics2D, radius: Double, vararg labels: String) {
    g2.color = Color.WHITE
    val dynamicFontSize = max((radius * FONT_RATIO).toFloat(), 10f)
    val font = g2.font.deriveFont(dynamicFontSize)
    val frc = g2.fontRenderContext
    val ty = getHandleDistance(radius, TimePicker.HANDLE_SIZE_RATIO)
    val labelAngle = 2.0 * Math.PI / labels.size
    val at = AffineTransform.getRotateInstance(0.0)
    for (txt in labels) {
      val s = getTextLayout(txt, font, frc).getOutline(null)
      g2.fill(createShapeAtPolarPosition(s, at, ty))
      at.rotate(labelAngle)
    }
  }

  private fun getTextLayout(
    txt: String,
    font: Font,
    frc: FontRenderContext,
  ) = TextLayout(txt, font, frc)

  fun toCenterRelativePoint(c: JComponent, p: Point): Point2D {
    val rect = SwingUtilities.calculateInnerArea(c, null)
    val x = p.getX() - rect.centerX
    val y = p.getY() - rect.centerY
    return Point2D.Double(x, y)
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
      defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}

