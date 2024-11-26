package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.text.Position

fun makeUI() = JPanel(GridLayout(1, 2)).also {
  UIManager.put("ScrollBar.minimumThumbSize", Dimension(12, 20))
  UIManager.put("List.lockToPositionOnScroll", false)
  val model = makeModel()
  it.add(makeScrollPane(makeList(model)))
  it.add(JLayer(makeScrollPane(makeList(model)), StickyLayerUI()))
  it.preferredSize = Dimension(320, 240)
}

private fun makeModel(): ListModel<String> {
  val model = DefaultListModel<String>()
  for (i in 0..99) {
    val indent = if (i % 10 == 0) "" else "    "
    val now = LocalDateTime.now(ZoneId.systemDefault())
    model.addElement(String.format("%s%04d: %s", indent, i, now))
  }
  return model
}

private fun makeList(m: ListModel<String>): Component {
  val list = JList(m)
  list.fixedCellHeight = 32
  return list
}

private fun makeScrollPane(c: Component) = object : JScrollPane(c) {
  override fun updateUI() {
    super.updateUI()
    setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS)
    setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
    getVerticalScrollBar().unitIncrement = 4
  }
}

private class StickyLayerUI : LayerUI<JScrollPane>() {
  private val renderer = JPanel()
  private var currentHeaderIdx = -1
  private var nextHeaderIdx = -1

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

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    super.processMouseMotionEvent(e, l)
    val c = l.view.viewport.view
    if (e.id == MouseEvent.MOUSE_DRAGGED && c is JList<*>) {
      update(c)
    }
  }

  override fun processMouseWheelEvent(e: MouseWheelEvent, l: JLayer<out JScrollPane>) {
    super.processMouseWheelEvent(e, l)
    val c = l.view.viewport.view
    if (c is JList<*>) {
      update(c)
    }
  }

  private fun update(list: JList<*>) {
    val idx = list.firstVisibleIndex
    if (idx >= 0) {
      currentHeaderIdx = getHeaderIndex1(list, idx)
      nextHeaderIdx = getNextHeaderIndex1(list, idx)
    } else {
      currentHeaderIdx = -1
      nextHeaderIdx = -1
    }
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val list = getList(c)
    if (list != null && currentHeaderIdx >= 0) {
      val scroll = (c as? JLayer<*>)?.view as? JScrollPane
      val headerRect = scroll?.viewport?.bounds ?: return
      headerRect.height = list.fixedCellHeight
      val g2 = g.create() as? Graphics2D ?: return
      val firstVisibleIdx = list.firstVisibleIndex
      if (firstVisibleIdx + 1 == nextHeaderIdx) {
        val d = headerRect.size
        val c1 = getComponent(list, currentHeaderIdx)
        val r1 = getHeaderRect(list, firstVisibleIdx, c, d)
        SwingUtilities.paintComponent(g2, c1, renderer, r1)
        val c2 = getComponent(list, nextHeaderIdx)
        val r2 = getHeaderRect(list, nextHeaderIdx, c, d)
        SwingUtilities.paintComponent(g2, c2, renderer, r2)
      } else {
        val c1 = getComponent(list, currentHeaderIdx)
        SwingUtilities.paintComponent(g2, c1, renderer, headerRect)
      }
      g2.dispose()
    }
  }

  companion object {
    private fun getList(layer: JComponent): JList<*>? {
      var list: JList<*>? = null
      if (layer is JLayer<*>) {
        val view = (layer.view as? JScrollPane)?.viewport?.view
        if (view is JList<*>) {
          list = view
        }
      }
      return list
    }
  }
}

private fun getHeaderIndex1(list: JList<*>, start: Int) =
  list.getNextMatch("0", start, Position.Bias.Backward)

private fun getNextHeaderIndex1(list: JList<*>, start: Int) =
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
