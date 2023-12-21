package example

import java.awt.*
import java.awt.event.MouseEvent
import java.time.LocalTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val tabbedPane0 = CloseableTabbedPane()
  val tabbedPane1 = JTabbedPane()
  listOf(tabbedPane0, tabbedPane1).forEach {
    it.addTab("1111111111111111111111111", JLabel("111"))
    it.addTab("22222222222222222", JLabel("222"))
    it.addTab("333", JLabel("333"))
    it.addTab("4", JLabel("444"))
  }

  val addTabButton = JButton("add tab")
  addTabButton.addActionListener {
    val title = LocalTime.now(ZoneId.systemDefault()).toString()
    listOf(tabbedPane0, tabbedPane1)
      .forEach { it.addTab(title, JLabel(title)) }
  }

  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.topComponent = tabbedPane0
  sp.bottomComponent = JLayer(tabbedPane1, CloseableTabbedPaneLayerUI())

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.add(addTabButton, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class CloseTabIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    if (c is AbstractButton && c.model.isRollover) {
      g2.paint = Color.ORANGE
    } else {
      g2.paint = Color.BLACK
    }
    g2.drawLine(4, 4, 11, 11)
    g2.drawLine(4, 5, 10, 11)
    g2.drawLine(5, 4, 11, 10)
    g2.drawLine(11, 4, 4, 11)
    g2.drawLine(11, 5, 5, 11)
    g2.drawLine(10, 4, 4, 10)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
}

private class CloseableTabbedPane : JTabbedPane() {
  override fun addTab(
    title: String?,
    content: Component?,
  ) {
    val tab = JPanel(BorderLayout())
    tab.isOpaque = false
    val label = JLabel(title)
    label.border = BorderFactory.createEmptyBorder(0, 0, 0, 4)
    val button = JButton(CLOSE_ICON)
    button.border = BorderFactory.createEmptyBorder()
    button.isContentAreaFilled = false
    button.addActionListener { removeTabAt(indexOfComponent(content)) }
    tab.add(label, BorderLayout.WEST)
    tab.add(button, BorderLayout.EAST)
    tab.border = BorderFactory.createEmptyBorder(2, 1, 1, 1)
    super.addTab(title, content)
    setTabComponentAt(tabCount - 1, tab)
  }

  companion object {
    private val CLOSE_ICON = CloseTabIcon()
  }
}

private class CloseableTabbedPaneLayerUI : LayerUI<JTabbedPane>() {
  private val rubberStamp = JPanel()
  private val pt = Point()
  private val button = object : JButton(CloseTabIcon()) {
    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createEmptyBorder()
      isFocusPainted = false
      isBorderPainted = false
      isContentAreaFilled = false
      isRolloverEnabled = false
    }
  }
  private val dim = button.preferredSize
  private val repaintRect = Rectangle(dim.width * 2, dim.height * 2)

  private fun getTabButtonRect(
    tabbedPane: JTabbedPane,
    index: Int,
  ): Rectangle {
    val r = tabbedPane.getBoundsAt(index)
    r.translate(r.width - dim.width - GAP, (r.height - dim.height) / 2)
    r.size = dim
    return r
  }

  override fun updateUI(l: JLayer<out JTabbedPane>) {
    super.updateUI(l)
    SwingUtilities.updateComponentTreeUI(button)
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    val tabbedPane = (c as? JLayer<*>)?.view as? JTabbedPane ?: return
    for (i in 0 until tabbedPane.tabCount) {
      val r = getTabButtonRect(tabbedPane, i)
      button.model.isRollover = r.contains(pt)
      SwingUtilities.paintComponent(g, button, rubberStamp, r)
    }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask =
      AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(
    e: MouseEvent,
    l: JLayer<out JTabbedPane>,
  ) {
    if (e.id == MouseEvent.MOUSE_CLICKED) {
      pt.location = e.point
      val tabbedPane = l.view
      val index = tabbedPane.indexAtLocation(pt.x, pt.y)
      if (index >= 0 && getTabButtonRect(tabbedPane, index).contains(pt)) {
        tabbedPane.removeTabAt(index)
      }
    }
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out JTabbedPane>,
  ) {
    val loc = e.point
    pt.location = loc
    if (l.view.indexAtLocation(pt.x, pt.y) >= 0) {
      loc.translate(-dim.width, -dim.height)
      repaintRect.location = loc
      l.repaint(repaintRect)
    }
  }

  companion object {
    const val GAP = 2
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      val g = CloseableTabbedPaneLayerUI.GAP
      UIManager.put("TabbedPane.tabInsets", Insets(g, 16 + g, g, 16 + g))
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
