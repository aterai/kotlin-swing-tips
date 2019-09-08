package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import java.io.IOException
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

class MainPanel : JPanel(BorderLayout()) {
  init {
    val model = DefaultListModel<ListItem>().also {
      // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
      it.addElement(ListItem("11111111", "wi0009-32.png"))
      it.addElement(ListItem("12345", "wi0054-32.png"))
      it.addElement(ListItem("222222.asd", "wi0062-32.png"))
      it.addElement(ListItem("test", "wi0063-32.png"))
      it.addElement(ListItem("32.png", "wi0064-32.png"))
      it.addElement(ListItem("33333.jpg", "wi0096-32.png"))
      it.addElement(ListItem("6896", "wi0111-32.png"))
      it.addElement(ListItem("t467467est", "wi0122-32.png"))
      it.addElement(ListItem("test123", "wi0124-32.png"))
      it.addElement(ListItem("test(1)", "wi0126-32.png"))
    }
    val list = ReorderingList(model)
    list.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    add(JScrollPane(list))
    setPreferredSize(Dimension(320, 240))
  }
}

data class ListItem(val title: String, val iconFile: String) {
  val nicon = ImageIcon(javaClass.getResource(iconFile))
  val sicon: ImageIcon

  init {
    val ip = FilteredImageSource(nicon.getImage().getSource(), SelectedImageFilter())
    this.sicon = ImageIcon(Toolkit.getDefaultToolkit().createImage(ip))
  }
}

class ReorderingList(model: ListModel<ListItem>) : JList<ListItem>(model) {
  private var rbl: MouseInputListener? = null
  private var rubberBandColor: Color? = null
  private val rubberBand: Path2D = Path2D.Double()

  override fun updateUI() {
    setSelectionForeground(null) // Nimbus
    setSelectionBackground(null) // Nimbus
    setCellRenderer(null)
    setTransferHandler(null)
    removeMouseListener(rbl)
    removeMouseMotionListener(rbl)
    super.updateUI()

    rubberBandColor = makeRubberBandColor(getSelectionBackground())
    setLayoutOrientation(HORIZONTAL_WRAP)
    setVisibleRowCount(0)
    setFixedCellWidth(62)
    setFixedCellHeight(62)
    // setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))

    setCellRenderer(ListItemListCellRenderer())
    rbl = RubberBandingListener()
    addMouseMotionListener(rbl)
    addMouseListener(rbl)

    // putClientProperty("List.isFileList", Boolean.TRUE)
    getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
    setTransferHandler(ListItemTransferHandler())
    setDropMode(DropMode.INSERT)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (getDragEnabled()) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setPaint(getSelectionBackground())
    g2.draw(rubberBand)
    g2.setComposite(ALPHA)
    g2.setPaint(rubberBandColor)
    g2.fill(rubberBand)
    g2.dispose()
  }

  private inner class RubberBandingListener : MouseInputAdapter() {
    private val srcPoint = Point()

    override fun mouseDragged(e: MouseEvent) {
      val l = (e.getComponent() as? JList<*>)?.takeUnless { it.getDragEnabled() } ?: return
      val destPoint = e.getPoint()
      rubberBand.reset()
      rubberBand.moveTo(srcPoint.getX(), srcPoint.getY())
      rubberBand.lineTo(destPoint.getX(), srcPoint.getY())
      rubberBand.lineTo(destPoint.getX(), destPoint.getY())
      rubberBand.lineTo(srcPoint.getX(), destPoint.getY())
      rubberBand.closePath()

      val indices = (0 until l.getModel().getSize())
        .filter { rubberBand.intersects(l.getCellBounds(it, it)) }.toIntArray()
      l.setSelectedIndices(indices)
      l.repaint()
    }

    override fun mouseReleased(e: MouseEvent) {
      (e.getComponent() as? JList<*>)?.also {
        rubberBand.reset()
        it.setFocusable(true)
        it.setDragEnabled(it.getSelectedIndices().isNotEmpty())
        it.repaint()
      }
    }

    override fun mousePressed(e: MouseEvent) {
      val l = e.getComponent() as? JList<*> ?: return
      val index = l.locationToIndex(e.getPoint())
      val rect = l.getCellBounds(index, index)
      if (rect.contains(e.getPoint())) {
        l.setFocusable(true)
        if (l.getDragEnabled()) {
          return
        }
        l.setSelectedIndex(index)
      } else {
        l.clearSelection()
        l.getSelectionModel().setAnchorSelectionIndex(-1)
        l.getSelectionModel().setLeadSelectionIndex(-1)
        l.setFocusable(false)
        l.setDragEnabled(false)
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
      icon.setIcon(value.sicon)
      label.setForeground(list.getSelectionForeground())
      label.setBackground(list.getSelectionBackground())
      label.setOpaque(true)
    } else {
      icon.setIcon(value.nicon)
      label.setForeground(list.getForeground())
      label.setBackground(list.getBackground())
      label.setOpaque(false)
    }
    return renderer
  }
}

// Demo - BasicDnD (The Java™ Tutorials > Creating a GUI With JFC/Swing > Drag and Drop and Data Transfer)
// https://docs.oracle.com/javase/tutorial/uiswing/dnd/basicdemo.html
class ListItemTransferHandler : TransferHandler() {
  val localObjectFlavor = DataFlavor(List::class.java, "List of items")
  // var indices: IntArray? = null
  private val selectedIndices = mutableListOf<Int>()
  private var addIndex = -1 // Location where items were added
  private var addCount = 0 // Number of items added.

  override fun createTransferable(c: JComponent): Transferable? {
    val source = c as? JList<*> ?: return null
    c.getRootPane().getGlassPane().setVisible(true)
    source.getSelectedIndices().forEach { selectedIndices.add(it) }
    val transferObjects = source.getSelectedValuesList()
    // return DataHandler(transferObjects, localObjectFlavor.getMimeType())
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(localObjectFlavor)

      override fun isDataFlavorSupported(flavor: DataFlavor) = localObjectFlavor == flavor

      @Throws(UnsupportedFlavorException::class, IOException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor)) {
          transferObjects
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(info: TransferSupport) =
      info.isDrop() &&
      info.isDataFlavorSupported(localObjectFlavor) &&
      info.getDropLocation() is JList.DropLocation

  override fun getSourceActions(c: JComponent): Int {
    println("getSourceActions")
    c.getRootPane().getGlassPane().setCursor(DragSource.DefaultMoveDrop)
    return MOVE // COPY_OR_MOVE
  }

  override fun importData(info: TransferSupport): Boolean {
    val dl = info.getDropLocation()
    val target = info.getComponent()
    if (!canImport(info) || dl !is JList.DropLocation || target !is JList<*>) {
      return false
    }
    @Suppress("UNCHECKED_CAST")
    val listModel = target.getModel() as DefaultListModel<Any>
    val max = listModel.getSize()
    var index = dl.getIndex().takeIf { it in 0 until max } ?: max
    addIndex = index
    val values = runCatching {
      info.getTransferable().getTransferData(localObjectFlavor) as? List<*>
    }.getOrNull() ?: emptyList<Any>()
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
    val glassPane = c.getRootPane().getGlassPane()
    // glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
    glassPane.setVisible(false)
    cleanup(c, action == MOVE)
  }

  private fun cleanup(c: JComponent, remove: Boolean) {
    if (remove && selectedIndices.isNotEmpty()) {
      // If we are moving items around in the same list, we
      // need to adjust the indices accordingly, since those
      // after the insertion point have moved.
      // if (addCount > 0) {
      //   for (i in selectedIndices.indices) {
      //     if (selectedIndices[i] >= addIndex) {
      //       selectedIndices[i] += addCount
      //     }
      //   }
      // }
      val selectedList = when {
        addCount > 0 -> selectedIndices.map { if (it >= addIndex) it + addCount else it }
        else -> selectedIndices.toList()
      }
      ((c as? JList<*>)?.getModel() as? DefaultListModel<*>)?.also { model ->
        for (i in selectedList.indices.reversed()) {
          model.remove(selectedList[i])
        }
      }
    }
    selectedIndices.clear()
    addCount = 0
    addIndex = -1
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
