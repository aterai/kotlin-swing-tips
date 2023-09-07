package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.time.LocalTime
import java.time.ZoneId
import javax.swing.*

private val timer = Timer(100, null)
private var time = LocalTime.now(ZoneId.systemDefault())
private const val COLUMN = 4
private const val ROW = 7
private val NUMBERS = listOf(
  setOf(0, 1, 2, 3, 4, 5, 6, 7, 13, 14, 20, 21, 22, 23, 24, 25, 26, 27),
  setOf(21, 22, 23, 24, 25, 26, 27),
  setOf(0, 3, 4, 5, 6, 7, 10, 13, 14, 17, 20, 21, 22, 23, 24, 27),
  setOf(0, 3, 6, 7, 10, 13, 14, 17, 20, 21, 22, 23, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 10, 17, 21, 22, 23, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 6, 7, 10, 13, 14, 17, 20, 21, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 4, 5, 6, 7, 10, 13, 14, 17, 20, 21, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 7, 14, 21, 22, 23, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 4, 5, 6, 7, 10, 13, 14, 17, 20, 21, 22, 23, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 6, 7, 10, 13, 14, 17, 20, 21, 22, 23, 24, 25, 26, 27)
)
private val DOT = listOf(2, 4)

fun makeUI(): Component {
  val model1 = object : DefaultListModel<Boolean>() {
    override fun getElementAt(index: Int) = getHoursMinutesDotMatrix(time, index)
  }
  model1.size = (COLUMN * 4 + 5) * ROW
  val hoursMinutes = makeLedMatrixList(model1, Dimension(10, 10))
  val model2 = object : DefaultListModel<Boolean>() {
    override fun getElementAt(index: Int) = getSecondsDotMatrix(time, index)
  }
  model2.size = (COLUMN * 2 + 1) * ROW
  val seconds = makeLedMatrixList(model2, Dimension(8, 8))
  timer.addActionListener {
    time = LocalTime.now(ZoneId.systemDefault())
    hoursMinutes.repaint()
    seconds.repaint()
  }
  hoursMinutes.alignmentY = Component.BOTTOM_ALIGNMENT
  seconds.alignmentY = Component.BOTTOM_ALIGNMENT

  val box = Box.createHorizontalBox()
  box.add(hoursMinutes)
  box.add(Box.createHorizontalStrut(10))
  box.add(seconds)

  val p = object : JPanel(GridBagLayout()) {
    private var listener: HierarchyListener? = null

    override fun updateUI() {
      removeHierarchyListener(listener)
      super.updateUI()
      listener = HierarchyListener { e ->
        if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
          if (e.component.isShowing) {
            timer.start()
          } else {
            timer.stop()
          }
        }
      }
      addHierarchyListener(listener)
    }
  }
  p.add(box)
  p.background = Color.BLACK
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun contains(index: Int, start: Int, end: Int, num: Int) =
  index < end * ROW && NUMBERS[num].contains(index - start * ROW)

@Suppress("ReturnCount")
private fun getHoursMinutesDotMatrix(time: LocalTime, index: Int): Boolean {
  val ten = 10
  val hours = time.hour
  val h1 = hours / ten
  var start = 0
  var end = start + COLUMN
  if (contains(index, start, end, h1)) {
    return hours >= ten
  }
  val gap = 1
  val h2 = hours - h1 * ten
  start = end + gap
  end = start + COLUMN
  if (contains(index, start, end, h2)) {
    return true
  }
  val seconds = time.second
  val s1 = seconds / ten
  val s2 = seconds - s1 * ten
  start = end + gap
  end = start + gap
  if (index < end * ROW && s2 % 2 == 0 && DOT.contains(index - start * ROW)) {
    return true
  }
  val minutes = time.minute
  val m1 = minutes / ten
  start = end + gap
  end = start + COLUMN
  if (contains(index, start, end, m1)) {
    return true
  }
  val m2 = minutes - m1 * ten
  start = end + gap
  end = start + COLUMN
  return contains(index, start, end, m2)
}

private fun getSecondsDotMatrix(time: LocalTime, index: Int): Boolean {
  val ten = 10
  val seconds = time.second
  val s1 = seconds / ten
  var start = 0
  var end = start + COLUMN
  if (contains(index, start, end, s1)) {
    return true
  }
  val gap = 1
  val s2 = seconds - s1 * ten
  start = end + gap
  end = start + COLUMN
  return contains(index, start, end, s2)
}

private fun makeLedMatrixList(m: ListModel<Boolean>, d: Dimension) = object : JList<Boolean>(m) {
  override fun updateUI() {
    fixedCellWidth = d.width
    fixedCellHeight = d.height
    visibleRowCount = ROW
    cellRenderer = null
    super.updateUI()
    layoutOrientation = VERTICAL_WRAP
    isFocusable = false
    val renderer = cellRenderer
    val on = LedDotIcon(true, d)
    val off = LedDotIcon(false, d)
    cellRenderer = ListCellRenderer { list, value, index, _, _ ->
      renderer.getListCellRendererComponent(list, null, index, false, false).also {
        (it as? JLabel)?.icon = if (java.lang.Boolean.TRUE == value) on else off
      }
    }
    border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    background = Color.BLACK
  }
}

private class LedDotIcon(private val led: Boolean, private val dim: Dimension) : Icon {
  private val on = Color(0x32_FF_AA)

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    // JList#setLayoutOrientation(VERTICAL_WRAP) + SynthLookAndFeel(Nimbus, GTK) bug???
    // g2.translate(x, y)
    g2.paint = if (led) on else c.background
    g2.fillOval(0, 0, iconWidth - 1, iconHeight - 1)
    g2.dispose()
  }

  override fun getIconWidth() = dim.width

  override fun getIconHeight() = dim.height
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
