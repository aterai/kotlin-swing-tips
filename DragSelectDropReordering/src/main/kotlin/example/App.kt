package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

fun makeUI(): Component {
  val model = DefaultListModel<ListItem>().also {
    // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
    it.addElement(ListItem("wi0009-32", "example/wi0009-32.png"))
    it.addElement(ListItem("12345", "example/wi0054-32.png"))
    it.addElement(ListItem("wi0062-32.png", "example/wi0062-32.png"))
    it.addElement(ListItem("test", "example/wi0063-32.png"))
    it.addElement(ListItem("32.png", "example/wi0064-32.png"))
    it.addElement(ListItem("wi0096-32.png", "example/wi0096-32.png"))
    it.addElement(ListItem("6896", "example/wi0111-32.png"))
    it.addElement(ListItem("t467467est", "example/wi0122-32.png"))
    it.addElement(ListItem("test123", "example/wi0124-32.png"))
    it.addElement(ListItem("test(1)", "example/wi0126-32.png"))
  }
  val list = ReorderingList(model)
  list.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
}

private data class ListItem(val title: String, val iconFile: String) {
  val icon: Icon
  val selectedIcon: Icon

  init {
    val img = makeImage(iconFile)
    icon = ImageIcon(img)
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
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
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
        .filter { rubberBand.intersects(l.getCellBounds(it, it)) }
        .toIntArray()
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
      val list = e.component as? JList<*> ?: return
      val index = list.locationToIndex(e.point)
      val rect = list.getCellBounds(index, index)
      if (rect?.contains(e.point) == true) {
        list.isFocusable = true
        if (list.dragEnabled) {
          return
        }
        list.setSelectedIndex(index)
      } else {
        list.clearSelection()
        list.selectionModel.anchorSelectionIndex = -1
        list.selectionModel.leadSelectionIndex = -1
        list.isFocusable = false
        list.setDragEnabled(false)
      }
      srcPoint.location = e.point
      list.repaint()
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
  override fun filterRGB(
    x: Int,
    y: Int,
    argb: Int,
  ) = argb and 0xFF_FF_FF_00.toInt() or (argb and 0xFF shr 1)
}

private class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val renderer = JPanel(BorderLayout())
  private val icon = JLabel(null as? Icon?, SwingConstants.CENTER)
  private val label = JLabel("", SwingConstants.CENTER)
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = getNoFocusBorder(focusBorder)

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

// Demo - BasicDnD (The Java™ Tutorials > Creating a GUI With JFC/Swing > Drag and Drop and Data Transfer)
// https://docs.oracle.com/javase/tutorial/uiswing/dnd/basicdemo.html
private class ListItemTransferHandler : TransferHandler() {
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable {
    c.rootPane.glassPane.isVisible = true
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(FLAVOR)

      override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

      @Throws(UnsupportedFlavorException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        val src = c as? JList<*>
        return if (isDataFlavorSupported(flavor) && src != null) {
          src.selectedIndices.forEach { selectedIndices.add(it) }
          src.selectedValuesList
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(info: TransferSupport) =
    info.isDrop && info.isDataFlavorSupported(FLAVOR)

  override fun getSourceActions(c: JComponent): Int {
    c.rootPane.glassPane.cursor = DragSource.DefaultMoveDrop
    return MOVE // COPY_OR_MOVE
  }

  override fun importData(info: TransferSupport): Boolean {
    val dl = info.dropLocation
    val target = info.component as? JList<*>

    @Suppress("UNCHECKED_CAST")
    val listModel = target?.model as? DefaultListModel<Any>
    if (dl !is JList.DropLocation || listModel == null) {
      return false
    }
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

  override fun exportDone(
    c: JComponent,
    data: Transferable,
    action: Int,
  ) {
    val glassPane = c.rootPane.glassPane
    glassPane.isVisible = false
    cleanup(c, action == MOVE)
  }

  private fun cleanup(
    c: JComponent,
    remove: Boolean,
  ) {
    if (remove && selectedIndices.isNotEmpty()) {
      // If we are moving items around in the same list, we
      // need to adjust the indices accordingly, since those
      // after the insertion point have moved.
      val selectedList = if (addCount > 0) {
        selectedIndices.map { if (it >= addIndex) it + addCount else it }
      } else {
        selectedIndices.toList()
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

  companion object {
    private val FLAVOR = DataFlavor(List::class.java, "List of items")
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
