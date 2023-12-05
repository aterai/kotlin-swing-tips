package example

import java.awt.*
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.plaf.basic.BasicScrollBarUI

fun makeUI() = JPanel(GridLayout(1, 2)).also {
  it.add(JScrollPane(makeList()))
  it.add(makeTranslucentScrollBar(makeList()))
  it.preferredSize = Dimension(320, 240)
}

private fun makeList(): Component {
  val m = DefaultListModel<String>()
  for (i in 0..50) {
    m.addElement("%05d: %s".format(i, LocalDateTime.now(ZoneId.systemDefault())))
  }
  return JList(m)
}

private fun makeTranslucentScrollBar(c: Component) = object : JScrollPane(c) {
  override fun isOptimizedDrawingEnabled() = false // JScrollBar is overlap

  override fun updateUI() {
    super.updateUI()
    EventQueue.invokeLater {
      getVerticalScrollBar().ui = TranslucentScrollBarUI()
      setComponentZOrder(getVerticalScrollBar(), 0)
      setComponentZOrder(getViewport(), 1)
      getVerticalScrollBar().isOpaque = false
    }
    setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    layout = TranslucentScrollPaneLayout()
  }
}

private class TranslucentScrollPaneLayout : ScrollPaneLayout() {
  override fun layoutContainer(parent: Container) {
    val scrollPane = parent as? JScrollPane ?: return
    val availR = SwingUtilities.calculateInnerArea(scrollPane, null)
    viewport?.bounds = availR
    vsb?.also {
      it.setLocation(availR.x + availR.width - BAR_SIZE, availR.y)
      it.setSize(BAR_SIZE, availR.height - BAR_SIZE)
      // it.isVisible = true
    }
  }

  companion object {
    private const val BAR_SIZE = 12
  }
}

private class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

private class TranslucentScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  override fun paintTrack(
    g: Graphics,
    c: JComponent?,
    r: Rectangle,
  ) {
    // val g2 = g.create() as? Graphics2D
    // g2.setPaint(Color(100, 100, 100, 100))
    // g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
    // g2.dispose()
  }

  override fun paintThumb(
    g: Graphics,
    c: JComponent?,
    r: Rectangle,
  ) {
    val sb = c as? JScrollBar ?: return
    val g2 = g.create() as? Graphics2D
    if (g2 == null || !sb.isEnabled || r.width > r.height) {
      return
    }
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = when {
      isDragging -> DRAGGING_COLOR
      isThumbRollover -> ROLLOVER_COLOR
      else -> DEFAULT_COLOR
    }
    g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
    g2.paint = Color.WHITE
    g2.drawRect(r.x, r.y, r.width - 1, r.height - 1)
    g2.dispose()
  }

  companion object {
    private val DEFAULT_COLOR = Color(220, 100, 100, 100)
    private val DRAGGING_COLOR = Color(200, 100, 100, 100)
    private val ROLLOVER_COLOR = Color(255, 120, 100, 100)
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
