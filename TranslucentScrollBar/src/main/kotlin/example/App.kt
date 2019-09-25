package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicScrollBarUI

class MainPanel : JPanel(GridLayout(1, 2)) {
  init {
    add(JScrollPane(makeList()))
    add(makeTranslucentScrollBar(makeList()))
    setPreferredSize(Dimension(320, 240))
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
        getVerticalScrollBar().setUI(TranslucentScrollBarUI())
        setComponentZOrder(getVerticalScrollBar(), 0)
        setComponentZOrder(getViewport(), 1)
        getVerticalScrollBar().setOpaque(false)
      }
      setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
      setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
      setLayout(TranslucentScrollPaneLayout())
    }
  }
}

internal class TranslucentScrollPaneLayout : ScrollPaneLayout() {
  override fun layoutContainer(parent: Container) {
    val scrollPane = parent as? JScrollPane ?: return
    val availR = scrollPane.getBounds()
    availR.setLocation(0, 0) // availR.x = availR.y = 0;

    val insets = parent.getInsets()
    availR.x = insets.left
    availR.y = insets.top
    availR.width -= insets.left + insets.right
    availR.height -= insets.top + insets.bottom

    val vsbR = Rectangle()
    vsbR.width = 12
    vsbR.height = availR.height
    vsbR.x = availR.x + availR.width - vsbR.width
    vsbR.y = availR.y

    viewport?.setBounds(availR)
    vsb?.also {
      it.setVisible(true)
      it.setBounds(vsbR)
    }
  }
}

internal class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

internal class TranslucentScrollBarUI : BasicScrollBarUI() {
  protected override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  protected override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  protected override fun paintTrack(g: Graphics, c: JComponent?, r: Rectangle) {
    // Graphics2D g2 = (Graphics2D) g.create();
    // g2.setPaint(new Color(100, 100, 100, 100));
    // g2.fillRect(r.x, r.y, r.width - 1, r.height - 1);
    // g2.dispose();
  }

  protected override fun paintThumb(g: Graphics, c: JComponent?, r: Rectangle) {
    val sb = c as? JScrollBar ?: return
    if (!sb.isEnabled() || r.width > r.height) {
      return
    }
    val color = when {
      isDragging -> DRAGGING_COLOR
      isThumbRollover() -> ROLLOVER_COLOR
      else -> DEFAULT_COLOR
    }
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setPaint(color)
    g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
    g2.setPaint(Color.WHITE)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
