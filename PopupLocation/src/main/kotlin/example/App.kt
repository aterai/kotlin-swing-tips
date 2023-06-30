package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.Border

fun makeUI(): Component {
  val popup = JPopupMenu()
  popup.add("menu 1")
  popup.add("menu 2")
  popup.add("menu 3")

  val field1 = JTextField("default JTextField")
  field1.componentPopupMenu = popup

  val field2 = object : JTextField("override JTextField#getPopupLocation(MouseEvent)") {
    override fun getPopupLocation(event: MouseEvent?) = if (event == null) {
      runCatching {
        modelToView(caretPosition)?.let { r ->
          r.location.also { it.translate(0, r.height) }
        }
      }.getOrNull()
    } else {
      super.getPopupLocation(event)
    }
  }
  field2.componentPopupMenu = popup

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
  val list = NewspaperStyleList(model)
  list.componentPopupMenu = popup

  val p = JPanel(GridLayout(2, 1, 5, 5))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(field1)
  p.add(field2)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val icon = JLabel(null, null, SwingConstants.CENTER)
  private val label = JLabel(" ", SwingConstants.CENTER)
  private val renderer = object : JPanel(BorderLayout()) {
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
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = UIManager.getBorder("List.noFocusBorder") ?: getNoFocusBorder()

  init {
    renderer.border = noFocusBorder
    renderer.isOpaque = true
    label.foreground = renderer.foreground
    label.background = renderer.background
    label.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    label.isOpaque = false
    icon.isOpaque = false
    renderer.add(icon)
    renderer.add(label, BorderLayout.SOUTH)
  }

  private fun getNoFocusBorder(): Border {
    val i = focusBorder.getBorderInsets(renderer)
    return BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right)
  }

  override fun getListCellRendererComponent(
    list: JList<out ListItem>,
    value: ListItem,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    icon.icon = value.icon
    label.text = value.title
    renderer.border = if (cellHasFocus) focusBorder else noFocusBorder
    if (isSelected) {
      label.foreground = list.selectionForeground
      renderer.background = SELECTED_COLOR
    } else {
      label.foreground = list.foreground
      renderer.background = list.background
    }
    return renderer
  }

  companion object {
    val SELECTED_COLOR = Color(0xAE_16_64_FF.toInt(), true)
  }
}

private data class ListItem(val title: String, val icon: Icon)

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.paint = Color.BLACK
    g2.drawRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 32
}

private open class NewspaperStyleList(model: DefaultListModel<ListItem>) : JList<ListItem>(model) {
  override fun updateUI() {
    selectionForeground = null
    selectionBackground = null
    cellRenderer = null
    super.updateUI()
    layoutOrientation = HORIZONTAL_WRAP
    selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    visibleRowCount = 0
    fixedCellWidth = 64
    fixedCellHeight = 64
    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    cellRenderer = ListItemListCellRenderer()
  }

  override fun getPopupLocation(event: MouseEvent?) = if (event == null) {
    val i = leadSelectionIndex
    getCellBounds(i, i)?.let { Point(it.centerX.toInt(), it.centerY.toInt()) }
  } else {
    super.getPopupLocation(event)
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
