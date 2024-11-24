package example

import java.awt.*
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.text.Position

fun makeUI() = JPanel(GridLayout(1, 2)).also {
  UIManager.put("ScrollBar.minimumThumbSize", Dimension(12, 20))
  UIManager.put("List.lockToPositionOnScroll", false)
  it.add(JScrollPane(makeList()))
  it.add(makeStickyHeaderScrollPane(makeList()))
  it.preferredSize = Dimension(320, 240)
}

private fun makeList(): JList<String> {
  val m = DefaultListModel<String>()
  for (i in 0..99) {
    val indent = if (i % 10 == 0) "" else "    "
    val now = LocalDateTime.now(ZoneId.systemDefault())
    m.addElement("%s%04d: %s".format(indent, i, now))
  }
  val list = JList(m)
  list.fixedCellHeight = 32
  return list
}

private fun makeStickyHeaderScrollPane(c: JList<String>): Component {
  val scroll = object : JScrollPane(c) {
    override fun updateUI() {
      super.updateUI()
      setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS)
      setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
      getVerticalScrollBar().unitIncrement = 2
    }
  }
  return JLayer(scroll, StickyLayerUI(c))
}

private class StickyLayerUI(
  val list: JList<String>,
) : LayerUI<JScrollPane>() {
  private val panel = JPanel()

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask =
        AWTEvent.MOUSE_WHEEL_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    if (c is JLayer<*>) {
      c.layerEventMask = 0
    }
    super.uninstallUI(c)
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (c is JLayer<*>) {
      val scroll = c.view as? JScrollPane
      val viewport = scroll?.viewport ?: return
      val cellHeight = list.fixedCellHeight
      val viewRect = viewport.viewRect
      val vp = SwingUtilities.convertPoint(viewport, 0, 0, c)
      val pt1 = SwingUtilities.convertPoint(c, vp, list)
      val idx1 = list.locationToIndex(pt1)
      val header1 = Rectangle(vp.x, vp.y, viewRect.width, cellHeight)
      if (idx1 >= 0) {
        val g2 = g.create() as? Graphics2D ?: return
        val headerIndex1 = getHeaderIndex1(list, idx1)
        val c1 = getComponent(list, headerIndex1)
        val nhi = getNextHeaderIndex1(list, idx1)
        val nextPt = list.getCellBounds(nhi, nhi).location
        if (header1.contains(SwingUtilities.convertPoint(list, nextPt, c))) {
          val d = header1.size
          SwingUtilities.paintComponent(g2, c1, panel, getHeaderRect(list, idx1, c, d))
          val cn = getComponent(list, nhi)
          SwingUtilities.paintComponent(g2, cn, panel, getHeaderRect(list, nhi, c, d))
        } else {
          SwingUtilities.paintComponent(g2, c1, panel, header1)
        }
        g2.dispose()
      }
    }
  }
}

private fun getHeaderIndex1(list: JList<String>, start: Int) =
  list.getNextMatch("0", start, Position.Bias.Backward)

private fun getNextHeaderIndex1(list: JList<String>, start: Int) =
  list.getNextMatch("0", start, Position.Bias.Forward)

private fun getHeaderRect(
  list: JList<*>,
  i: Int,
  dst: Component,
  d: Dimension,
): Rectangle {
  val r = SwingUtilities.convertRectangle(list, list.getCellBounds(i, i), dst)
  r.size = d
  return r
}

private fun <E> getComponent(list: JList<E>, idx: Int): Component {
  val value = list.model.getElementAt(idx)
  val renderer = list.cellRenderer
  val c = renderer.getListCellRendererComponent(list, value, idx, false, false)
  c.background = Color.GRAY
  c.foreground = Color.WHITE
  return c
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
