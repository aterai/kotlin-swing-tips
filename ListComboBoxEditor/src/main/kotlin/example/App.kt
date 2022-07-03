package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.ComboPopup

fun makeUI(): Component {
  val model = makeModel()
  val list = NewspaperStyleList(model)
  val scroll = object : JScrollPane(list) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 62 * 3 + 10
      d.height = 32
      return d
    }
  }
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.border = BorderFactory.createEmptyBorder()
  scroll.viewportBorder = BorderFactory.createEmptyBorder()
  val list2 = NewspaperStyleList(model)
  val scroll2 = object : JScrollPane(list2) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 62 * 4 + 10
      return d
    }
  }
  scroll2.border = BorderFactory.createEmptyBorder()
  scroll2.viewportBorder = BorderFactory.createEmptyBorder()
  val popup = JPopupMenu()
  popup.layout = BorderLayout()
  list2.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (popup.isVisible && e.clickCount >= 2) {
        popup.isVisible = false
      }
    }
  })
  popup.add(scroll2)
  popup.border = BorderFactory.createLineBorder(Color.GRAY)
  val dropDown = JToggleButton(DropDownArrowIcon())
  popup.addPopupMenuListener(object : PopupMenuListener {
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      list2.selectedIndex = list.selectedIndex
      EventQueue.invokeLater { popup.pack() }
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      dropDown.isSelected = false
      list.requestFocusInWindow()
      val i = list2.selectedIndex
      if (i >= 0) {
        list.selectedIndex = i
        list.scrollRectToVisible(list.getCellBounds(i, i))
      }
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
      popupMenuWillBecomeInvisible(e)
    }
  })
  dropDown.border = BorderFactory.createEmptyBorder()
  dropDown.isContentAreaFilled = false
  dropDown.isFocusPainted = false
  dropDown.isFocusable = false
  dropDown.addItemListener { e ->
    val b = e.itemSelectable
    if (e.stateChange == ItemEvent.SELECTED && b is AbstractButton) {
      popup.show(b, -scroll.width, b.height)
    }
  }
  val verticalScrollBar = scroll.verticalScrollBar
  val verticalBox = object : JPanel(BorderLayout()) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = 32 + 5 + 5
      return d
    }
  }
  verticalBox.isOpaque = false
  verticalBox.add(verticalScrollBar)
  verticalBox.add(dropDown, BorderLayout.SOUTH)
  val panel = JPanel(BorderLayout(0, 0))
  panel.add(scroll)
  panel.add(verticalBox, BorderLayout.EAST)
  panel.border = BorderFactory.createLineBorder(Color.DARK_GRAY)
  val p = JPanel(GridLayout(2, 1, 25, 25))
  p.add(makeTitledPanel("JList + JPopupMenu", panel))
  p.add(makeTitledPanel("JComboBox + ComboBoxEditor", makeListEditorComboBox()))
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeListEditorComboBox(): JComboBox<ListItem> {
  val model = makeModel()
  val list = NewspaperStyleList(model)
  val scroll = object : JScrollPane(list) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 62 * 4
      d.height = 40
      return d
    }
  }
  scroll.border = BorderFactory.createEmptyBorder()
  scroll.viewportBorder = BorderFactory.createEmptyBorder()
  val cm = DefaultComboBoxModel<ListItem>()
  for (i in 0 until model.size) {
    cm.addElement(model.getElementAt(i))
  }
  val combo = object : JComboBox<ListItem>(cm) {
    override fun updateUI() {
      super.updateUI()
      setMaximumRowCount(4)
      prototypeDisplayValue = ListItem("red", ColorIcon(Color.RED))
      EventQueue.invokeLater {
        (getAccessibleContext().getAccessibleChild(0) as? ComboPopup)?.also {
          val lst = it.list
          lst.layoutOrientation = JList.HORIZONTAL_WRAP
          lst.visibleRowCount = 0
          lst.fixedCellWidth = 62
          lst.fixedCellHeight = 40
          lst.isOpaque = true
          lst.background = Color(0x32_32_32)
          lst.setForeground(Color.WHITE)
        }
      }
    }
  }
  combo.setRenderer(ListItemListCellRenderer())
  combo.isEditable = true
  combo.editor = object : ComboBoxEditor {
    override fun getEditorComponent(): Component {
      return scroll
    }

    override fun setItem(anObject: Any?) {
      combo.selectedIndex = list.selectedIndex
    }

    override fun getItem(): Any? {
      return list.selectedValue
    }

    override fun selectAll() {
      // println("selectAll")
    }

    override fun addActionListener(l: ActionListener) {
      // println("addActionListener")
    }

    override fun removeActionListener(l: ActionListener) {
      // println("removeActionListener")
    }
  }
  return combo
}

private fun makeModel(): ListModel<ListItem> {
  val model = DefaultListModel<ListItem>()
  model.addElement(ListItem("red", ColorIcon(Color.RED)))
  model.addElement(ListItem("green", ColorIcon(Color.GREEN)))
  model.addElement(ListItem("blue", ColorIcon(Color.BLUE)))
  model.addElement(ListItem("cyan", ColorIcon(Color.CYAN)))
  model.addElement(ListItem("darkGray", ColorIcon(Color.DARK_GRAY)))
  model.addElement(ListItem("gray", ColorIcon(Color.GRAY)))
  model.addElement(ListItem("lightGray", ColorIcon(Color.LIGHT_GRAY)))
  model.addElement(ListItem("magenta", ColorIcon(Color.MAGENTA)))
  model.addElement(ListItem("orange", ColorIcon(Color.ORANGE)))
  model.addElement(ListItem("pink", ColorIcon(Color.PINK)))
  model.addElement(ListItem("yellow", ColorIcon(Color.YELLOW)))
  // model.addElement(new ListItem("black", new ColorIcon(Color.BLACK)));
  model.addElement(ListItem("white", ColorIcon(Color.WHITE)))
  return model
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class NewspaperStyleList(model: ListModel<ListItem>) : JList<ListItem>(model) {
  override fun updateUI() {
    selectionForeground = null // Nimbus
    selectionBackground = null // Nimbus
    cellRenderer = null
    super.updateUI()
    layoutOrientation = HORIZONTAL_WRAP
    visibleRowCount = 0
    fixedCellWidth = 62
    fixedCellHeight = 40
    setCellRenderer(ListItemListCellRenderer())
    isOpaque = true
    background = Color(0x323232)
    foreground = Color.WHITE
  }
}

private class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val label: JLabel = object : JLabel("", null, CENTER) {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      if (SELECTED_COLOR == background) {
        val g2 = g.create() as Graphics2D
        g2.paint = SELECTED_COLOR
        g2.fillRect(0, 0, width, height)
        g2.dispose()
      }
    }
  }
  private val renderer = JPanel(BorderLayout())
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder: Border

  init {
    var b = UIManager.getBorder("List.noFocusBorder")
    if (b == null) { // Nimbus???
      val i = focusBorder.getBorderInsets(renderer)
      b = BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right)
    }
    noFocusBorder = b
    label.verticalTextPosition = SwingConstants.BOTTOM
    label.horizontalTextPosition = SwingConstants.CENTER
    label.foreground = renderer.foreground
    label.background = renderer.background
    label.border = noFocusBorder
    label.isOpaque = false
    renderer.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    renderer.add(label)
    renderer.isOpaque = false
  }

  override fun getListCellRendererComponent(
    list: JList<out ListItem>,
    value: ListItem?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    label.text = value?.title
    label.border = if (cellHasFocus) focusBorder else noFocusBorder
    label.icon = value?.icon
    if (isSelected) {
      label.foreground = list.selectionForeground
      label.background = SELECTED_COLOR
    } else {
      label.foreground = list.foreground
      label.background = list.background
    }
    return renderer
  }

  companion object {
    protected val SELECTED_COLOR = Color(0x40_32_64_FF, true)
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

  override fun getIconHeight() = 12
}

private class DropDownArrowIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    if (c is AbstractButton && c.isSelected) {
      g2.paint = Color.LIGHT_GRAY
    } else {
      g2.paint = Color.DARK_GRAY
    }
    g2.drawLine(2, 3, 6, 3)
    g2.drawLine(3, 4, 5, 4)
    g2.drawLine(4, 5, 4, 5)
    g2.dispose()
  }

  override fun getIconWidth() = 9

  override fun getIconHeight() = 9
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
      defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
