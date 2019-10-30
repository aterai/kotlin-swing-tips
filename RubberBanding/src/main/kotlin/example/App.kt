package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border

class MainPanel : JPanel(BorderLayout()) {
  init {
    val model = DefaultListModel<ListItem>().also {
      // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
      it.addElement(ListItem("wi0054 aaa", "wi0054-32.png"))
      it.addElement(ListItem("test", "wi0062-32.png"))
      it.addElement(ListItem("wi0063 00", "wi0063-32.png"))
      it.addElement(ListItem("Test", "wi0064-32.png"))
      it.addElement(ListItem("12345", "wi0096-32.png"))
      it.addElement(ListItem("111111", "wi0054-32.png"))
      it.addElement(ListItem("22222", "wi0062-32.png"))
      it.addElement(ListItem("3333", "wi0063-32.png"))
    }
    add(JScrollPane(RubberBandSelectionList(model)))
    setPreferredSize(Dimension(320, 240))
  }
}

data class ListItem(val title: String, val iconFile: String) {
  val icon = ImageIcon(javaClass.getResource(iconFile))
  val selectedIcon: ImageIcon

  init {
    val ip = FilteredImageSource(icon.getImage().getSource(), SelectedImageFilter())
    this.selectedIcon = ImageIcon(Toolkit.getDefaultToolkit().createImage(ip))
  }
}

class RubberBandSelectionList(model: ListModel<ListItem>) : JList<ListItem>(model) {
  private var rbl: RubberBandingListener? = null
  private var rubberBandColor: Color? = null
  private val rubberBand = Path2D.Double()

  override fun updateUI() {
    setSelectionForeground(null) // Nimbus
    setSelectionBackground(null) // Nimbus
    setCellRenderer(null)
    removeMouseListener(rbl)
    removeMouseMotionListener(rbl)
    super.updateUI()

    rubberBandColor = makeRubberBandColor(getSelectionBackground())
    setLayoutOrientation(HORIZONTAL_WRAP)
    setVisibleRowCount(0)
    setFixedCellWidth(62)
    setFixedCellHeight(62)
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))

    setCellRenderer(ListItemListCellRenderer())
    rbl = RubberBandingListener()
    addMouseMotionListener(rbl)
    addMouseListener(rbl)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as Graphics2D
    g2.setPaint(getSelectionBackground())
    g2.draw(rubberBand)
    g2.setComposite(ALPHA)
    g2.setPaint(rubberBandColor)
    g2.fill(rubberBand)
    g2.dispose()
  }

  private inner class RubberBandingListener : MouseAdapter() {
    private val srcPoint = Point()

    override fun mouseDragged(e: MouseEvent) {
      val l = e.getComponent() as? JList<*> ?: return
      l.setFocusable(true)
      val destPoint = e.getPoint()
      rubberBand.reset()
      rubberBand.moveTo(srcPoint.getX(), srcPoint.getY())
      rubberBand.lineTo(destPoint.getX(), srcPoint.getY())
      rubberBand.lineTo(destPoint.getX(), destPoint.getY())
      rubberBand.lineTo(srcPoint.getX(), destPoint.getY())
      rubberBand.closePath()

      val indices = (0 until l.getModel().getSize())
        .filter { rubberBand.intersects(l.getCellBounds(it, it)) }
        .toIntArray()
      l.setSelectedIndices(indices)
      l.repaint()
    }

    override fun mouseReleased(e: MouseEvent) {
      rubberBand.reset()
      val c = e.getComponent()
      c.setFocusable(true)
      c.repaint()
    }

    override fun mousePressed(e: MouseEvent) {
      val l = e.getComponent() as? JList<*> ?: return
      val index = l.locationToIndex(e.getPoint())
      val rect = l.getCellBounds(index, index)
      if (rect.contains(e.getPoint())) {
        l.setFocusable(true)
      } else {
        l.clearSelection()
        l.getSelectionModel().setAnchorSelectionIndex(-1)
        l.getSelectionModel().setLeadSelectionIndex(-1)
        l.setFocusable(false)
      }
      srcPoint.setLocation(e.getPoint())
      l.repaint()
    }
  }

  companion object {
    private val ALPHA = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .1f)

    private fun makeRubberBandColor(c: Color): Color {
      val r = c.getRed()
      val g = c.getGreen()
      val b = c.getBlue()
      return when {
        r > g -> if (r > b) Color(r, 0, 0) else Color(0, 0, b)
        else -> if (g > b) Color(0, g, 0) else Color(0, 0, b)
      }
    }
  }
}

class SelectedImageFilter : RGBImageFilter() {
  // override fun filterRGB(x: Int, y: Int, argb: Int) = argb and -0x100 or (argb and 0xFF shr 1)
  override fun filterRGB(x: Int, y: Int, argb: Int) = argb and 0xFF_FF_FF_00.toInt() or (argb and 0xFF shr 1)
}

class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val renderer = JPanel(BorderLayout())
  private val icon = JLabel(null as? Icon?, SwingConstants.CENTER)
  private val label = JLabel("", SwingConstants.CENTER)
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = UIManager.getBorder("List.noFocusBorder") ?: getNimbusNoFocusBorder()

  init {
    icon.setOpaque(false)
    label.setForeground(renderer.getForeground())
    label.setBackground(renderer.getBackground())
    label.setBorder(noFocusBorder)
    renderer.setOpaque(false)
    renderer.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
    renderer.add(icon)
    renderer.add(label, BorderLayout.SOUTH)
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
    label.setText(value.title)
    label.setBorder(if (cellHasFocus) focusBorder else noFocusBorder)
    if (isSelected) {
      icon.setIcon(value.selectedIcon)
      label.setForeground(list.getSelectionForeground())
      label.setBackground(list.getSelectionBackground())
      label.setOpaque(true)
    } else {
      icon.setIcon(value.icon)
      label.setForeground(list.getForeground())
      label.setBackground(list.getBackground())
      label.setOpaque(false)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
