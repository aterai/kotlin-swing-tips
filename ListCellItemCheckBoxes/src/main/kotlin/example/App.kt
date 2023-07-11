package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.border.Border

fun makeUI(): Component {
  val model = DefaultListModel<ListItem>().also {
    it.addElement(ListItem("red", ColorIcon(Color.RED)))
    it.addElement(ListItem("green", ColorIcon(Color.GREEN)))
    it.addElement(ListItem("blue", ColorIcon(Color.BLUE)))
    it.addElement(ListItem("cyan", ColorIcon(Color.CYAN)))
    it.addElement(ListItem("darkGray", ColorIcon(Color.DARK_GRAY)))
    it.addElement(ListItem("gray", ColorIcon(Color.GRAY)))
    it.addElement(ListItem("lightGray", ColorIcon(Color.LIGHT_GRAY)))
    it.addElement(ListItem("magenta", ColorIcon(Color.MAGENTA)))
    it.addElement(ListItem("orange", ColorIcon(Color.ORANGE)))
    it.addElement(ListItem("pink", ColorIcon(Color.PINK)))
    it.addElement(ListItem("yellow", ColorIcon(Color.YELLOW)))
    it.addElement(ListItem("black", ColorIcon(Color.BLACK)))
    it.addElement(ListItem("white", ColorIcon(Color.WHITE)))
  }

  val list = RubberBandSelectionList(model)
  list.prototypeCellValue = ListItem("red", ColorIcon(Color.RED))
  list.isOpaque = false
  list.background = Color(0x0, true)

  val popup = JPopupMenu("JList JPopupMenu")
  popup.add("info").addActionListener {
    val msg = list.selectedValuesList
      .map { it.title }
      .joinToString(", ")
    JOptionPane.showMessageDialog(list.rootPane, msg)
  }
  popup.addSeparator()
  popup.add("JMenuItem 1")
  popup.add("JMenuItem 2")
  list.componentPopupMenu = popup

  val scroll = JScrollPane(list)
  scroll.background = Color(0x0, true)
  scroll.isOpaque = false
  scroll.border = BorderFactory.createEmptyBorder()
  scroll.viewportBorder = BorderFactory.createEmptyBorder()
  scroll.viewport.isOpaque = false

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.isOpaque = false
    it.preferredSize = Dimension(320, 240)
  }
}

private class RubberBandSelectionList(model: ListModel<ListItem>) : JList<ListItem>(model) {
  private var rbl: ItemCheckBoxesListener? = null
  private var rubberBandColor: Color? = null
  private val rubberBand = Path2D.Double()
  private var rollOverRowIndex = -1
  private var checkedIndex = -1
  override fun updateUI() {
    selectionForeground = null // Nimbus
    selectionBackground = null // Nimbus
    cellRenderer = null
    removeMouseListener(rbl)
    removeMouseMotionListener(rbl)
    super.updateUI()
    rubberBandColor = makeRubberBandColor(selectionBackground)
    layoutOrientation = HORIZONTAL_WRAP
    visibleRowCount = 0
    fixedCellWidth = 80
    fixedCellHeight = 60
    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    setCellRenderer(ListItemCellRenderer())
    rbl = ItemCheckBoxesListener()
    addMouseMotionListener(rbl)
    addMouseListener(rbl)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = selectionBackground
    g2.draw(rubberBand)
    g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .1f)
    g2.paint = rubberBandColor
    g2.fill(rubberBand)
    g2.dispose()
  }

  override fun setSelectionInterval(anchor: Int, lead: Int) {
    if (checkedIndex < 0 && !rubberBand.bounds.isEmpty) {
      super.setSelectionInterval(anchor, lead)
    } else {
      EventQueue.invokeLater {
        if (checkedIndex >= 0 && lead == anchor && checkedIndex == anchor) {
          super.addSelectionInterval(checkedIndex, checkedIndex)
        } else {
          super.setSelectionInterval(anchor, lead)
        }
      }
    }
  }

  override fun removeSelectionInterval(index0: Int, index1: Int) {
    if (checkedIndex < 0) {
      super.removeSelectionInterval(index0, index1)
    } else {
      EventQueue.invokeLater { super.removeSelectionInterval(index0, index1) }
    }
  }

  private inner class ItemCheckBoxesListener : MouseAdapter() {
    private val srcPoint = Point()
    override fun mouseDragged(e: MouseEvent) {
      checkedIndex = -1
      val l = e.component as? JList<*> ?: return
      l.isFocusable = true
      val destPoint = e.point
      rubberBand.reset()
      rubberBand.moveTo(srcPoint.getX(), srcPoint.getY())
      rubberBand.lineTo(destPoint.getX(), srcPoint.getY())
      rubberBand.lineTo(destPoint.getX(), destPoint.getY())
      rubberBand.lineTo(srcPoint.getX(), destPoint.getY())
      rubberBand.closePath()
      val indices = (0 until l.model.size)
        .filter { rubberBand.intersects(l.getCellBounds(it, it)) }
        .toIntArray()
      l.selectedIndices = indices
      l.repaint()
    }

    override fun mouseExited(e: MouseEvent) {
      rollOverRowIndex = -1
      e.component.repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
      val row = locationToIndex(e.point)
      if (row != rollOverRowIndex) {
        val rect = getCellBounds(row, row)
        if (rollOverRowIndex >= 0) {
          rect.add(getCellBounds(rollOverRowIndex, rollOverRowIndex))
        }
        rollOverRowIndex = row
        (e.component as? JComponent)?.repaint(rect)
      }
    }

    override fun mouseReleased(e: MouseEvent) {
      rubberBand.reset()
      val c = e.component
      c.isFocusable = true
      c.repaint()
    }

    override fun mousePressed(e: MouseEvent) {
      val l = e.component as? JList<*> ?: return
      val index = l.locationToIndex(e.point)
      if (l.getCellBounds(index, index).contains(e.point)) {
        l.isFocusable = true
        cellPressed(l, e, index)
      } else {
        l.isFocusable = false
        l.clearSelection()
        l.selectionModel.anchorSelectionIndex = -1
        l.selectionModel.leadSelectionIndex = -1
      }
      srcPoint.location = e.point
      l.repaint()
    }

    private fun cellPressed(l: JList<*>, e: MouseEvent, index: Int) {
      if (e.button == MouseEvent.BUTTON1 && e.clickCount > 1) {
        val item: ListItem = model.getElementAt(index)
        JOptionPane.showMessageDialog(l.rootPane, item.title)
      } else {
        checkedIndex = -1
        getItemCheckBox(l, e, index)?.also {
          checkedIndex = index
          if (l.isSelectedIndex(index)) {
            l.isFocusable = false
            removeSelectionInterval(index, index)
          } else {
            setSelectionInterval(index, index)
          }
        }
      }
    }
  }

  private inner class ListItemCellRenderer : ListCellRenderer<ListItem> {
    private val renderer = JPanel(BorderLayout(0, 0))
    private val check = JCheckBox()
    private val icon = JLabel("", null, SwingConstants.CENTER)
    private val label = JLabel("", SwingConstants.CENTER)
    private val itemPanel = object : JPanel(BorderLayout(2, 2)) {
      override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (SELECTED_COLOR == background) {
          val g2 = g.create() as? Graphics2D ?: return
          g2.paint = SELECTED_COLOR
          g2.fillRect(0, 0, width, height)
          g2.dispose()
        }
      }
    }
    val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
    val noFocusBorder = UIManager.getBorder("List.noFocusBorder") ?: getNimbusNoFocusBorder()

    init {
      itemPanel.border = noFocusBorder
      label.verticalTextPosition = SwingConstants.TOP
      label.horizontalTextPosition = SwingConstants.CENTER
      label.foreground = itemPanel.foreground
      label.background = itemPanel.background
      label.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
      label.isOpaque = false
      icon.horizontalTextPosition = SwingConstants.CENTER
      icon.border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
      icon.isOpaque = false
      check.isOpaque = false
      check.isVisible = false
      val d = check.preferredSize
      val p = JPanel(BorderLayout(0, 0))
      p.isOpaque = false
      p.add(check, BorderLayout.NORTH)
      p.add(Box.createHorizontalStrut(d.width), BorderLayout.SOUTH)
      itemPanel.add(p, BorderLayout.EAST)
      itemPanel.add(Box.createHorizontalStrut(d.width), BorderLayout.WEST)
      itemPanel.add(icon)
      itemPanel.add(label, BorderLayout.SOUTH)
      itemPanel.isOpaque = true
      renderer.add(itemPanel)
      renderer.isOpaque = false
      renderer.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    }

    private fun getNimbusNoFocusBorder(): Border {
      val i = focusBorder.getBorderInsets(label)
      return BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right)
    }

    override fun getListCellRendererComponent(
      list: JList<out ListItem>,
      value: ListItem,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      label.text = value.title
      itemPanel.border = if (cellHasFocus) focusBorder else noFocusBorder
      icon.icon = value.icon
      check.isSelected = isSelected
      check.model.isRollover = index == rollOverRowIndex
      if (isSelected) {
        label.foreground = list.selectionForeground
        label.background = SELECTED_COLOR
        itemPanel.background = SELECTED_COLOR
        check.isVisible = true
      } else if (index == rollOverRowIndex) {
        itemPanel.background = ROLLOVER_COLOR
        check.isVisible = true
      } else {
        label.foreground = list.foreground
        label.background = list.background
        itemPanel.background = list.background
        check.isVisible = false
      }
      return renderer
    }
  }

  private fun <E> getItemCheckBox(list: JList<E>, e: MouseEvent, index: Int): AbstractButton? {
    if (e.isShiftDown || e.isControlDown || e.isAltDown) {
      return null
    }
    val proto = list.prototypeCellValue
    val cr = list.cellRenderer
    val c = cr.getListCellRendererComponent(list, proto, index, false, false)
    val r = list.getCellBounds(index, index)
    c.bounds = r
    val pt = e.point
    pt.translate(-r.x, -r.y)
    return SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y) as? AbstractButton
  }

  private fun makeRubberBandColor(c: Color): Color {
    val r = c.red
    val g = c.green
    val b = c.blue
    val v = when (val max = maxOf(r, g, b)) {
      r -> max shl 8
      g -> max shl 4
      else -> max
    }
    return Color(v)
  }

  companion object {
    val SELECTED_COLOR = Color(0x40_32_64_FF, true)
    val ROLLOVER_COLOR = Color(0x40_32_64_AA, true)
  }
}

private data class ListItem(val title: String, val icon: Icon)

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 32
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
