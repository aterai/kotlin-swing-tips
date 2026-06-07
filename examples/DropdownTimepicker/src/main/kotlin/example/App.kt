package example

import java.awt.*
import java.text.DateFormatSymbols
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.JFormattedTextField.AbstractFormatter
import javax.swing.JPanel
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.BasicScrollBarUI
import javax.swing.text.DefaultFormatter
import javax.swing.text.MaskFormatter
import kotlin.math.max
import kotlin.math.min

fun createUI() = JPanel(FlowLayout(FlowLayout.LEFT, 8, 8)).also {
  it.add(JLabel("Select time:"))
  it.add(TimePickerField())
  it.preferredSize = Dimension(320, 240)
}

private class TimePickerField : JPanel() {
  private val timeField: JFormattedTextField
  private val popup: TimePickerPopup

  init {
    setLayout(OverlayLayout(this))

    val dropdownButton = DropdownButton()
    dropdownButton.setAlignmentX(RIGHT_ALIGNMENT)
    dropdownButton.setAlignmentY(CENTER_ALIGNMENT)
    dropdownButton.addActionListener { togglePopup() }
    add(dropdownButton)

    popup = TimePickerPopup(this)
    dropdownButton.setComponentPopupMenu(popup)

    runCatching {
      MaskFormatter("##:## **").also {
        it.placeholderCharacter = '_'
        it.commitsOnValidEdit = false
      }
    }

    timeField = JFormattedTextField(createFormatter())
    timeField.setHorizontalAlignment(JTextField.LEFT)
    timeField.setFocusLostBehavior(JFormattedTextField.PERSIST)
    timeField.text = getNowString()
    timeField.setAlignmentX(RIGHT_ALIGNMENT)
    timeField.setColumns(10)
    add(timeField)
  }

  override fun isOptimizedDrawingEnabled() = false

  override fun isOpaque() = false

  fun getTimeText(): String = timeField.getText()

  private fun togglePopup() {
    if (popup.isVisible) {
      popup.setVisible(false)
    } else {
      popup.show(this, 0, getHeight())
    }
  }

  fun applyTime(text: String?) {
    timeField.text = text
  }

  companion object {
    private val TIME_DELIMITER = Pattern.compile("[:\\s]+")

    private fun createFormatter(): AbstractFormatter {
      val formatter = DefaultFormatter()
      formatter.overwriteMode = true
      formatter.allowsInvalid = true
      formatter.commitsOnValidEdit = false
      return formatter
    }

    fun getNowString(): String {
      val fmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
      return LocalTime.now(ZoneId.systemDefault()).format(fmt).uppercase()
    }

    fun parseTime(text: String): IntArray {
      val parts = TIME_DELIMITER.split(text.trim())
      val hour = parts[0].trim { it <= ' ' }.toInt()
      val min = parts[1].trim { it <= ' ' }.toInt()
      val ampmIdx = if (parts.size > 2 &&
        "PM".equals(parts[2].trim(), true)
      ) {
        1
      } else {
        0
      }
      return intArrayOf(hour, min, ampmIdx)
    }

    fun getAmPmStrings(): Array<String> =
      DateFormatSymbols.getInstance().amPmStrings
  }
}

private class TimePickerPopup(
  private val owner: TimePickerField,
) : JPopupMenu() {
  private val hourList: JList<String>
  private val minList: JList<String>
  private val ampmList: JList<String>
  private val hourModel = (1..12).map { "%02d".format(it) }.toList()
  private val minModel = (0..<60).map { "%02d".format(it) }.toList()

  init {
    hourList = createList(hourModel.toTypedArray<String>())
    minList = createList(minModel.toTypedArray<String>())
    ampmList = createList(TimePickerField.getAmPmStrings())

    val listsPanel = JPanel(GridBagLayout())
    listsPanel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6))

    val c = GridBagConstraints()
    c.fill = GridBagConstraints.BOTH
    c.weightx = 1.0
    c.weighty = 1.0
    c.insets = Insets(0, 2, 0, 2)

    c.gridx = GridBagConstraints.RELATIVE
    val loc = Locale.getDefault()
    val hourLabel = ChronoField.HOUR_OF_DAY.getDisplayName(loc)
    listsPanel.add(createColumn(hourLabel, hourList, true), c)

    val minLabel = ChronoField.MINUTE_OF_HOUR.getDisplayName(loc)
    listsPanel.add(createColumn(minLabel, minList, true), c)

    val ampmLabel = getAmpmLabel(loc)
    listsPanel.add(createColumn(ampmLabel, ampmList, false), c)

    val root = JPanel(BorderLayout(0, 0))
    root.add(listsPanel, BorderLayout.CENTER)
    root.add(createFooter(), BorderLayout.SOUTH)

    setLayout(BorderLayout())
    add(root)

    addPopupMenuListener(object : PopupMenuListener {
      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
        synchronizeFromField(owner.getTimeText())
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
    })
  }

  private fun popupMenuLocation(): Point? {
    val invoker = getInvoker()
    if (invoker == null || !invoker.isShowing()) {
      return null
    }
    val p = owner.locationOnScreen
    p.y += owner.getHeight()
    return p
  }

  override fun show(invoker: Component, x: Int, y: Int) {
    setInvoker(invoker)
    val p = popupMenuLocation()
    if (p != null) {
      setLocation(p.x, p.y)
      setVisible(true)
    } else {
      super.show(invoker, x, y)
    }
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.width = 240
    return d
  }

  private fun createList(model: Array<String>): JList<String> {
    val list = JList(model)
    list.selectionMode = ListSelectionModel.SINGLE_SELECTION
    list.setFixedCellHeight(20)
    list.setFocusable(true)
    list.setCellRenderer(object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(
        l: JList<*>,
        value: Any?,
        idx: Int,
        sel: Boolean,
        focus: Boolean,
      ): Component {
        super.getListCellRendererComponent(l, value, idx, sel, focus)
        setHorizontalAlignment(CENTER)
        setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4))
        return this
      }
    })
    return list
  }

  private fun createColumn(
    label: String,
    list: JList<String>,
    alwaysScroll: Boolean,
  ): JPanel {
    val lbl = JLabel(label, SwingConstants.CENTER)
    lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0))
    lbl.setFont(lbl.getFont().deriveFont(9f))
    val sp = if (alwaysScroll) {
      TranslucentScrollPane(list)
    } else {
      JScrollPane(list)
    }
    val col = JPanel(BorderLayout(0, 1))
    col.setOpaque(false)
    col.add(lbl, BorderLayout.NORTH)
    col.add(sp)
    return col
  }

  private fun createFooter(): JPanel {
    val resetBtn = JButton("Now")
    resetBtn.addActionListener {
      synchronizeFromField(TimePickerField.getNowString())
    }
    val okBtn = JButton("OK")
    okBtn.addActionListener { applyAndClose() }
    val footer = JPanel(FlowLayout(FlowLayout.TRAILING, 6, 1))
    footer.add(resetBtn)
    footer.add(okBtn)
    return footer
  }

  fun synchronizeFromField(text: String) {
    val t = TimePickerField.parseTime(text)
    val hour = t[0] // 1..12
    val min = t[1] // 0..59
    val ampm = t[2] // 0=AM, 1=PM
    val hourIndex = min(max(hour - 1, 0), 11)
    hourList.setSelectedIndex(hourIndex)
    minList.setSelectedIndex(min)
    ampmList.setSelectedIndex(ampm)
    EventQueue.invokeLater {
      scrollToSelected(hourList)
      scrollToSelected(minList)
      scrollToSelected(ampmList)
    }
  }

  private fun scrollToSelected(list: JList<*>) {
    val idx = list.selectedIndex
    if (idx >= 0) {
      val cell = list.getCellBounds(idx, idx)
      if (cell != null) {
        val vis = list.getVisibleRect()
        vis.y = max(0, cell.y + cell.height / 2 - vis.height / 2)
        list.scrollRectToVisible(vis)
      }
    }
  }

  private fun applyAndClose() {
    val hourIndex = hourList.selectedIndex
    val minuteIndex = minList.selectedIndex
    val ampmIndex = ampmList.selectedIndex
    val hour = if (hourIndex >= 0) hourModel[hourIndex] else "12"
    val min = if (minuteIndex >= 0) minModel[minuteIndex] else "00"
    val ampmStrings = TimePickerField.getAmPmStrings()
    val ampm = if (ampmIndex == 1) ampmStrings[1] else ampmStrings[0]
    owner.applyTime(hour + ":" + min + " " + ampm.uppercase())
    setVisible(false)
  }

  private fun getAmpmLabel(loc: Locale): String {
    val ampmRaw = ChronoField.AMPM_OF_DAY.getDisplayName(loc)
    val ampmLabel: String?
    val b1 = ampmRaw != "AmPmOfDay"
    val b2 = ampmRaw != "AMPM_OF_DAY"
    if (ampmRaw?.isNotEmpty() == true && b1 && b2) {
      ampmLabel = ampmRaw
    } else {
      val ap2 = DateFormatSymbols.getInstance(loc).amPmStrings
      ampmLabel = ap2[0] + "/" + ap2[1]
    }
    return ampmLabel
  }
}

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

private class CharIcon(
  private val name: String,
  private val color: Color,
  private val rollover: Color,
  private val size: Int,
) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = color
    if (c is AbstractButton && c.getModel().isRollover) {
      g2.paint = rollover
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

private class TranslucentScrollPane(
  view: Component,
) : JScrollPane(view) {
  // JScrollBar is overlap
  override fun isOptimizedDrawingEnabled() = false

  override fun updateUI() {
    super.updateUI()
    EventQueue.invokeLater {
      getVerticalScrollBar().setUI(TranslucentScrollBarUI())
      setComponentZOrder(getVerticalScrollBar(), 0)
      setComponentZOrder(getViewport(), 1)
      getVerticalScrollBar().setOpaque(false)
    }
    setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS)
    setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
    layout = TranslucentScrollPaneLayout()
  }
}

private class TranslucentScrollPaneLayout : ScrollPaneLayout() {
  override fun layoutContainer(parent: Container?) {
    if (parent is JScrollPane) {
      val availR = SwingUtilities.calculateInnerArea(parent, null)
      viewport?.bounds = availR
      val w = TranslucentScrollBarUI.BAR_WIDTH
      vsb?.setLocation(availR.x + availR.width - w, availR.y)
      vsb?.setSize(w, availR.height)
    }
  }
}

private class InvisibleButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

private class TranslucentScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = InvisibleButton()

  override fun createIncreaseButton(orientation: Int) = InvisibleButton()

  override fun paintTrack(g: Graphics, c: JComponent, r: Rectangle) {
    // BasicScrollBarUI overrides
  }

  override fun paintThumb(g: Graphics, c: JComponent?, r: Rectangle) {
    if (c != null && c.isEnabled && r.width <= r.height) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      val color = getThumbColor()
      if (DEFAULT_COLOR == color) {
        val dw = r.width - c.getPreferredSize().width
        r.x += dw
        r.width -= dw
      }
      g2.paint = color
      g2.fillRect(r.x, r.y, r.width - 2, r.height - 1)
      g2.dispose()
    }
  }

  private fun getThumbColor(): Color {
    val color = if (isDragging) {
      DRAGGING_COLOR
    } else if (isThumbRollover) {
      ROLLOVER_COLOR
    } else {
      DEFAULT_COLOR
    }
    return color
  }

  companion object {
    const val BAR_WIDTH = 8
    private val DEFAULT_COLOR = Color(0x64_64_B4_FF, true)
    private val DRAGGING_COLOR = Color(0x64_64_B4_C8, true)
    private val ROLLOVER_COLOR = Color(0x64_64_B4_DC, true)
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
