package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(JScrollPane(RubberBandSelectionList(makeModel())))
  it.preferredSize = Dimension(320, 240)
}

private fun makeModel() = DefaultListModel<ListItem>().also {
  // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
  it.addElement(ListItem("wi0054 aaa", "example/wi0054-32.png"))
  it.addElement(ListItem("test", "example/wi0062-32.png"))
  it.addElement(ListItem("wi0063 00", "example/wi0063-32.png"))
  it.addElement(ListItem("Test", "example/wi0064-32.png"))
  it.addElement(ListItem("12345", "example/wi0096-32.png"))
  it.addElement(ListItem("111111", "example/wi0054-32.png"))
  it.addElement(ListItem("22222", "example/wi0062-32.png"))
  it.addElement(ListItem("3333", "example/wi0063-32.png"))
}

private data class ListItem(val title: String, val iconFile: String) {
  val img = makeImage(iconFile)
  val icon = ImageIcon(img)
  val selectedIcon: ImageIcon

  init {
    val ip = FilteredImageSource(img.source, SelectedImageFilter())
    selectedIcon = ImageIcon(Toolkit.getDefaultToolkit().createImage(ip))
  }
}

private fun makeImage(path: String): Image {
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  return url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val iw = missingIcon.iconWidth
  val ih = missingIcon.iconHeight
  val bi = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, (32 - iw) / 2, (32 - ih) / 2)
  g2.dispose()
  return bi
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
    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

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
        .filter { rubberBand.intersects(l.getCellBounds(it, it)) }
        .toIntArray()
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

private class SelectedImageFilter : RGBImageFilter() {
  // override fun filterRGB(x: Int, y: Int, argb: Int) = argb and -0x100 or (argb and 0xFF shr 1)
  override fun filterRGB(x: Int, y: Int, argb: Int) =
    argb and 0xFF_FF_FF_00.toInt() or (argb and 0xFF shr 1)
}

private class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val renderer = JPanel(BorderLayout())
  private val icon = JLabel(null as? Icon?, SwingConstants.CENTER)
  private val label = JLabel("", SwingConstants.CENTER)
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = UIManager.getBorder("List.noFocusBorder") ?: getSynthNoFocusBorder()

  init {
    icon.isOpaque = false
    label.foreground = renderer.foreground
    label.background = renderer.background
    label.border = noFocusBorder
    renderer.isOpaque = false
    renderer.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    renderer.add(icon)
    renderer.add(label, BorderLayout.SOUTH)
  }

  private fun getSynthNoFocusBorder(): Border {
    val i = focusBorder.getBorderInsets(label)
    return BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right)
  }

  override fun getListCellRendererComponent(
    list: JList<out ListItem>,
    value: ListItem?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    if (value != null) {
      label.text = value.title
      label.border = if (cellHasFocus) focusBorder else noFocusBorder
      if (isSelected) {
        icon.icon = value.selectedIcon
        label.foreground = list.selectionForeground
        label.background = list.selectionBackground
        label.isOpaque = true
      } else {
        icon.icon = value.icon
        label.foreground = list.foreground
        label.background = list.background
        label.isOpaque = false
      }
    }
    return renderer
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
