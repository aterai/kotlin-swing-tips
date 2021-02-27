package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports
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

  val list = RubberBandSelectionList(model).also {
    it.isOpaque = false
    it.background = Color(0x0, true)
    it.foreground = Color.WHITE
  }

  val scroll = JScrollPane(list).also {
    it.background = Color(0x0, true)
    it.isOpaque = false
    it.border = BorderFactory.createEmptyBorder()
    it.viewportBorder = BorderFactory.createEmptyBorder()
    it.viewport.isOpaque = false
  }

  val panel = object : JPanel(BorderLayout()) {
    private val texture = TextureUtils.createCheckerTexture(6)
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = texture
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      super.paintComponent(g)
    }
  }
  panel.isOpaque = false
  panel.add(scroll)

  return JPanel(BorderLayout()).also {
    it.add(panel)
    it.preferredSize = Dimension(320, 240)
  }
}

private class RubberBandSelectionList(model: ListModel<ListItem>) : JList<ListItem>(model) {
  private var rbl: RubberBandingListener? = null
  private var rubberBandColor: Color? = null
  private val rubberBand = Path2D.Double()

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
    fixedCellWidth = 62
    fixedCellHeight = 62
    border = BorderFactory.createEmptyBorder(5, 10, 5, 10)

    cellRenderer = ListItemListCellRenderer()
    rbl = RubberBandingListener()
    addMouseMotionListener(rbl)
    addMouseListener(rbl)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = selectionBackground
    g2.draw(rubberBand)
    g2.composite = ALPHA
    g2.paint = rubberBandColor
    g2.fill(rubberBand)
    g2.dispose()
  }

  private inner class RubberBandingListener : MouseAdapter() {
    private val srcPoint = Point()

    override fun mouseDragged(e: MouseEvent) {
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
        .filter { rubberBand.intersects(l.getCellBounds(it, it)) }.toIntArray()
      l.selectedIndices = indices
      l.repaint()
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
      val rect = l.getCellBounds(index, index)
      if (rect.contains(e.point)) {
        l.setFocusable(true)
      } else {
        l.clearSelection()
        l.selectionModel.anchorSelectionIndex = -1
        l.selectionModel.leadSelectionIndex = -1
        l.setFocusable(false)
      }
      srcPoint.location = e.point
      l.repaint()
    }
  }

  companion object {
    private val ALPHA = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .1f)

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
  }
}

private class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val label = object : JLabel("", null, SwingConstants.CENTER) {
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
  private val renderer = JPanel(BorderLayout())
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = UIManager.getBorder("List.noFocusBorder") ?: getNimbusNoFocusBorder()

  init {
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
    label.border = if (cellHasFocus) focusBorder else noFocusBorder
    label.icon = value.icon
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
    val SELECTED_COLOR = Color(50, 100, 255, 64)
  }
}

private data class ListItem(val title: String, val icon: Icon)

private object TextureUtils {
  // private val DEFAULT_COLOR = Color(0xEE_32_32_32.toInt(), true)
  // private val DEFAULT_COLOR = Color(-0x11CDCDCE, true)
  private val DEFAULT_COLOR = Color(0x32, 0x32, 0x32, 0xEE)

  @JvmOverloads
  fun createCheckerTexture(cs: Int, color: Color = DEFAULT_COLOR): TexturePaint {
    val size = cs * cs
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    g2.paint = color
    g2.fillRect(0, 0, size, size)
    var i = 0
    while (i * cs < size) {
      var j = 0
      while (j * cs < size) {
        if ((i + j) % 2 == 0) {
          g2.fillRect(i * cs, j * cs, cs, cs)
        }
        j++
      }
      i++
    }
    g2.dispose()
    return TexturePaint(img, Rectangle(size, size))
  }
}

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
