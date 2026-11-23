package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.Border

fun createUI(): Component {
  val list = JList(createModel())
  list.cellRenderer = SimpleListItemCellRenderer()

  return JPanel(GridLayout(1, 2)).also {
    it.add(JScrollPane(list))
    it.add(JScrollPane(MultipleSelectionList(createModel())))
    it.isOpaque = false
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createModel(): DefaultListModel<ListItem> {
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
  return model
}

private class MultipleSelectionList(
  model: ListModel<ListItem>,
) : JList<ListItem>(model) {
  private var checkListener: ItemCheckBoxesListener? = null
  private var rolloverIndex = -1
  private var checkIndex = -1 // index of the cell whose check box is pressed

  override fun updateUI() {
    selectionForeground = null // Nimbus
    selectionBackground = null // Nimbus
    cellRenderer = null
    removeMouseListener(checkListener)
    removeMouseMotionListener(checkListener)
    super.updateUI()
    setCellRenderer(ListItemCellRenderer())
    checkListener = ItemCheckBoxesListener()
    addMouseMotionListener(checkListener)
    addMouseListener(checkListener)
  }

  override fun processMouseEvent(e: MouseEvent) {
    // The MouseListener of the ListUI is notified before ItemCheckBoxesListener
    // and calls setSelectionInterval(...), so the index of the pressed check box
    // must be updated before any MouseListener is notified.
    if (e.id == MouseEvent.MOUSE_PRESSED) {
      checkIndex = getCheckBoxIndexAt(e)
    }
    super.processMouseEvent(e)
  }

  override fun setSelectionInterval(
    anchor: Int,
    lead: Int,
  ) {
    if (checkIndex < 0) {
      super.setSelectionInterval(anchor, lead)
    } else {
      // Defer the selection change requested by the ListUI so that it is
      // applied after the check box toggles the selection of the cell
      EventQueue.invokeLater {
        val idx = checkIndex
        if (idx >= 0 && lead == anchor && idx == anchor) {
          super.addSelectionInterval(idx, idx)
        } else {
          super.setSelectionInterval(anchor, lead)
        }
      }
    }
  }

  override fun removeSelectionInterval(
    index0: Int,
    index1: Int,
  ) {
    if (checkIndex < 0) {
      super.removeSelectionInterval(index0, index1)
    } else {
      EventQueue.invokeLater { super.removeSelectionInterval(index0, index1) }
    }
  }

  fun getCellIndexAt(pt: Point): Int {
    val index = locationToIndex(pt)
    val r = getCellBounds(index, index)
    return if (r != null && r.contains(pt)) index else -1
  }

  fun repaintCell(index: Int) {
    getCellBounds(index, index)?.also { repaint(it) }
  }

  private fun getCheckBoxIndexAt(e: MouseEvent): Int {
    val modifierDown = e.isShiftDown || e.isControlDown || e.isAltDown
    val pt = e.point
    val index = getCellIndexAt(pt)
    val hit = !modifierDown && index >= 0 && isCheckBoxAt(index, pt)
    return if (hit) index else -1
  }

  private fun isCheckBoxAt(
    index: Int,
    pt: Point,
  ): Boolean {
    val value = model.getElementAt(index)
    val c = cellRenderer.getListCellRendererComponent(this, value, index, false, false)
    val r = getCellBounds(index, index)
    c.bounds = r
    c.validate() // lay out the JCheckBox or Box.Filler swapped by the cell renderer
    val deepest = SwingUtilities.getDeepestComponentAt(c, pt.x - r.x, pt.y - r.y)
    // The cell renderer uses a Box.Filler instead of a hidden JCheckBox
    return deepest is JCheckBox || deepest is Box.Filler
  }

  private inner class ItemCheckBoxesListener : MouseAdapter() {
    override fun mouseExited(e: MouseEvent) {
      repaintCell(rolloverIndex)
      rolloverIndex = -1
    }

    override fun mouseMoved(e: MouseEvent) {
      val index = getCellIndexAt(e.point)
      if (index != rolloverIndex) {
        repaintCell(rolloverIndex)
        rolloverIndex = index
        repaintCell(index)
      }
    }

    override fun mousePressed(e: MouseEvent) {
      val index = getCellIndexAt(e.point)
      if (index >= 0) {
        cellPressed(e, index)
      } else {
        EventQueue.invokeLater {
          selectionModel.anchorSelectionIndex = -1
          selectionModel.leadSelectionIndex = -1
          rolloverIndex = -1
          checkIndex = -1
          clearSelection()
        }
      }
      repaint()
    }

    private fun cellPressed(
      e: MouseEvent,
      index: Int,
    ) {
      if (SwingUtilities.isLeftMouseButton(e) && e.clickCount > 1) {
        val item = model.getElementAt(index)
        JOptionPane.showMessageDialog(rootPane, item.title)
      } else if (checkIndex == index) {
        if (isSelectedIndex(index)) {
          removeSelectionInterval(index, index)
        } else {
          setSelectionInterval(index, index)
        }
      }
    }
  }

  private inner class ListItemCellRenderer : ListCellRenderer<ListItem> {
    private val renderer = JPanel(BorderLayout(0, 0))
    private val itemLayout = BorderLayout(2, 2)
    private val check = JCheckBox()
    private val filler = Box.createRigidArea(check.getPreferredSize())
    private val label = JLabel("")
    private val itemPanel = object : JPanel(itemLayout) {
      override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        // Paint the translucent selection color once more to make
        // the selected cell darker than the rollover cell
        if (SELECTED_COLOR == getBackground()) {
          val g2 = g.create() as? Graphics2D ?: return
          g2.paint = SELECTED_COLOR
          g2.fillRect(0, 0, getWidth(), getHeight())
          g2.dispose()
        }
      }
    }
    val focusBorder: Border = UIManager.getBorder(
      "List.focusCellHighlightBorder",
    )
    val noFocusBorder = getNoFocusBorder(focusBorder)

    init {
      itemPanel.setBorder(noFocusBorder)
      label.setOpaque(false)
      check.setOpaque(false)
      itemPanel.add(filler, BorderLayout.WEST)
      itemPanel.add(label)
      itemPanel.setOpaque(true)
      renderer.add(itemPanel)
      renderer.setOpaque(false)
    }

    private fun getNoFocusBorder(focusBorder: Border): Border {
      val b = UIManager.getBorder("List.noFocusBorder")
      return b ?: focusBorder.getBorderInsets(renderer).let {
        BorderFactory.createEmptyBorder(it.top, it.left, it.bottom, it.right)
      }
    }

    override fun getListCellRendererComponent(
      list: JList<out ListItem>,
      value: ListItem?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean,
    ): Component {
      label.text = value?.title
      label.icon = value?.icon
      val rollover = index == rolloverIndex
      itemPanel.border = if (cellHasFocus) focusBorder else noFocusBorder
      check.isSelected = isSelected
      check.model.isRollover = rollover
      // Show the JCheckBox only in the selected or rollover cell, otherwise
      // use a Box.Filler of the same size to keep the position of the label
      setLeadingComponent(if (isSelected || rollover) check else filler)
      label.foreground = if (isSelected) list.selectionForeground else list.foreground
      itemPanel.background = when {
        isSelected -> SELECTED_COLOR
        rollover -> ROLLOVER_COLOR
        else -> list.background
      }
      return renderer
    }

    private fun setLeadingComponent(c: Component) {
      val old = itemLayout.getLayoutComponent(BorderLayout.WEST)
      if (old != c) {
        old?.also { itemPanel.remove(it) }
        itemPanel.add(c, BorderLayout.WEST)
      }
    }
  }

  companion object {
    val SELECTED_COLOR = Color(0x40_32_64_FF, true)
    val ROLLOVER_COLOR = Color(0x40_32_64_AA, true)
  }
}

private class SimpleListItemCellRenderer : ListCellRenderer<ListItem> {
  private val renderer = JLabel("")
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = getNoFocusBorder(focusBorder)

  init {
    renderer.setBorder(noFocusBorder)
    renderer.setOpaque(true)
  }

  private fun getNoFocusBorder(focusBorder: Border): Border {
    val b = UIManager.getBorder("List.noFocusBorder")
    return b ?: focusBorder.getBorderInsets(renderer).let {
      BorderFactory.createEmptyBorder(it.top, it.left, it.bottom, it.right)
    }
  }

  override fun getListCellRendererComponent(
    list: JList<out ListItem>,
    value: ListItem,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    renderer.setText(value.title)
    renderer.setIcon(value.icon)
    renderer.setBorder(if (cellHasFocus) focusBorder else noFocusBorder)
    if (isSelected) {
      renderer.setForeground(list.selectionForeground)
      renderer.setBackground(list.selectionBackground)
    } else {
      renderer.setForeground(list.foreground)
      renderer.setBackground(list.background)
    }
    return renderer
  }
}

private data class ListItem(
  val title: String,
  val icon: Icon,
)

private class ColorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
