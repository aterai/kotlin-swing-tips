package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicScrollBarUI

fun makeUI() = JPanel(GridLayout(1, 2)).also {
  it.add(JScrollPane(makeList()))
  it.add(makeTranslucentScrollBar(makeList()))
  it.preferredSize = Dimension(320, 240)
}

private fun makeList(): Component {
  val m = DefaultListModel<String>()
  (0..50)
    .map { "%05d: %s".format(it, LocalDateTime.now(ZoneId.systemDefault())) }
    .forEach { m.addElement(it) }
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
    val availR = scrollPane.bounds
    availR.setLocation(0, 0) // availR.x = availR.y = 0;

    val ins = parent.insets
    availR.x = ins.left
    availR.y = ins.top
    availR.width -= ins.left + ins.right
    availR.height -= ins.top + ins.bottom

    val vsbR = Rectangle()
    vsbR.width = 12
    vsbR.height = availR.height
    vsbR.x = availR.x + availR.width - vsbR.width
    vsbR.y = availR.y

    viewport?.bounds = availR
    vsb?.also {
      it.isVisible = true
      it.bounds = vsbR
    }
  }
}

private class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

private class TranslucentScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  override fun paintTrack(g: Graphics, c: JComponent?, r: Rectangle) {
    // Graphics2D g2 = (Graphics2D) g.create();
    // g2.setPaint(new Color(100, 100, 100, 100));
    // g2.fillRect(r.x, r.y, r.width - 1, r.height - 1);
    // g2.dispose();
  }

  override fun paintThumb(g: Graphics, c: JComponent?, r: Rectangle) {
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
