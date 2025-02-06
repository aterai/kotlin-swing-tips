package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.MouseInputAdapter

private val defaultModel = arrayOf(
  ListItem("red", Color.RED),
  ListItem("green", Color.GREEN),
  ListItem("blue", Color.BLUE),
  ListItem("cyan", Color.CYAN),
  ListItem("darkGray", Color.DARK_GRAY),
  ListItem("gray", Color.GRAY),
  ListItem("lightGray", Color.LIGHT_GRAY),
  ListItem("magenta", Color.MAGENTA),
  ListItem("orange", Color.ORANGE),
  ListItem("pink", Color.PINK),
  ListItem("yellow", Color.YELLOW),
  ListItem("black", Color.BLACK),
  ListItem("white", Color.WHITE),
)
private val model = DefaultListModel<ListItem>()
private val list = object : JList<ListItem>(model) {
  @Transient private var handler: MouseInputAdapter? = null

  override fun updateUI() {
    removeMouseListener(handler)
    selectionForeground = null
    selectionBackground = null
    cellRenderer = null
    super.updateUI()
    layoutOrientation = HORIZONTAL_WRAP
    selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    visibleRowCount = 0
    fixedCellWidth = 56
    fixedCellHeight = 56
    border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
    cellRenderer = ListItemListCellRenderer()
    handler = ClearSelectionListener()
    addMouseListener(handler)
  }
}
private var comparator: Comparator<ListItem>? = null
private val ascending = JRadioButton("ascending", true)
private val descending = JRadioButton("descending")
private val directionList = listOf(ascending, descending)
private val r1 = JRadioButton("None", true)
private val r2 = JRadioButton("Name")
private val r3 = JRadioButton("Color")

fun makeUI(): Component {
  defaultModel.forEach { model.addElement(it) }
  list.model = model

  val box1 = Box.createHorizontalBox()
  box1.add(JLabel("Sort: "))
  val bg1 = ButtonGroup()
  listOf(r1, r2, r3).forEach {
    bg1.add(it)
    box1.add(it)
  }
  box1.add(Box.createHorizontalGlue())

  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      comparator = null
      sort()
    }
  }
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      comparator = Comparator.comparing(ListItem::title)
      reversed()
      sort()
    }
  }
  r3.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      comparator = Comparator.comparing { it.color.rgb }
      reversed()
      sort()
    }
  }

  val p = JPanel(GridLayout(2, 1))
  p.border = BorderFactory.createEmptyBorder(2, 5, 2, 2)
  p.add(box1)
  p.add(makeDirectionBox())

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun reversed() {
  if (descending.isSelected) {
    comparator = comparator?.reversed()
  }
}

private fun makeDirectionBox(): Component {
  val box2 = Box.createHorizontalBox()
  box2.add(JLabel("Direction: "))
  val bg2 = ButtonGroup()
  val listener = ItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      comparator = comparator?.reversed()
      sort()
    }
  }
  directionList.forEach {
    bg2.add(it)
    box2.add(it)
    it.addItemListener(listener)
    it.isEnabled = false
  }
  box2.add(Box.createHorizontalGlue())
  return box2
}

private fun sort() {
  val selected = list.selectedValuesList
  model.clear()
  directionList.forEach { item -> item.isEnabled = false }
  comparator?.also {
    defaultModel.sortedWith(it).forEach { item -> model.addElement(item) }
    directionList.forEach { item -> item.isEnabled = true }
  } ?: defaultModel.forEach { model.addElement(it) }
  for (item in selected) {
    val i = model.indexOf(item)
    list.addSelectionInterval(i, i)
  }
}

private class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val label = JLabel("", null, SwingConstants.CENTER)
  private val renderer = JPanel(BorderLayout())
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = getNoFocusBorder(focusBorder)

  init {
    label.verticalTextPosition = SwingConstants.BOTTOM
    label.horizontalTextPosition = SwingConstants.CENTER
    label.foreground = renderer.foreground
    label.background = renderer.background
    label.border = noFocusBorder
    label.isOpaque = false
    renderer.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    renderer.add(label)
    renderer.isOpaque = true
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
    label.text = value.title
    label.border = if (cellHasFocus) focusBorder else noFocusBorder
    label.icon = value.icon
    if (isSelected) {
      label.foreground = list.selectionForeground
      renderer.background = list.selectionBackground
    } else {
      label.foreground = list.foreground
      renderer.background = list.background
    }
    return renderer
  }
}

private data class ListItem(
  val title: String,
  val color: Color,
) {
  val icon = ColorIcon(color)
}

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
    g2.paint = Color.BLACK
    g2.drawRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 30

  override fun getIconHeight() = 30
}

// https://github.com/aterai/java-swing-tips/blob/master/ClearSelection/src/java/example/MainPanel.java
private class ClearSelectionListener : MouseInputAdapter() {
  private var startOutside = false

  override fun mousePressed(e: MouseEvent) {
    val list = e.component as? JList<*> ?: return
    startOutside = !contains(list, e.point)
    if (startOutside) {
      clearSelectionAndFocus(list)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    startOutside = false
  }

  override fun mouseDragged(e: MouseEvent) {
    val list = e.component as? JList<*> ?: return
    if (contains(list, e.point)) {
      startOutside = false
    } else if (startOutside) {
      clearSelectionAndFocus(list)
    }
  }

  companion object {
    private fun <E> clearSelectionAndFocus(list: JList<E>) {
      list.clearSelection()
      list.selectionModel.anchorSelectionIndex = -1
      list.selectionModel.leadSelectionIndex = -1
    }

    private fun <E> contains(
      list: JList<E>,
      pt: Point,
    ): Boolean {
      for (i in 0..<list.model.size) {
        if (list.getCellBounds(i, i).contains(pt)) {
          return true
        }
      }
      return false
    }
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
