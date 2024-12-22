package example

import java.awt.*
import java.awt.event.MouseEvent
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val model = makeModel()
  return JPanel(GridLayout(1, 2)).also {
    it.add(makeScrollPane(makeList(model)))
    it.add(JLayer(makeScrollPane(makeList(model)), RolloverLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): ListModel<String> {
  val model = DefaultListModel<String>()
  for (i in 0..99) {
    val indent = if (i % 10 == 0) "" else "    "
    val now = LocalDateTime.now(ZoneId.systemDefault())
    model.addElement("%s%04d: %s".format(indent, i, now))
  }
  return model
}

private fun <E> makeList(m: ListModel<E>): JList<E> {
  val list = object : JList<E>(m) {
    override fun getToolTipText(e: MouseEvent): String? {
      val idx = locationToIndex(e.point)
      val value = model.getElementAt(idx)
      return value?.toString() ?: super.getToolTipText(e)
    }
  }
  val renderer = list.cellRenderer
  list.setCellRenderer { lst, value, index, isSelected, _ ->
    renderer.getListCellRendererComponent(
      lst,
      value,
      index,
      isSelected,
      false,
    )
  }
  list.componentPopupMenu = makePopupMenu()
  return list
}

private fun makePopupMenu(): JPopupMenu {
  val popup = JPopupMenu()
  popup.add("JMenuItem 1")
  popup.add("JMenuItem 2")
  popup.add("JMenuItem 3")
  popup.addSeparator()
  popup.add(JCheckBoxMenuItem("JCheckBoxMenuItem"))
  popup.add(JRadioButtonMenuItem("JRadioButtonMenuItem"))
  return popup
}

private fun makeScrollPane(c: JComponent) = object : JScrollPane(c) {
  override fun updateUI() {
    super.updateUI()
    setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS)
    setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
  }
}

private class RolloverLayerUI : LayerUI<JScrollPane>() {
  private val renderer = JPanel()
  private var rolloverIdx = -1
  private val loc = Point(-100, -100)
  private val button = object : JButton(ThreeDotsIcon()) {
    override fun updateUI() {
      super.updateUI()
      isBorderPainted = false
      isContentAreaFilled = false
      isFocusPainted = false
      isFocusable = false
      isOpaque = false
      border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
    }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask =
        AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    if (c is JLayer<*>) {
      c.layerEventMask = 0
    }
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    super.processMouseEvent(e, l)
    val c = e.component as? JList<*> ?: return
    val id = e.id
    if (id == MouseEvent.MOUSE_CLICKED && SwingUtilities.isLeftMouseButton(e)) {
      val r = c.getCellBounds(rolloverIdx, rolloverIdx)
      val d = button.preferredSize
      r.width = l.view.viewportBorderBounds.width - d.width
      val popup = (c as? JComponent)?.componentPopupMenu
      val pt = e.point
      if (popup != null && !r.contains(pt)) {
        popup.show(c, pt.x, pt.y)
      }
    } else if (id == MouseEvent.MOUSE_EXITED) {
      c.repaint(c.getCellBounds(rolloverIdx, rolloverIdx))
      rolloverIdx = -1
      loc.setLocation(-100, -100)
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    super.processMouseMotionEvent(e, l)
    val c = e.component
    if (e.id == MouseEvent.MOUSE_MOVED && c is JList<*>) {
      val pt = e.point
      rolloverIdx = c.locationToIndex(pt)
      button.doLayout()
      val r = c.getCellBounds(rolloverIdx, rolloverIdx)
      r.width = l.view.viewportBorderBounds.width
      r.grow(0, r.height)
      loc.location = pt
      c.repaint(r)
    }
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val g2 = g.create() as? Graphics2D ?: return
    val list = getList(c)
    if (list != null && rolloverIdx >= 0) {
      val r = list.getCellBounds(rolloverIdx, rolloverIdx)
      val cc = getRendererComponent(list, rolloverIdx)
      val d = button.preferredSize
      r.width = SwingUtilities.getUnwrappedParent(list).width - d.width
      val buttonRollover = !r.contains(loc)
      button.model.isRollover = buttonRollover
      val rr = SwingUtilities.convertRectangle(list, r, c)
      SwingUtilities.paintComponent(g2, cc, renderer, rr)
      rr.x += rr.width
      rr.width = d.width
      g2.paint = cc.background
      g2.fill(rr)
      SwingUtilities.paintComponent(g2, button, renderer, rr)
    }
    g2.dispose()
  }

  companion object {
    private fun getList(c: JComponent): JList<*>? {
      val layer = c as? JLayer<*>
      val scroll = layer?.view as? JScrollPane
      return scroll?.viewport?.view as? JList<*>
    }

    private fun <E> getRendererComponent(list: JList<E>, idx: Int): Component {
      val value = list.model.getElementAt(idx)
      val r = list.cellRenderer
      val isSelected = list.isSelectedIndex(idx)
      val cellHasFocus = list.selectionModel.leadSelectionIndex == idx
      val c = r.getListCellRendererComponent(list, value, idx, isSelected, cellHasFocus)
      if (!isSelected) {
        c.background = Color.GRAY
        c.foreground = Color.WHITE
      }
      return c
    }
  }
}

private class ThreeDotsIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    if (c is JButton) {
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      val r = SwingUtilities.calculateInnerArea(c, null)
      val rollover = c.model.isRollover
      if (rollover) {
        g2.paint = Color.DARK_GRAY
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 2, 2)
      }
      g2.translate(x, y)
      g2.paint = if (rollover) Color.WHITE else Color.LIGHT_GRAY
      val count = 3
      val diff = 4
      val firstColumn = (r.width - count * diff) / 2
      val firstRow = iconHeight / 2
      for (i in 0..<count) {
        val column = firstColumn + i * diff
        g2.fillRect(column, firstRow, 2, 2)
      }
    }
    g2.dispose()
  }

  override fun getIconWidth(): Int = 20

  override fun getIconHeight(): Int = 12
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
