package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.Border

fun makeUI(): Component {
  val list = JList(makeModel())
  list.cellRenderer = SimpleListItemCellRenderer()

  return JPanel(GridLayout(1, 2)).also {
    it.add(JScrollPane(list))
    it.add(JScrollPane(MultipleSelectionList(makeModel())))
    it.isOpaque = false
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): DefaultListModel<ListItem> {
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
  private var rollOverIndex = -1
  private var checkedIndex = -1

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

  override fun setSelectionInterval(
    anchor: Int,
    lead: Int,
  ) {
    if (checkedIndex < 0) {
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

  override fun removeSelectionInterval(
    index0: Int,
    index1: Int,
  ) {
    if (checkedIndex < 0) {
      super.removeSelectionInterval(index0, index1)
    } else {
      EventQueue.invokeLater { super.removeSelectionInterval(index0, index1) }
    }
  }

  private inner class ItemCheckBoxesListener : MouseAdapter() {
    private val srcPoint = Point()

    override fun mouseExited(e: MouseEvent) {
      rollOverIndex = -1
      e.component.repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
      val pt = e.getPoint()
      var idx = locationToIndex(pt)
      if (!getCellBounds(idx, idx).contains(pt)) {
        idx = -1
      }
      val rect = Rectangle()
      if (idx >= 0) {
        rect.add(getCellBounds(idx, idx))
        if (rollOverIndex >= 0 && idx != rollOverIndex) {
          rect.add(getCellBounds(rollOverIndex, rollOverIndex))
        }
        rollOverIndex = idx
      } else {
        if (rollOverIndex >= 0) {
          rect.add(getCellBounds(rollOverIndex, rollOverIndex))
        }
        rollOverIndex = -1
      }
      (e.component as? JComponent)?.repaint(rect)
    }

    override fun mousePressed(e: MouseEvent) {
      val l = e.component as? JList<*> ?: return
      val index = l.locationToIndex(e.point)
      if (l.getCellBounds(index, index).contains(e.point)) {
        cellPressed(e, index)
      } else {
        EventQueue.invokeLater {
          l.clearSelection()
          l.selectionModel.anchorSelectionIndex = -1
          l.selectionModel.leadSelectionIndex = -1
        }
      }
      srcPoint.location = e.point
      l.repaint()
    }

    private fun cellPressed(
      e: MouseEvent,
      index: Int,
    ) {
      if (SwingUtilities.isLeftMouseButton(e) && e.clickCount > 1) {
        val item = model.getElementAt(index)
        JOptionPane.showMessageDialog(rootPane, item.title)
      } else {
        checkedIndex = -1
        getDeepestButtonAt(e, index)?.also {
          checkedIndex = index
          if (isSelectedIndex(index)) {
            setFocusable(false)
            removeSelectionInterval(index, index)
          } else {
            setSelectionInterval(index, index)
          }
        }
      }
    }

    private fun getDeepestButtonAt(
      e: MouseEvent,
      index: Int,
    ): Component? = if (e.isShiftDown || e.isControlDown || e.isAltDown) {
      null
    } else {
      val list = this@MultipleSelectionList
      val proto = list.prototypeCellValue
      val cr = list.cellRenderer
      val c = cr.getListCellRendererComponent(list, proto, index, false, false)
      val r = list.getCellBounds(index, index)
      c.bounds = r
      val pt = e.point
      pt.translate(-r.x, -r.y)
      val box = SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y)
      if (box is Box.Filler || box is JCheckBox) box else null
    }
  }

  private inner class ListItemCellRenderer : ListCellRenderer<ListItem> {
    private val renderer = JPanel(BorderLayout(0, 0))
    private val check = JCheckBox()
    private val filler = Box.createRigidArea(check.getPreferredSize())
    private val label = JLabel("")
    private val itemPanel = object : JPanel(BorderLayout(2, 2)) {
      override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (SELECTED_COLOR == getBackground()) {
          val g2 = g.create() as? Graphics2D ?: return
          g2.paint = SELECTED_COLOR
          g2.fillRect(0, 0, getWidth(), getHeight())
          g2.dispose()
        }
      }
    }
    val focusBorder: Border = UIManager.getBorder("List.focusCellHighlightBorder")
    val noFocusBorder = getNoFocusBorder(focusBorder)

    init {
      itemPanel.setBorder(noFocusBorder)
      label.setForeground(itemPanel.getForeground())
      label.setBackground(itemPanel.getBackground())
      label.setOpaque(false)
      check.setOpaque(false)
      check.isVisible = false
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
      itemPanel.border = if (cellHasFocus) focusBorder else noFocusBorder
      check.isSelected = isSelected
      check.model.isRollover = index == rollOverIndex
      if (isSelected) {
        label.foreground = list.selectionForeground
        label.background = SELECTED_COLOR
        itemPanel.background = SELECTED_COLOR
        itemPanel.add(check, BorderLayout.WEST)
        check.isVisible = true
      } else if (index == rollOverIndex) {
        itemPanel.background = ROLLOVER_COLOR
        itemPanel.add(check, BorderLayout.WEST)
        check.isVisible = true
      } else {
        label.foreground = list.foreground
        label.background = list.background
        itemPanel.background = list.background
        itemPanel.add(filler, BorderLayout.WEST)
        check.isVisible = false
      }
      return renderer
    }
  }

  companion object {
    val SELECTED_COLOR = Color(0x40_32_64_FF, true)
    val ROLLOVER_COLOR = Color(0x40_32_64_AA, true)
  }
}

private class SimpleListItemCellRenderer : ListCellRenderer<ListItem> {
  private val renderer = JLabel("")
  private val focusBorder: Border = UIManager.getBorder(
    "List.focusCellHighlightBorder",
  )
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
