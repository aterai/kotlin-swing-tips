package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.text.MaskFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

fun createUI(): Component = JPanel(FlowLayout(FlowLayout.LEFT, 8, 8)).also {
  it.add(JLabel("Select time:"))
  it.add(TimePickerField())
  it.preferredSize = Dimension(320, 240)
}

// TimePickerField - composite widget: masked text field + dropdown button
// HH:mm format (24 hours)
private class TimePickerField : JPanel() {
  private val timeField: JFormattedTextField
  private val popup: TimePickerPopup

  init {
    layout = OverlayLayout(this)

    val dropdownButton: JButton = DropdownButton()
    dropdownButton.setAlignmentX(RIGHT_ALIGNMENT)
    dropdownButton.setAlignmentY(CENTER_ALIGNMENT)
    dropdownButton.addActionListener { togglePopup() }
    add(dropdownButton)

    popup = TimePickerPopup(this)
    dropdownButton.setComponentPopupMenu(popup)

    val mask = TimePickerUtils.createMaskFormatter()
    timeField = if (mask != null) {
      JFormattedTextField(mask)
    } else {
      JFormattedTextField()
    }
    timeField.setHorizontalAlignment(JTextField.LEFT)
    timeField.setFocusLostBehavior(JFormattedTextField.PERSIST)
    timeField.text = TimePickerUtils.nowString
    timeField.setAlignmentX(RIGHT_ALIGNMENT)
    timeField.setColumns(10)
    add(timeField)
  }

  override fun isOptimizedDrawingEnabled() = false

  override fun isOpaque() = false

  val timeText: String
    // Getter method to retrieve the text from the text field
    get() = timeField.getText()

  fun applyTime(text: String) {
    timeField.text = text
  }

  private fun togglePopup() {
    if (popup.isVisible) {
      popup.setVisible(false)
    } else {
      // Since the common synchronization is consolidated in the PopupMenuListener,
      // just show it at the specified position here
      popup.show(this, 0, getHeight())
    }
  }
}

// TimePickerPopup - JPopupMenu with hour / minute columns
private class TimePickerPopup(
  private val owner: TimePickerField,
) : JPopupMenu() {
  private val panel: TimePickerPopupPanel
  private var handler: PopupMenuListener? = null

  init {
    val hourModel = (0..24).map { h -> "%02d".format(h) }
    val minModel = (0..60).map { m -> "%02d".format(m) }
    panel = TimePickerPopupPanel(owner, hourModel, minModel)
    add(panel)
  }

  override fun updateUI() {
    removePopupMenuListener(handler)
    super.updateUI()
    // Add a listener to monitor events right before the popup becomes visible
    handler = TimePickerPopupListener()
    addPopupMenuListener(handler)
  }

  // Override the default placement position determined by ComponentPopupMenu
  // in environments like Windows LookAndFeel
  override fun show(invoker: Component?, x: Int, y: Int) {
    setInvoker(invoker)
    val p = popupMenuLocation()
    if (p != null) {
      // Pass screen coordinates directly to setLocation
      setLocation(p.x, p.y)
      setVisible(true)
    } else {
      super.show(invoker, x, y)
    }
  }

  // Calculates the appropriate screen coordinates where the popup should be displayed.
  private fun popupMenuLocation(): Point? {
    var p: Point? = null
    val invoker = getInvoker()
    if (invoker != null && invoker.isShowing()) {
      // Regardless of which component is the invoker,
      // always base it on the bottom-left edge of the TimePickerField
      p = owner.locationOnScreen
      p.y += owner.getHeight()
    }
    return p
  }

  private inner class TimePickerPopupListener : PopupMenuListener {
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
      // Always synchronize with the field time before showing
      panel.synchronizeFromField(owner.timeText)

      // Force correction of unexpected display position shifts
      // caused by right-clicks, etc.
      val p = popupMenuLocation()
      if (p != null) {
        setInvoker(owner)
        setLocation(p.x, p.y)
      }
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
      // No operation needed
    }

    override fun popupMenuCanceled(e: PopupMenuEvent?) {
      // No operation needed
    }
  }
}

/** Panel that contains the drum roll pickers and footer actions extracted from the popup.  */
private class TimePickerPopupPanel(
  private val owner: TimePickerField,
  hourModel: List<String>,
  minModel: List<String>,
) : JPanel(BorderLayout(0, 0)) {
  private val hourPicker = DrumRollPicker(hourModel)
  private val minPicker = DrumRollPicker(minModel)

  init {

    val pickers = JPanel(GridLayout(1, 2, 2, 2))
    pickers.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
    val loc = Locale.getDefault()
    val hourName = ChronoField.HOUR_OF_DAY.getDisplayName(loc)
    pickers.add(createColumn(hourName, hourPicker))
    val minName = ChronoField.MINUTE_OF_HOUR.getDisplayName(loc)
    pickers.add(createColumn(minName, minPicker))

    add(pickers, BorderLayout.CENTER)
    add(buildFooter(), BorderLayout.SOUTH)
  }

  private fun buildFooter(): JPanel {
    val resetBtn = JButton("Now")
    resetBtn.addActionListener { synchronizeFromField(TimePickerUtils.nowString) }
    val okBtn = JButton("OK")
    okBtn.addActionListener { applyAndClose() }
    val footer = JPanel(FlowLayout(FlowLayout.TRAILING, 6, 1))
    footer.add(resetBtn)
    footer.add(okBtn)
    return footer
  }

  fun synchronizeFromField(text: String) {
    val t = TimePickerUtils.parseTime(text)
    val hour = t[0] // 0..23
    val min = t[1] // 0..59
    // DrumRollPicker#setSelectedIndex clamps or wraps the index by itself,
    // so no extra scrolling is required
    hourPicker.selectedIndex = hour
    minPicker.selectedIndex = min
  }

  private fun applyAndClose() {
    val hour = hourPicker.selectedItem
    val min = minPicker.selectedItem
    owner.applyTime("$hour:$min")
    val popup = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, this)
    if (popup is JPopupMenu) {
      popup.setVisible(false)
    }
  }

  companion object {
    private fun createColumn(label: String?, picker: DrumRollPicker?): JPanel {
      val lbl = JLabel(label, SwingConstants.CENTER)
      lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0))
      val col = JPanel(BorderLayout(0, 1))
      col.setOpaque(false)
      col.add(lbl, BorderLayout.NORTH)
      col.add(picker)
      return col
    }
  }
}

// Remaining helper classes (unchanged)
private class DropdownButton : JButton() {
  override fun updateUI() {
    super.updateUI()
    val c1 = UIManager.getColor("ComboBox.foreground")
    val c2 = UIManager.getColor("ComboBox.selectionBackground")
    setIcon(CharIcon("⏰", c1, c2, 10))
    setBorderPainted(false)
    setContentAreaFilled(false)
    setFocusPainted(false)
    setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5))
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
  }
}

// Static utility methods shared by TimePickerField and TimePickerPopup
private object TimePickerUtils {
  private val TIME_DELIMITER = Pattern.compile("[:\\s]+")

  /** Returns current time as "HH:mm" (24-hour).  */
  val nowString: String
    get() {
      val fmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
      return LocalTime.now(ZoneId.systemDefault()).format(fmt)
    }

  /** Parses "HH:mm" text into {hour, minute}.  */
  fun parseTime(text: String): IntArray {
    val parts: Array<String> = TIME_DELIMITER.split(text.trim())
    val hour = parts[0].trim().toInt()
    val min = parts[1].trim().toInt()
    return intArrayOf(hour, min)
  }

  /** Creates a [MaskFormatter] for the "##:##" pattern.  */
  fun createMaskFormatter(): MaskFormatter? = runCatching {
    MaskFormatter("##:##").also {
      it.placeholderCharacter = '_'
      it.commitsOnValidEdit = false
    }
  }.getOrNull()
}

private class CharIcon(
  private val name: String,
  private val color: Color,
  private val rollover: Color,
  private val size: Int,
) : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = color
    if (c is AbstractButton) {
      val m = c.getModel()
      if (m.isRollover) {
        g2.paint = rollover
      }
    }
    val fontMetrics = g2.fontMetrics
    g2.translate(x, y)
    val tx = (size - fontMetrics.stringWidth(name)) / 2
    val ty = (size - fontMetrics.height) / 2 + fontMetrics.ascent
    g2.drawString(name, tx, ty)
    g2.dispose()
  }

  override fun getIconWidth() = size

  override fun getIconHeight() = size
}

private class DrumRollPicker(
  private val items: List<String>,
) : JPanel() {
  private var index = 0
  private var pressedY = 0
  private var dragging = false
  private var handler: PickerHandler? = null
  private var scroller: DrumRollScroller? = null

  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    removeMouseWheelListener(handler)
    removeFocusListener(handler)
    scroller?.setOffset(0)
    super.updateUI()
    setOpaque(true)
    setFocusable(true)
    setBackground(UIManager.getColor("List.background"))
    setForeground(UIManager.getColor("List.foreground"))
    setFont(UIManager.getFont("List.font"))
    handler = PickerHandler()
    addMouseListener(handler)
    addMouseMotionListener(handler)
    addMouseWheelListener(handler)
    addFocusListener(handler)
    scroller = DrumRollScroller(this)
    val im = getInputMap(WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke("UP"), "previous")
    im.put(KeyStroke.getKeyStroke("DOWN"), "next")
    val am = actionMap
    am.put("previous", PickerAction(-1))
    am.put("next", PickerAction(1))
  }

  override fun getPreferredSize(): Dimension = if (isPreferredSizeSet) {
    super.getPreferredSize()
  } else {
    Dimension(COLUMN_WIDTH, ITEM_HEIGHT * VISIBLE_ROWS)
  }

  // Stop the timer when the popup is closed so it does not keep ticking while hidden.
  // Jump to the end of the animation: the index is already final, so only the
  // offset needs clearing to avoid reappearing with a stale drum position
  override fun removeNotify() {
    scroller?.setOffset(0)
    super.removeNotify()
  }

  val selectedItem: String
    get() = items[index]

  /**
   * Selects the item at the given index without animation:
   * wraps around or clamps it if out of range.
   */
  var selectedIndex: Int
    get() = index
    set(idx) {
      index = if (this.isCyclic) {
        Math.floorMod(idx, items.size)
      } else {
        idx.coerceIn(0, items.size - 1)
      }
      // Java 21: index = isCyclic() ? ... : Math.clamp(idx, 0, items.size() - 1);
      // Cancels a running animation and puts the drum back on the item
      scroller?.setOffset(0)
      repaint()
    }

  /**
   * Moves the selection by the given number of items with an easing animation.
   * The index is updated immediately and only the painting catches up,
   * so the selected value is always the final one even mid-animation.
   */
  fun scroll(delta: Int) {
    val prev = index
    val offset = scroller?.getOffset() ?: 0 // offset of an animation still in flight
    this.selectedIndex = index + delta
    // Clamped at both ends when the drum is not cyclic, so the drum may move less
    val moved = if (this.isCyclic) delta else index - prev
    // Shift the drum back by the distance it just jumped, then slide that offset to zero
    val max = ITEM_HEIGHT * MAX_ANIM_ROWS
    val from = offset + moved * ITEM_HEIGHT
    scroller?.start(from.coerceIn(-max, max))
  }

  private val isCyclic: Boolean
    // The drum rotates endlessly only when it has more items than the visible rows
    get() = items.size > VISIBLE_ROWS

  // Returns the item index for the given offset from the selected item, or -1 if it is empty
  private fun itemIndexAt(offset: Int): Int {
    var idx = index + offset
    if (this.isCyclic) {
      idx = Math.floorMod(idx, items.size)
    } else if (idx < 0 || idx >= items.size) {
      idx = -1
    }
    return idx
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
    )
    val w = getWidth()
    val h = getHeight()
    val centerY = h / 2
    paintSelectionBand(g2, w, centerY)
    // While the drum is offset, the rows that scrolled in from the edge must be
    // painted too, otherwise a gap appears at the leading edge
    val offset = scroller?.getOffset() ?: 0
    val rows = PAINT_ROWS + (abs(offset) + ITEM_HEIGHT - 1) / ITEM_HEIGHT
    for (i in -rows..rows) {
      val idx = itemIndexAt(i)
      if (idx >= 0) {
        paintItem(g2, items[idx], w, centerY + i * ITEM_HEIGHT + offset, centerY)
      }
    }
    paintFadeGradient(g2, w, h, centerY)
    g2.dispose()
  }

  // Highlight band that indicates the selected item
  private fun paintSelectionBand(g2: Graphics2D, w: Int, centerY: Int) {
    val y = centerY - ITEM_HEIGHT / 2
    val sc = UIManager.getColor("List.selectionBackground")
    val bc = sc ?: getForeground()
    g2.paint = Color(bc.red, bc.green, bc.blue, BAND_ALPHA)
    g2.fillRect(0, y, w, ITEM_HEIGHT)
    g2.paint = if (hasFocus()) bc else getForeground()
    g2.drawLine(0, y, w, y)
    g2.drawLine(0, y + ITEM_HEIGHT, w, y + ITEM_HEIGHT)
  }

  // The farther from the center, the smaller and more transparent the item becomes
  private fun paintItem(g2: Graphics2D, text: String, w: Int, y: Int, centerY: Int) {
    val d = abs(y - centerY) / ITEM_HEIGHT.toFloat()
    val alpha = max(0f, 1f - d * ALPHA_STEP)
    if (alpha > 0) {
      val scale = max(MIN_SCALE, MAX_SCALE - d * SCALE_STEP)
      val font = getFont()
      val style = if (d < .5f) Font.BOLD else Font.PLAIN
      g2.font = font.deriveFont(style, font.size2D * scale)
      val fg = getForeground()
      g2.color = Color(
        fg.red,
        fg.green,
        fg.blue,
        (alpha * MAX_ALPHA).toInt(),
      )
      val fm = g2.fontMetrics
      val tx = (w - fm.stringWidth(text)) / 2
      val ty = y + (fm.ascent - fm.descent) / 2
      g2.drawString(text, tx, ty)
    }
  }

  // Overlay gradient that fades out the top and bottom of the drum
  private fun paintFadeGradient(g2: Graphics2D, w: Int, h: Int, centerY: Int) {
    val bg = getBackground()
    val tc = Color(bg.red, bg.green, bg.blue, 0)
    val fade = max(0, centerY - ITEM_HEIGHT)
    g2.paint = GradientPaint(0f, 0f, bg, 0f, fade.toFloat(), tc)
    g2.fillRect(0, 0, w, fade)
    g2.paint = GradientPaint(0f, h.toFloat(), bg, 0f, (h - fade).toFloat(), tc)
    g2.fillRect(0, h - fade, w, fade)
  }

  private inner class PickerAction(
    private val delta: Int,
  ) : AbstractAction() {
    override fun actionPerformed(e: ActionEvent?) {
      scroll(delta)
    }
  }

  private inner class PickerHandler :
    MouseInputAdapter(),
    FocusListener {
    override fun mousePressed(e: MouseEvent) {
      pressedY = e.getY()
      dragging = false
      // Grabbing the drum takes over from a running animation:
      // freeze it where it is instead of snapping to the item
      scroller?.stop()
      requestFocusInWindow()
    }

    override fun mouseDragged(e: MouseEvent) {
      var delta = e.getY() - pressedY
      if (abs(delta) > DRAG_THRESHOLD) {
        dragging = true
      }
      // Dragging downwards brings the previous items to the center
      val steps = delta / ITEM_HEIGHT
      if (steps != 0) {
        pressedY += steps * ITEM_HEIGHT
        delta -= steps * ITEM_HEIGHT
        selectedIndex = index - steps
      }
      // Stop the drum at both ends if it is not cyclic
      val top = delta > 0 && itemIndexAt(-1) < 0
      val bottom = delta < 0 && itemIndexAt(1) < 0
      scroller?.setOffset(if (top || bottom) 0 else delta)
      repaint()
    }

    override fun mouseReleased(e: MouseEvent) {
      // Clicking a neighbor item moves it to the center: the item under the cursor is
      // shifted by the drum offset, so subtract it to find the row actually clicked
      val offset = scroller?.getOffset() ?: 0
      val dy = e.getY() - getHeight() / 2f - offset
      val rows = if (dragging) 0 else (dy / ITEM_HEIGHT).roundToInt()
      dragging = false
      // Also eases the leftover drag offset back to the center when rows is zero
      scroll(rows)
    }

    override fun mouseWheelMoved(e: MouseWheelEvent) {
      scroll(e.getWheelRotation())
    }

    override fun focusGained(e: FocusEvent?) {
      repaint()
    }

    override fun focusLost(e: FocusEvent?) {
      repaint()
    }
  }

  companion object {
    private const val ITEM_HEIGHT = 26
    private const val VISIBLE_ROWS = 5 // odd number: center item + neighbors
    private const val COLUMN_WIDTH = 24

    // Number of items painted on each side of the center: one extra row for dragging
    private const val PAINT_ROWS = VISIBLE_ROWS / 2 + 1
    private const val ALPHA_STEP = .35f // alpha decrease per item
    private const val MAX_SCALE = 1.5f // font scale of the center item
    private const val SCALE_STEP = .25f // font scale decrease per item
    private const val MIN_SCALE = .8f
    private const val BAND_ALPHA = 48
    private const val DRAG_THRESHOLD = 4
    private const val MAX_ALPHA = 255

    // Upper bound of the animated distance: a fast wheel spin catches up instead of
    // sliding through every item
    private const val MAX_ANIM_ROWS = 2
  }
}

/**
 * Slides a pixel offset back to zero with a cubic ease-out.
 * The drum is painted at this offset, so the picker can update its selected
 * index immediately and let the drawing catch up afterward.
 */
private class DrumRollScroller(
  private val view: JComponent,
) {
  private val animator = Timer(FRAME_DELAY) { update() }
  private var offset = 0 // pixel offset of the drum: zero while it rests on an item
  private var from = 0 // offset the current animation starts from
  private var startTime: Long = 0

  fun getOffset() = offset

  // Places the drum by hand while dragging: setOffset(0) also cancels an animation
  fun setOffset(px: Int) {
    animator.stop()
    offset = px
  }

  // Interrupts an animation without moving the drum
  fun stop() {
    animator.stop()
  }

  // Slides the offset from the given value back to zero, redirecting a running animation
  fun start(fromOffset: Int) {
    from = fromOffset
    if (from != 0) {
      offset = from
      // nanoTime is monotonic and fine-grained: currentTimeMillis has a granularity
      // close to FRAME_DELAY on some platforms, which makes the easing look stepped
      startTime = System.nanoTime()
      animator.restart()
      view.repaint()
    }
  }

  // Called on every timer tick: eases the offset toward zero and stops at the end
  private fun update() {
    val elapsed = System.nanoTime() - startTime
    if (elapsed >= DURATION) {
      setOffset(0)
    } else {
      val progress = elapsed / DURATION.toFloat()
      // easeOut is applied to the remaining distance, so the offset reaches zero
      offset = (from * (1f - easeOut(progress))).roundToInt()
    }
    view.repaint()
  }

  companion object {
    private const val DURATION = 180000000L // 180ms in nanoseconds
    private const val FRAME_DELAY = 15 // interval between animation frames (~66fps)

    // Cubic ease-out: starts fast and gently settles into the selection band
    private fun easeOut(progress: Float): Float {
      val remaining = 1f - progress
      return 1f - remaining * remaining * remaining
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
