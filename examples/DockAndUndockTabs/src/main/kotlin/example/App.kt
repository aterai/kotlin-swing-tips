package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.File
import java.io.IOException
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicButtonUI
import javax.swing.plaf.metal.MetalTabbedPaneUI

fun makeUI(): Component {
  val sub = DnDTabbedPane().also {
    it.addTab("Title aa", JLabel("aaa"))
    it.addTab("Title bb", JScrollPane(JTree()))
    it.addTab("Title cc", JScrollPane(JTextArea("JTextArea cc")))
  }

  val tabbedPane = DnDTabbedPane().also {
    it.addTab("JTree 00", JScrollPane(JTree()))
    it.addTab("JLabel 01", JLabel("Test"))
    it.addTab("JTable 02", JScrollPane(JTable(10, 3)))
    it.addTab("JTextArea 03", JScrollPane(JTextArea("JTextArea 03")))
    it.addTab("JLabel 04", JLabel("<html>1111111111111111<br>13412341234123446745"))
    it.addTab("null 05", null)
    it.addTab("JTabbedPane 06", sub)
    it.addTab("Title 000000000000000007", JScrollPane(JTree()))
  }
  for (i in 0..<tabbedPane.tabCount) {
    setTabComponent(tabbedPane, i)
  }

  tabbedPane.name = "JTabbedPane#main"
  sub.name = "JTabbedPane#sub1"

  val handler = TabTransferHandler()
  val layerUI = DropLocationLayerUI()
  val listener = TabDropTargetAdapter()
  // val handler = TabTransferHandler()
  listOf(tabbedPane, sub).forEach { tp ->
    tp.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
    tp.transferHandler = handler
    runCatching {
      tp.dropTarget.addDropTargetListener(listener)
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
  }

  val p = JPanel(BorderLayout())
  p.add(JLayer(tabbedPane, layerUI))
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun setTabComponent(
  tabbedPane: JTabbedPane,
  i: Int,
) {
  tabbedPane.setTabComponentAt(i, ButtonTabComponent(tabbedPane))
  tabbedPane.setToolTipTextAt(i, "tooltip: $i")
}

class DnDTabbedPane : JTabbedPane() {
  var dragTabIndex = -1
  var dropLocation: DropLocation? = null
  val pointOnScreen = Point(-1, -1)
  val tabAreaBounds: Rectangle
    get() {
      val tabbedRect = bounds
      val xx = tabbedRect.x
      val yy = tabbedRect.y
      val compRect = selectedComponent?.bounds ?: Rectangle()
      val tabPlacement = getTabPlacement()
      if (isTopBottomTabPlacement(tabPlacement)) {
        tabbedRect.height -= compRect.height
        if (tabPlacement == BOTTOM) {
          tabbedRect.y += compRect.y + compRect.height
        }
      } else {
        tabbedRect.width -= compRect.width
        if (tabPlacement == RIGHT) {
          tabbedRect.x += compRect.x + compRect.width
        }
      }
      tabbedRect.translate(-xx, -yy)
      return tabbedRect
    }

  init {
    val h = Handler()
    addMouseListener(h)
    addMouseMotionListener(h)
    addPropertyChangeListener(h)
  }

  class DropLocation(
    pt: Point,
    val index: Int,
  ) : TransferHandler.DropLocation(pt)

  private enum class ScrollDirection { FORWARD, BACKWARD }

  private fun clickArrowButton(dir: ScrollDirection) {
    var forwardButton: JButton? = null
    var backwardButton: JButton? = null
    for (c in components) {
      val b = c as? JButton ?: continue
      if (forwardButton == null) {
        forwardButton = b
      } else if (backwardButton == null) {
        backwardButton = b
      }
    }
    val button = if (dir == ScrollDirection.FORWARD) forwardButton else backwardButton
    button?.takeIf { it.isEnabled }?.doClick()
  }

  fun autoScrollTest(pt: Point) {
    val r = tabAreaBounds
    if (isTopBottomTabPlacement(getTabPlacement())) {
      RECT_BACKWARD.setBounds(r.x, r.y, SCROLL_SIZE, r.height)
      RECT_FORWARD.setBounds(
        r.x + r.width - SCROLL_SIZE - BUTTON_SIZE,
        r.y,
        SCROLL_SIZE + BUTTON_SIZE,
        r.height,
      )
    } else { // if (tabPlacement == LEFT || tabPlacement == RIGHT) {
      RECT_BACKWARD.setBounds(r.x, r.y, r.width, SCROLL_SIZE)
      RECT_FORWARD.setBounds(
        r.x,
        r.y + r.height - SCROLL_SIZE - BUTTON_SIZE,
        r.width,
        SCROLL_SIZE + BUTTON_SIZE,
      )
    }
    if (RECT_BACKWARD.contains(pt)) {
      clickArrowButton(ScrollDirection.BACKWARD)
    } else if (RECT_FORWARD.contains(pt)) {
      clickArrowButton(ScrollDirection.FORWARD)
    }
  }

  fun tabDropLocationForPoint(p: Point): DropLocation {
    for (i in 0..<tabCount) {
      if (getBoundsAt(i).contains(p)) {
        return DropLocation(p, i)
      }
    }
    val idx = if (tabAreaBounds.contains(p)) tabCount else -1
    return DropLocation(p, idx)
  }

  fun updateTabDropLocation(
    location: DropLocation?,
    forDrop: Boolean,
  ): Any? {
    val old = dropLocation
    dropLocation = if (location == null || !forDrop) {
      DropLocation(Point(), -1)
    } else {
      location
    }
    firePropertyChange("dropLocation", old, dropLocation)
    return null
  }

  fun exportTab(
    dragIndex: Int,
    target: JTabbedPane,
    targetIndex: Int,
  ) {
    val cmp = getComponentAt(dragIndex)
    val title = getTitleAt(dragIndex)
    val icon = getIconAt(dragIndex)
    val toolTipText = getToolTipTextAt(dragIndex)
    val isEnabled = isEnabledAt(dragIndex)
    var tab = getTabComponentAt(dragIndex)
    if (tab is ButtonTabComponent) {
      tab = ButtonTabComponent(target)
    }
    remove(dragIndex)
    target.insertTab(title, icon, cmp, toolTipText, targetIndex)
    target.setEnabledAt(targetIndex, isEnabled)
    target.setTabComponentAt(targetIndex, tab)
    target.selectedIndex = targetIndex
    (tab as? JComponent)?.also {
      it.scrollRectToVisible(it.bounds)
    }
    pointOnScreen.setLocation(-1, -1)
  }

  fun convertTab(
    prev: Int,
    next: Int,
  ) {
    val cmp = getComponentAt(prev)
    val tab = getTabComponentAt(prev)
    val title = getTitleAt(prev)
    val icon = getIconAt(prev)
    val toolTipText = getToolTipTextAt(prev)
    val isEnabled = isEnabledAt(prev)
    val tgtIndex = if (prev > next) next else next - 1
    remove(prev)
    insertTab(title, icon, cmp, toolTipText, tgtIndex)
    setEnabledAt(tgtIndex, isEnabled)
    if (isEnabled) {
      selectedIndex = tgtIndex
    }
    setTabComponentAt(tgtIndex, tab)
    pointOnScreen.setLocation(-1, -1)
  }

  private inner class Handler :
    MouseAdapter(),
    PropertyChangeListener { // , BeforeDrag
    private var startPt: Point? = null
    private val dragThreshold = DragSource.getDragThreshold()

    // PropertyChangeListener
    override fun propertyChange(e: PropertyChangeEvent) {
      val propertyName = e.propertyName
      if ("dropLocation" == propertyName) {
        repaint()
      }
    }

    // MouseListener
    override fun mousePressed(e: MouseEvent) {
      val src = e.component as? DnDTabbedPane ?: return
      val tabPt = e.point // e.getDragOrigin()
      val idx = src.indexAtLocation(tabPt.x, tabPt.y)
      val flag = idx < 0 || !src.isEnabledAt(idx) || src.getComponentAt(idx) == null
      startPt = if (flag) null else tabPt
    }

    override fun mouseDragged(e: MouseEvent) {
      val tabPt = e.point // e.getDragOrigin()
      val pt = startPt ?: return
      (e.component as? DnDTabbedPane)
        ?.takeIf { tabPt.distance(startPt) > dragThreshold }
        ?.also {
          val th = it.transferHandler
          val idx = it.indexAtLocation(pt.x, pt.y)
          val selIdx = it.selectedIndex
          val isRotate = it.tabLayoutPolicy == WRAP_TAB_LAYOUT && idx != selIdx
          dragTabIndex = if (it.ui !is MetalTabbedPaneUI && isRotate) selIdx else idx
          th.exportAsDrag(it, e, TransferHandler.MOVE)
          startPt = null
        }
    }
  }

  private fun isTopBottomTabPlacement(tp: Int) = tp == TOP || tp == BOTTOM

  companion object {
    private const val SCROLL_SIZE = 20 // Test
    private const val BUTTON_SIZE = 30 // XXX 30 is magic number of scroll button size

    private val RECT_BACKWARD = Rectangle()
    private val RECT_FORWARD = Rectangle()
  }
}

private class TabDropTargetAdapter : DropTargetAdapter() {
  private fun clearDropLocationPaint(c: Component) {
    val t = c as? DnDTabbedPane ?: return
    t.updateTabDropLocation(null, false)
    t.cursor = Cursor.getDefaultCursor()
  }

  override fun drop(e: DropTargetDropEvent) {
    clearDropLocationPaint(e.dropTargetContext.component)
  }

  override fun dragExit(e: DropTargetEvent) {
    clearDropLocationPaint(e.dropTargetContext.component)
  }

  // override fun dragEnter(e: DropTargetDragEvent) {
  //   println("DropTargetListener#dragEnter: ${e.dropTargetContext.component.name}")
  // }

  // override dragOver(e: DropTargetDragEvent) {
  //   println("dragOver")
  // }

  // override dropActionChanged(e: DropTargetDragEvent) {
  //   println("dropActionChanged")
  // }
}

private data class DnDTabData(
  val tabbedPane: DnDTabbedPane,
)

private class TabTransferHandler : TransferHandler() {
  private val localObjectFlavor = DataFlavor(DnDTabData::class.java, "DnDTabData")
  private var source: DnDTabbedPane? = null
  private val label = object : JLabel() {
    override fun contains(
      x: Int,
      y: Int,
    ) = false
  }
  private var dialog: JWindow? = null

  init {
    DragSource.getDefaultDragSource().addDragSourceMotionListener {
      val pt = it.location
      pt.translate(5, 5) // offset
      dialog?.location = pt
      source?.pointOnScreen?.location = pt
    }
  }

  override fun createTransferable(c: JComponent): Transferable {
    val src = c as? DnDTabbedPane
    source = src
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(
        localObjectFlavor,
        DataFlavor.javaFileListFlavor,
      )

      override fun isDataFlavorSupported(flavor: DataFlavor) =
        getTransferDataFlavors().contains(flavor)

      @Throws(UnsupportedFlavorException::class, IOException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        val flavors = transferDataFlavors
        return if (flavor.equals(flavors[0]) && src != null) {
          DnDTabData(src)
        } else if (flavor.equals(flavors[1]) && src != null) {
          emptyList<File>()
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(support: TransferSupport): Boolean {
    val target = support.component
    val b = !support.isDrop || !support.isDataFlavorSupported(localObjectFlavor)
    if (b || target !is DnDTabbedPane) {
      return false
    }
    support.dropAction = MOVE
    val tdl = support.dropLocation
    val pt = tdl.dropPoint
    target.autoScrollTest(pt)
    val dl = target.tabDropLocationForPoint(pt)
    val idx = dl.index

    val isAreaContains = target.tabAreaBounds.contains(pt) && idx >= 0
    val canDrop = if (target == source) {
      isAreaContains && idx != target.dragTabIndex && idx != target.dragTabIndex + 1
    } else {
      source?.let { !it.isAncestorOf(target) } ?: false && isAreaContains
    }

    target.cursor = if (canDrop) {
      DragSource.DefaultMoveDrop
    } else {
      DragSource.DefaultMoveNoDrop
    }

    support.setShowDropLocation(canDrop)
    // dl.canDrop = canDrop
    target.updateTabDropLocation(dl, canDrop)
    return canDrop
  }

  private fun makeDragTabImage(tabs: DnDTabbedPane): BufferedImage {
    val rect = tabs.getBoundsAt(tabs.dragTabIndex)
    val image = BufferedImage(tabs.width, tabs.height, BufferedImage.TYPE_INT_ARGB)
    val g2 = image.createGraphics()
    tabs.paint(g2)
    g2.dispose()
    if (rect.x < 0) {
      rect.translate(-rect.x, 0)
    }
    if (rect.y < 0) {
      rect.translate(0, -rect.y)
    }
    if (rect.x + rect.width > image.width) {
      rect.width = image.width - rect.x
    }
    if (rect.y + rect.height > image.height) {
      rect.height = image.height - rect.y
    }
    return image.getSubimage(rect.x, rect.y, rect.width, rect.height)
  }

  override fun getSourceActions(c: JComponent): Int {
    val src = c as? DnDTabbedPane ?: return NONE
    return if (src.dragTabIndex < 0) {
      NONE
    } else {
      label.icon = ImageIcon(makeDragTabImage(src))
      dialog = JWindow().also {
        it.add(label)
        it.opacity = .5f
        it.pack()
        it.isVisible = true
      }
      MOVE
    }
  }

  override fun importData(support: TransferSupport): Boolean {
    val target = support.component
    val data = runCatching {
      support.transferable.getTransferData(localObjectFlavor) as? DnDTabData
    }.getOrNull()
    if (target !is DnDTabbedPane || data == null) {
      return false
    }
    val src = data.tabbedPane
    val index = target.dropLocation?.index ?: -1
    if (target == src) {
      src.convertTab(src.dragTabIndex, index) // getTargetTabIndex(e.getLocation()))
    } else {
      src.exportTab(src.dragTabIndex, target, index)
    }
    return true
  }

  override fun exportDone(
    c: JComponent?,
    data: Transferable?,
    action: Int,
  ) {
    val src = c as? DnDTabbedPane ?: return
    if (src.pointOnScreen.x > 0) {
      createNewFrame(src)
    }
    if (src.tabCount == 0) {
      SwingUtilities.getWindowAncestor(src)?.dispose()
    }
    src.updateTabDropLocation(null, false)
    src.repaint()
    dialog?.dispose()
  }

  private fun createNewFrame(src: DnDTabbedPane) {
    val index = src.dragTabIndex
    val cmp = src.getComponentAt(index)
    val title = src.getTitleAt(index)
    val icon = src.getIconAt(index)
    val tip = src.getToolTipTextAt(index)
    src.remove(index)
    val tabs = DnDTabbedPane()
    tabs.transferHandler = src.transferHandler
    tabs.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
    tabs.addTab(title, icon, cmp, tip)
    tabs.setTabComponentAt(0, ButtonTabComponent(tabs))
    val listener = TabDropTargetAdapter()
    runCatching {
      tabs.dropTarget.addDropTargetListener(listener)
    }.onFailure {
      it.printStackTrace()
      UIManager.getLookAndFeel().provideErrorFeedback(tabs)
    }
    val frame = JFrame()
    frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    frame.contentPane.add(JLayer(tabs, DropLocationLayerUI()))
    frame.setSize(320, 240)
    frame.location = src.pointOnScreen
    frame.isVisible = true
    EventQueue.invokeLater { frame.toFront() }
  }
}

private class DropLocationLayerUI : LayerUI<DnDTabbedPane>() {
  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    val tabbedPane = (c as? JLayer<*>)?.view as? DnDTabbedPane ?: return
    tabbedPane.dropLocation?.takeIf { it.index >= 0 }?.also {
      val g2 = g.create() as? Graphics2D ?: return
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f)
      g2.paint = Color.RED
      initLineRect(tabbedPane, it)
      g2.fill(RECT_LINE)
      g2.dispose()
    }
  }

  private fun initLineRect(
    tabs: JTabbedPane,
    loc: DnDTabbedPane.DropLocation,
  ) {
    val index = loc.index
    val a = minOf(index, 1)
    val r = tabs.getBoundsAt(a * (index - 1))
    val tp = tabs.tabPlacement
    if (tp == JTabbedPane.TOP || tp == JTabbedPane.BOTTOM) {
      RECT_LINE.setBounds(r.x - LINE_SIZE / 2 + r.width * a, r.y, LINE_SIZE, r.height)
    } else {
      RECT_LINE.setBounds(r.x, r.y - LINE_SIZE / 2 + r.height * a, r.width, LINE_SIZE)
    }
  }

  companion object {
    private const val LINE_SIZE = 3
    private val RECT_LINE = Rectangle()
  }
}

private class ButtonTabComponent(
  private val tabbedPane: JTabbedPane,
) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {
  private inner class TabButtonHandler :
    MouseAdapter(),
    ActionListener {
    override fun actionPerformed(e: ActionEvent) {
      val i = tabbedPane.indexOfTabComponent(this@ButtonTabComponent)
      if (i != -1) {
        tabbedPane.remove(i)
      }
    }

    override fun mouseEntered(e: MouseEvent) {
      (e.component as? AbstractButton)?.isBorderPainted = true
    }

    override fun mouseExited(e: MouseEvent) {
      (e.component as? AbstractButton)?.isBorderPainted = false
    }
  }

  init {
    isOpaque = false
    val label = object : JLabel() {
      override fun getText(): String? {
        val i = tabbedPane.indexOfTabComponent(this@ButtonTabComponent)
        return if (i != -1) tabbedPane.getTitleAt(i) else null
      }

      override fun getIcon(): Icon? {
        val i = tabbedPane.indexOfTabComponent(this@ButtonTabComponent)
        return if (i != -1) tabbedPane.getIconAt(i) else null
      }
    }
    add(label)
    label.border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
    val button = TabButton()
    val handler = TabButtonHandler()
    button.addActionListener(handler)
    button.addMouseListener(handler)
    add(button)
    border = BorderFactory.createEmptyBorder(2, 0, 0, 0)
  }
}

private class TabButton : JButton() {
  override fun getPreferredSize() = Dimension(SIZE, SIZE)

  override fun updateUI() {
    // we don't want to update UI for this button
    setUI(BasicButtonUI())
    toolTipText = "close this tab"
    isContentAreaFilled = false
    isFocusable = false
    border = BorderFactory.createEtchedBorder()
    isBorderPainted = false
    isRolloverEnabled = true
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.stroke = BasicStroke(2f)
    g2.paint = Color.BLACK
    if (model.isRollover) {
      g2.paint = Color.ORANGE
    }
    if (model.isPressed) {
      g2.paint = Color.BLUE
    }
    g2.drawLine(DELTA, DELTA, width - DELTA - 1, height - DELTA - 1)
    g2.drawLine(width - DELTA - 1, DELTA, DELTA, height - DELTA - 1)
    g2.dispose()
  }

  companion object {
    private const val SIZE = 17
    private const val DELTA = 6
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
    val frame = JFrame()
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.contentPane.add(makeUI())
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
  }
}
