package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

fun makeUI(): Component {
  val model = DefaultListModel<ListItem>().also {
    // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
    it.addElement(ListItem("wi0009-32", "wi0009-32.png"))
    it.addElement(ListItem("12345", "wi0054-32.png"))
    it.addElement(ListItem("wi0062-32.png", "wi0062-32.png"))
    it.addElement(ListItem("test", "wi0063-32.png"))
    it.addElement(ListItem("32.png", "wi0064-32.png"))
    it.addElement(ListItem("wi0096-32.png", "wi0096-32.png"))
    it.addElement(ListItem("6896", "wi0111-32.png"))
    it.addElement(ListItem("t467467est", "wi0122-32.png"))
    it.addElement(ListItem("test123", "wi0124-32.png"))
    it.addElement(ListItem("test(1)", "wi0126-32.png"))
  }
  val list = ReorderingList(model)

  val check = object : JCheckBox("Compact drag image mode") {
    override fun updateUI() {
      super.updateUI()
      isSelected = false
    }
  }
  check.addActionListener { e ->
    if ((e.source as? JCheckBox)?.isSelected == true) {
      list.transferHandler = CompactListItemTransferHandler()
    } else {
      list.transferHandler = ListItemTransferHandler()
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
}

private data class ListItem(val title: String, val iconFile: String) {
  val icon = ImageIcon(javaClass.getResource(iconFile))
  val selectedIcon: ImageIcon

  init {
    val ip = FilteredImageSource(icon.image.source, SelectedImageFilter())
    selectedIcon = ImageIcon(Toolkit.getDefaultToolkit().createImage(ip))
  }
}

private class ReorderingList(model: ListModel<ListItem>) : JList<ListItem>(model) {
  private var rbl: MouseInputListener? = null
  private var rubberBandColor: Color? = null
  private val rubberBand = Path2D.Double()

  override fun updateUI() {
    selectionForeground = null // Nimbus
    selectionBackground = null // Nimbus
    cellRenderer = null
    transferHandler = null
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

    // putClientProperty("List.isFileList", true)
    selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    transferHandler = ListItemTransferHandler()
    dropMode = DropMode.INSERT
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (dragEnabled) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = selectionBackground
    g2.draw(rubberBand)
    g2.composite = ALPHA
    g2.paint = rubberBandColor
    g2.fill(rubberBand)
    g2.dispose()
  }

  private inner class RubberBandingListener : MouseInputAdapter() {
    private val srcPoint = Point()

    override fun mouseDragged(e: MouseEvent) {
      val l = (e.component as? JList<*>)?.takeUnless { it.dragEnabled } ?: return
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
      (e.component as? JList<*>)?.also {
        rubberBand.reset()
        it.isFocusable = true
        it.dragEnabled = it.selectedIndices.isNotEmpty()
        it.repaint()
      }
    }

    override fun mousePressed(e: MouseEvent) {
      val l = e.component as? JList<*> ?: return
      val index = l.locationToIndex(e.point)
      val rect = l.getCellBounds(index, index)
      if (rect.contains(e.point)) {
        l.isFocusable = true
        if (l.dragEnabled) {
          return
        }
        l.setSelectedIndex(index)
      } else {
        l.clearSelection()
        l.selectionModel.anchorSelectionIndex = -1
        l.selectionModel.leadSelectionIndex = -1
        l.isFocusable = false
        l.setDragEnabled(false)
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
      return when {
        r > g -> if (r > b) Color(r, 0, 0) else Color(0, 0, b)
        else -> if (g > b) Color(0, g, 0) else Color(0, 0, b)
      }
    }
  }
}

private class SelectedImageFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int) =
    argb and 0xFF_FF_FF_00.toInt() or (argb and 0xFF shr 1)
}

private class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val renderer = JPanel(BorderLayout())
  private val icon = JLabel(null as? Icon?, SwingConstants.CENTER)
  private val label = JLabel("", SwingConstants.CENTER)
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = UIManager.getBorder("List.noFocusBorder") ?: getNimbusNoFocusBorder()

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
    return renderer
  }
}

private open class ListItemTransferHandler : TransferHandler() {
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  init {
    LABEL.isOpaque = true
    LABEL.border = BorderFactory.createLineBorder(Color.GRAY)
    LABEL.horizontalAlignment = SwingConstants.CENTER
    LABEL.foreground = Color.WHITE
    LABEL.background = Color(0, 0, 255, 200)
  }

  override fun createTransferable(c: JComponent): Transferable? {
    val source = c as? JList<*> ?: return null
    c.rootPane.glassPane.isVisible = true
    source.selectedIndices.forEach { selectedIndices.add(it) }
    val transferredObjects = source.selectedValuesList
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor)) {
          transferredObjects
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(info: TransferSupport) =
    info.isDrop && info.isDataFlavorSupported(FLAVOR)

  override fun getSourceActions(c: JComponent): Int {
    println("getSourceActions")
    c.rootPane.glassPane.cursor = DragSource.DefaultMoveDrop
    if (c is JList<*>) {
      dragImage = createDragImage(c)
      c.getMousePosition()?.also { dragImageOffset = it }
      return MOVE // TransferHandler.COPY_OR_MOVE;
    }
    return NONE
  }

  override fun importData(info: TransferSupport): Boolean {
    val dl = info.dropLocation
    val target = info.component
    if (dl !is JList.DropLocation || target !is JList<*>) {
      return false
    }
    @Suppress("UNCHECKED_CAST")
    val listModel = target.model as DefaultListModel<Any>
    val max = listModel.size
    var index = dl.index.takeIf { it in 0 until max } ?: max
    addIndex = index
    val values = runCatching {
      info.transferable.getTransferData(FLAVOR) as? List<*>
    }.getOrNull().orEmpty()
    for (o in values) {
      val i = index++
      listModel.add(i, o)
      target.addSelectionInterval(i, i)
    }
    addCount = values.size
    return addCount > 0
  }

  override fun exportDone(c: JComponent, data: Transferable, action: Int) {
    println("exportDone")
    val glassPane = c.rootPane.glassPane
    glassPane.isVisible = false
    cleanup(c, action == MOVE)
  }

  private fun cleanup(c: JComponent, remove: Boolean) {
    if (remove && selectedIndices.isNotEmpty()) {
      val selectedList = when {
        addCount > 0 -> selectedIndices.map { if (it >= addIndex) it + addCount else it }
        else -> selectedIndices.toList()
      }
      ((c as? JList<*>)?.model as? DefaultListModel<*>)?.also { model ->
        for (i in selectedList.indices.reversed()) {
          model.remove(selectedList[i])
        }
      }
    }
    selectedIndices.clear()
    addCount = 0
    addIndex = -1
  }

  private fun <E> createDragImage(source: JList<E>): BufferedImage {
    val w = source.width
    val h = source.height
    val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g2 = bi.createGraphics()
    val renderer = source.cellRenderer
    for (i in source.selectedIndices) {
      val c =
        renderer.getListCellRendererComponent(source, source.model.getElementAt(i), i, false, false)
      SwingUtilities.paintComponent(g2, c, source, source.getCellBounds(i, i))
    }
    g2.dispose()
    return bi
  }

  companion object {
    private val FLAVOR = DataFlavor(MutableList::class.java, "List of items")
    val LABEL = object : JLabel() {
      override fun getPreferredSize(): Dimension {
        val d = super.getPreferredSize()
        d.width = 32
        return d
      }
    }
  }
}

private class CompactListItemTransferHandler : ListItemTransferHandler() {
  override fun getSourceActions(c: JComponent): Int {
    println("getSourceActions")
    val glassPane = c.rootPane.glassPane
    glassPane.cursor = DragSource.DefaultMoveDrop
    if (c !is JList<*>) {
      return NONE
    }
    val w = c.fixedCellWidth
    val h = c.fixedCellHeight - 20
    dragImage = createCompactDragImage(c, w, h)
    dragImageOffset = Point(w / 2, h)
    return MOVE // TransferHandler.COPY_OR_MOVE;
  }

  private fun <E> createCompactDragImage(source: JList<E>, w: Int, h: Int): BufferedImage {
    require(!(w <= 0 || h <= 0)) { "width and height must be > 0" }
    val selectedIndices = source.selectedIndices
    val br = source.graphicsConfiguration.createCompatibleImage(w, h, Transparency.TRANSLUCENT)
    val g2 = br.createGraphics()
    val renderer = source.cellRenderer
    val idx = selectedIndices[0]
    val valueAt = source.model.getElementAt(idx)
    val c = renderer.getListCellRendererComponent(source, valueAt, idx, false, false)
    val rect = source.getCellBounds(idx, idx)
    SwingUtilities.paintComponent(g2, c, source, 0, 0, rect.width, rect.height)
    val selectedCount = selectedIndices.size
    val isMoreThanOneItemSelected = selectedCount > 1
    if (isMoreThanOneItemSelected) {
      LABEL.text = selectedCount.toString()
      val d = LABEL.preferredSize
      SwingUtilities.paintComponent(
        g2,
        LABEL,
        source,
        (w - d.width) / 2,
        (h - d.height) / 2,
        d.width,
        d.height
      )
    }
    g2.dispose()
    br.coerceData(true)
    return br
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
