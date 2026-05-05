package example

import java.awt.*
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.plaf.basic.BasicScrollBarUI

fun createUI(): Component {
  UIManager.put("ScrollBar.minimumThumbSize", Dimension(12, 20))
  val scroll = createTranslucentScrollPane(createSampleList())
  return JPanel(GridLayout(1, 2)).also {
    it.add(JScrollPane(createSampleList()))
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createSampleList(): Component {
  val m = DefaultListModel<String>()
  for (i in 0..500) {
    m.addElement("%03d: %s".format(i, LocalDateTime.now(ZoneId.systemDefault())))
  }
  return JList(m)
}

private fun createTranslucentScrollPane(c: Component) = object : JScrollPane(c) {
  override fun isOptimizedDrawingEnabled() = false // JScrollBar is overlap

  override fun updateUI() {
    super.updateUI()
    setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS)
    setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
    verticalScrollBar.also {
      it.setUI(TranslucentScrollBarUI())
      it.isOpaque = false
    }
    setComponentZOrder(verticalScrollBar, 0)
    setComponentZOrder(viewport, 1)
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

private class InvisibleButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

private class TranslucentScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = InvisibleButton()

  override fun createIncreaseButton(orientation: Int) = InvisibleButton()

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
    val g2 = g.create() as? Graphics2D ?: return
    if (c is JScrollBar && c.isEnabled) {
      val min = UIManager.getDimension("ScrollBar.minimumThumbSize")
      r.height = r.height.coerceAtLeast(min.height)
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.paint = when {
        isDragging -> DRAGGING_COLOR
        isThumbRollover -> ROLLOVER_COLOR
        else -> DEFAULT_COLOR
      }
      g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
      g2.paint = Color.WHITE
      g2.drawRect(r.x, r.y, r.width - 1, r.height - 1)
    }
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
