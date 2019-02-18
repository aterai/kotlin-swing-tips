package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.util.stream.IntStream
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border

class MainPanel : JPanel(BorderLayout()) {
  init {
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
    model.addElement(ListItem("black", ColorIcon(Color.BLACK)))
    model.addElement(ListItem("white", ColorIcon(Color.WHITE)))

    val list = RubberBandSelectionList(model)
    list.setOpaque(false)
    list.setBackground(Color(0x0, true))
    list.setForeground(Color.WHITE)
    // list.addListSelectionListener(e -> SwingUtilities.getUnwrappedParent((Component) e.getSource()).repaint());

    val scroll = JScrollPane(list)
    scroll.setBackground(Color(0x0, true))
    scroll.setOpaque(false)
    scroll.setBorder(BorderFactory.createEmptyBorder())
    scroll.setViewportBorder(BorderFactory.createEmptyBorder())
    // scroll.getViewport().addChangeListener(e -> ((Component) e.getSource()).repaint());
    scroll.getViewport().setOpaque(false)

    val panel = object : JPanel(BorderLayout()) {
      private val texture = TextureUtils.createCheckerTexture(6)
      override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setPaint(texture)
        g2.fillRect(0, 0, getWidth(), getHeight())
        g2.dispose()
        super.paintComponent(g)
      }
    }
    panel.setOpaque(false)
    panel.add(scroll)

    add(panel)
    setPreferredSize(Dimension(320, 240))
  }
}

internal class RubberBandSelectionList(model: ListModel<ListItem>) : JList<ListItem>(model) {
  private var rbl: RubberBandingListener? = null
  private var rubberBandColor: Color? = null
  protected val rubberBand: Path2D = Path2D.Double()

  override fun updateUI() {
    setSelectionForeground(null) // Nimbus
    setSelectionBackground(null) // Nimbus
    setCellRenderer(null)
    removeMouseListener(rbl)
    removeMouseMotionListener(rbl)
    super.updateUI()

    rubberBandColor = makeRubberBandColor(getSelectionBackground())
    setLayoutOrientation(JList.HORIZONTAL_WRAP)
    setVisibleRowCount(0)
    setFixedCellWidth(62)
    setFixedCellHeight(62)
    setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10))

    setCellRenderer(ListItemListCellRenderer())
    rbl = RubberBandingListener()
    addMouseMotionListener(rbl)
    addMouseListener(rbl)
  }

  protected override fun paintComponent(g: Graphics) {
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
      val l = e.getComponent() as JList<*>
      l.setFocusable(true)
      val destPoint = e.getPoint()
      val rb = rubberBand
      rb.reset()
      rb.moveTo(srcPoint.x.toDouble(), srcPoint.y.toDouble())
      rb.lineTo(destPoint.x.toDouble(), srcPoint.y.toDouble())
      rb.lineTo(destPoint.x.toDouble(), destPoint.y.toDouble())
      rb.lineTo(srcPoint.x.toDouble(), destPoint.y.toDouble())
      rb.closePath()

      val indices = IntStream.range(0, l.getModel().getSize())
          .filter { rb.intersects(l.getCellBounds(it, it)) }.toArray()

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
      val l = e.getComponent() as JList<*>
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
      return if (r > g)
        if (r > b) Color(r, 0, 0) else Color(0, 0, b)
        else if (g > b) Color(0, g, 0) else Color(0, 0, b)
    }
  }
}

internal class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val label = object : JLabel("", null as Icon?, SwingConstants.CENTER) {
    protected override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      if (SELECTED_COLOR == getBackground()) {
        val g2 = g.create() as Graphics2D
        g2.setPaint(SELECTED_COLOR)
        g2.fillRect(0, 0, getWidth(), getHeight())
        g2.dispose()
      }
    }
  }
  private val renderer = JPanel(BorderLayout())
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = UIManager.getBorder("List.noFocusBorder") ?: getNimbusNoFocusBorder()

  init {
    label.setVerticalTextPosition(SwingConstants.BOTTOM)
    label.setHorizontalTextPosition(SwingConstants.CENTER)
    label.setForeground(renderer.getForeground())
    label.setBackground(renderer.getBackground())
    label.setBorder(noFocusBorder)
    label.setOpaque(false)
    renderer.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
    renderer.add(label)
    renderer.setOpaque(false)
  }

  fun getNimbusNoFocusBorder(): Border {
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
    label.setIcon(value.icon)
    if (isSelected) {
      label.setForeground(list.getSelectionForeground())
      label.setBackground(SELECTED_COLOR)
    } else {
      label.setForeground(list.getForeground())
      label.setBackground(list.getBackground())
    }
    return renderer
  }

  companion object {
    protected val SELECTED_COLOR = Color(50, 100, 255, 64)
  }
}

internal class ListItem(val title: String, val icon: Icon)

internal object TextureUtils {
  private val DEFAULT_COLOR = Color(-0x11cdcdce, true)

  @JvmOverloads
  fun createCheckerTexture(cs: Int, color: Color = DEFAULT_COLOR): TexturePaint {
    val size = cs * cs
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    g2.setPaint(color)
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
} /* HideUtilityClassConstructor */

internal class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.setPaint(color)
    g2.fillRect(0, 0, getIconWidth(), getIconHeight())
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 32
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
