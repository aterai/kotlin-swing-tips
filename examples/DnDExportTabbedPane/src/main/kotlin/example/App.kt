package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.IOException
import javax.swing.*
import javax.swing.plaf.metal.MetalTabbedPaneUI

fun createUI(): Component {
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

  val sub2 = DnDTabbedPane().also {
    it.addTab("Title aaa", JLabel("aaa"))
    it.addTab("Title bbb", JScrollPane(JTree()))
    it.addTab("Title ccc", JScrollPane(JTextArea("JTextArea ccc")))
  }

  tabbedPane.name = "JTabbedPane#main"
  sub.name = "JTabbedPane#sub1"
  sub2.name = "JTabbedPane#sub2"

  val listener = TabDropTargetAdapter()
  val handler = TabTransferHandler()
  listOf(tabbedPane, sub, sub2).forEach { tp ->
    tp.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
    tp.transferHandler = handler
    runCatching {
      tp.dropTarget.addDropTargetListener(listener)
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
  }

  val p = JPanel(GridLayout(2, 1))
  p.add(tabbedPane)
  p.add(sub2)

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(createCheckBoxPanel(tabbedPane), BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createCheckBoxPanel(tabs: JTabbedPane): Component {
  val tc = JCheckBox("Top", true)
  tc.addActionListener {
    tabs.tabPlacement = if (tc.isSelected) {
      SwingConstants.TOP
    } else {
      SwingConstants.RIGHT
    }
  }
  val sc = JCheckBox("SCROLL_TAB_LAYOUT", true)
  sc.addActionListener {
    tabs.tabLayoutPolicy = if (sc.isSelected) {
      JTabbedPane.SCROLL_TAB_LAYOUT
    } else {
      JTabbedPane.WRAP_TAB_LAYOUT
    }
  }
  return JPanel(FlowLayout(FlowLayout.LEFT)).also {
    it.add(tc)
    it.add(sc)
  }
}

class DnDTabbedPane : JTabbedPane() {
  var dragTabIndex = -1
  var dropLocation: DropLocation? = null
  val tabAreaBounds: Rectangle
    get() {
      // Start from the component coordinate system
      // instead of bounds + translate(-x, -y).
      val tabbedRect = Rectangle(size)
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
      return tabbedRect
    }

  class DropLocation(
    pt: Point,
    val index: Int,
  ) : TransferHandler.DropLocation(pt)

  init {
    val h = Handler()
    addMouseListener(h)
    addMouseMotionListener(h)
    addPropertyChangeListener(h)
  }

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
    val button = if (dir == ScrollDirection.FORWARD) {
      forwardButton
    } else {
      backwardButton
    }
    button?.takeIf { it.isEnabled }?.doClick()
  }

  fun autoScrollTest(pt: Point) {
    val r = tabAreaBounds
    val backward = Rectangle()
    val forward = Rectangle()
    if (isTopBottomTabPlacement(getTabPlacement())) {
      backward.setBounds(r.x, r.y, SCROLL_SIZE, r.height)
      forward.setBounds(
        r.x + r.width - SCROLL_SIZE - BUTTON_SIZE,
        r.y,
        SCROLL_SIZE + BUTTON_SIZE,
        r.height,
      )
    } else { // if (tabPlacement == LEFT || tabPlacement == RIGHT) {
      backward.setBounds(r.x, r.y, r.width, SCROLL_SIZE)
      forward.setBounds(
        r.x,
        r.y + r.height - SCROLL_SIZE - BUTTON_SIZE,
        r.width,
        SCROLL_SIZE + BUTTON_SIZE,
      )
    }
    if (backward.contains(pt)) {
      clickArrowButton(ScrollDirection.BACKWARD)
    } else if (forward.contains(pt)) {
      clickArrowButton(ScrollDirection.FORWARD)
    }
  }

  // Test whether the point is in the first half of the tab:
  // the left half for TOP/BOTTOM, the upper half for LEFT/RIGHT.
  private fun isFirstHalf(r: Rectangle, pt: Point) =
    if (isTopBottomTabPlacement(getTabPlacement())) {
      pt.getX() <= r.centerX
    } else {
      pt.getY() <= r.centerY
    }

  fun tabDropLocationForPoint(p: Point): DropLocation {
    val count = tabCount
    // firstOrNull is short-circuiting, so isFirstHalf(...) is evaluated
    // only for the first tab that contains the point.
    val i = (0..<count).firstOrNull { getBoundsAt(it).contains(p) }
    val idx = i?.let { if (isFirstHalf(getBoundsAt(it), p)) it else it + 1 }
      ?: if (count == 0) -1 else count
    return DropLocation(p, idx)
  }

  fun updateTabDropLocation(
    location: DropLocation?,
    forDrop: Boolean,
  ): Any? {
    val old = dropLocation
    dropLocation = location?.takeIf { forDrop } ?: DropLocation(Point(), -1)
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
    val tab = getTabComponentAt(dragIndex)
    remove(dragIndex)
    target.insertTab(title, icon, cmp, toolTipText, targetIndex)
    target.setEnabledAt(targetIndex, isEnabled)
    target.setTabComponentAt(targetIndex, tab)
    target.selectedIndex = targetIndex
    (tab as? JComponent)?.also {
      it.scrollRectToVisible(it.bounds)
    }
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
  }

  private inner class Handler :
    MouseAdapter(),
    PropertyChangeListener { // , BeforeDrag
    private var startPt: Point? = null
    private val gestureMotionThreshold = DragSource.getDragThreshold()

    private fun repaintDropLocation() {
      (rootPane.glassPane as? GhostGlassPane)?.also {
        it.setTargetTabbedPane(this@DnDTabbedPane)
        it.repaint()
      }
    }

    // PropertyChangeListener
    override fun propertyChange(e: PropertyChangeEvent) {
      val propertyName = e.propertyName
      if ("dropLocation" == propertyName) {
        repaintDropLocation()
      }
    }

    // MouseListener
    override fun mousePressed(e: MouseEvent) {
      val src = e.component as? DnDTabbedPane ?: return
      val isOnlyOneTab = src.tabCount <= 1
      if (isOnlyOneTab) {
        startPt = null
        return
      }
      val tabPt = e.point // e.getDragOrigin()
      val idx = src.indexAtLocation(tabPt.x, tabPt.y)
      val flag = idx < 0 || !src.isEnabledAt(idx) || src.getComponentAt(idx) == null
      startPt = if (flag) null else tabPt
    }

    override fun mouseDragged(e: MouseEvent) {
      val tabPt = e.point // e.getDragOrigin()
      val src = e.component
      val pt = startPt ?: return
      if (tabPt.distance(startPt) > gestureMotionThreshold && src is DnDTabbedPane) {
        val th = src.transferHandler
        val idx = src.indexAtLocation(pt.x, pt.y)
        val selIdx = src.selectedIndex
        val isWrapTabLayout = src.tabLayoutPolicy == WRAP_TAB_LAYOUT
        val isNotMetal = src.ui !is MetalTabbedPaneUI
        val isTabRunsRotated = isNotMetal && isWrapTabLayout
        dragTabIndex = if (isTabRunsRotated && idx != selIdx) selIdx else idx
        th.exportAsDrag(src, e, TransferHandler.MOVE)
        src.rootPane.glassPane.isVisible = true
        src.updateTabDropLocation(DropLocation(tabPt, -1), true)
        startPt = null
      }
    }
  }

  private fun isTopBottomTabPlacement(tp: Int) = tp == TOP || tp == BOTTOM

  companion object {
    private const val SCROLL_SIZE = 20 // Test
    private const val BUTTON_SIZE = 30 // 30 is magic number of buttons
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

  override fun createTransferable(c: JComponent): Transferable {
    val src = c as? DnDTabbedPane
    source = src
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(localObjectFlavor)

      override fun isDataFlavorSupported(flavor: DataFlavor) =
        localObjectFlavor == flavor

      @Throws(UnsupportedFlavorException::class, IOException::class)
      override fun getTransferData(
        flavor: DataFlavor,
      ) = if (isDataFlavorSupported(flavor) && src != null) {
        DnDTabData(src)
      } else {
        throw UnsupportedFlavorException(flavor)
      }
    }
  }

  override fun canImport(support: TransferSupport): Boolean {
    val isDrop = support.isDrop
    val isFlavorSupported = support.isDataFlavorSupported(localObjectFlavor)
    val tgt = support.component as? DnDTabbedPane
    if (!isDrop || !isFlavorSupported || tgt == null) {
      return false
    }
    support.dropAction = MOVE
    val tdl = support.dropLocation
    val pt = tdl.dropPoint
    tgt.autoScrollTest(pt)
    val dl = tgt.tabDropLocationForPoint(pt)
    val idx = dl.index

    val isAreaContains = tgt.tabAreaBounds.contains(pt) && idx >= 0
    val canDrop = if (tgt == source) {
      isAreaContains && idx != tgt.dragTabIndex && idx != tgt.dragTabIndex + 1
    } else {
      source?.let { !it.isAncestorOf(tgt) } ?: false && isAreaContains
    }

    // [JDK-6700748]
    // Cursor flickering during D&D when using CellRendererPane with validation - Java Bug System
    // https://bugs.openjdk.org/browse/JDK-6700748
    val cursor = if (canDrop) {
      DragSource.DefaultMoveDrop
    } else {
      DragSource.DefaultMoveNoDrop
    }
    val glassPane = tgt.rootPane.glassPane
    glassPane.cursor = cursor
    tgt.cursor = cursor

    support.setShowDropLocation(canDrop)
    // dl.canDrop = canDrop
    tgt.updateTabDropLocation(dl, canDrop)
    return canDrop
  }

  private fun createDragTabImage(tabs: DnDTabbedPane): BufferedImage {
    val rect = tabs.getBoundsAt(tabs.dragTabIndex).intersection(Rectangle(tabs.size))
    val w = maxOf(1, rect.width)
    val h = maxOf(1, rect.height)
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g2 = image.createGraphics()
    // The clip of this Graphics2D is the tab bounds, so the children
    // outside of it are skipped and no subimage is needed.
    g2.translate(-rect.x, -rect.y)
    tabs.paint(g2)
    g2.dispose()
    return image
  }

  override fun getSourceActions(c: JComponent): Int {
    val src = c as? DnDTabbedPane ?: return NONE
    src.rootPane.glassPane = GhostGlassPane(src)
    return if (src.dragTabIndex < 0) {
      NONE
    } else {
      dragImage = createDragTabImage(src)
      src.rootPane.glassPane.isVisible = true
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
    src.rootPane.glassPane.isVisible = false
    src.updateTabDropLocation(null, false)
    src.repaint()
    src.cursor = Cursor.getDefaultCursor()
  }
}

private class GhostGlassPane(
  private var tabbedPane: DnDTabbedPane,
) : JComponent() {
  override fun isOpaque() = false

  fun setTargetTabbedPane(tab: DnDTabbedPane) {
    tabbedPane = tab
  }

  override fun paintComponent(g: Graphics) {
    getDropLineRect()?.also { rect ->
      val g2 = g.create() as? Graphics2D ?: return
      val r = SwingUtilities.convertRectangle(tabbedPane, rect, this)
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f)
      g2.paint = Color.RED
      g2.fill(r)
      g2.dispose()
    }
  }

  fun getDropLineRect(): Rectangle? {
    val index = tabbedPane.dropLocation?.index ?: -1
    if (index < 0) {
      return null
    }
    val a = minOf(index, 1) // index == 0 ? 0 : 1
    val r = tabbedPane.getBoundsAt(maxOf(index - 1, 0))
    val tp = tabbedPane.tabPlacement
    val rect = if (tp == JTabbedPane.TOP || tp == JTabbedPane.BOTTOM) {
      Rectangle(r.x - LINE_SIZE / 2 + r.width * a, r.y, LINE_SIZE, r.height)
    } else {
      Rectangle(r.x, r.y - LINE_SIZE / 2 + r.height * a, r.width, LINE_SIZE)
    }
    return rect.takeUnless { it.isEmpty }
  }

  companion object {
    private const val LINE_SIZE = 3
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
