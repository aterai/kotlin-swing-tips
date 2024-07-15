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
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.IOException
import javax.swing.*
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
    it.add(makeCheckBoxPanel(tabbedPane), BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeCheckBoxPanel(tabs: JTabbedPane): Component {
  val tc = JCheckBox("Top", true)
  tc.addActionListener {
    tabs.tabPlacement = if (tc.isSelected) SwingConstants.TOP else SwingConstants.RIGHT
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

  private fun clickArrowButton(actionKey: String) {
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
    val button = if ("scrollTabsForwardAction" == actionKey) forwardButton else backwardButton
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
      clickArrowButton("scrollTabsBackwardAction")
    } else if (RECT_FORWARD.contains(pt)) {
      clickArrowButton("scrollTabsForwardAction")
    }
  }

  fun tabDropLocationForPoint(p: Point): DropLocation {
    val horiz = isTopBottomTabPlacement(getTabPlacement())
    val idx = (0..<tabCount)
      .map { if (horiz) getHorizontalIndex(it, p) else getVerticalIndex(it, p) }
      .firstOrNull { it >= 0 }
      ?: -1
    return DropLocation(p, idx)
  }

  private fun getHorizontalIndex(i: Int, pt: Point): Int {
    val r = getBoundsAt(i)
    val contains = r.contains(pt)
    val lastTab = i == tabCount - 1
    var idx = -1
    val cr: Rectangle2D = Rectangle2D.Double(r.centerX, r.getY(), .1, r.getHeight())
    val iv = cr.outcode(pt)
    if (cr.contains(pt) || contains && iv and Rectangle2D.OUT_LEFT != 0) {
      // First half.
      idx = i
    } else if ((contains || lastTab) && iv and Rectangle2D.OUT_RIGHT != 0) {
      // Second half.
      idx = i + 1
    }
    return idx
  }

  private fun getVerticalIndex(i: Int, pt: Point): Int {
    val r = getBoundsAt(i)
    val contains = r.contains(pt)
    val lastTab = i == tabCount - 1
    var idx = -1
    val cr = Rectangle2D.Double(r.getX(), r.centerY, r.getWidth(), .1)
    val iv = cr.outcode(pt)
    if (cr.contains(pt) || contains && iv and Rectangle2D.OUT_TOP != 0) {
      // First half.
      idx = i
    } else if ((contains || lastTab) && iv and Rectangle2D.OUT_BOTTOM != 0) {
      // Second half.
      idx = i + 1
    }
    return idx
  }

  fun updateTabDropLocation(
    location: DropLocation?,
    forDrop: Boolean,
  ): Any? {
    val old = dropLocation
    dropLocation = if (location == null || !forDrop) DropLocation(Point(), -1) else location
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
      if (tabPt.distance(startPt) > gestureMotionThreshold && src is DnDTabbedPane) {
        val th = src.transferHandler
        val idx = src.indexAtLocation(tabPt.x, tabPt.y)
        val selIdx = src.selectedIndex
        val isWrapTabLayout = src.tabLayoutPolicy == WRAP_TAB_LAYOUT
        val isNotMetal = src.ui !is MetalTabbedPaneUI
        val isTabRunsRotated = isNotMetal && isWrapTabLayout
        dragTabIndex = if (isTabRunsRotated && idx != selIdx) selIdx else idx
        th.exportAsDrag(src, e, TransferHandler.MOVE)
        // RECT_LINE.setBounds(0, 0, 0, 0)
        src.rootPane.glassPane.isVisible = true
        src.updateTabDropLocation(DropLocation(tabPt, -1), true)
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

  override fun createTransferable(c: JComponent): Transferable {
    val src = c as? DnDTabbedPane
    source = src
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(localObjectFlavor)

      override fun isDataFlavorSupported(flavor: DataFlavor) = localObjectFlavor == flavor

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
    val tgt = support.component as? DnDTabbedPane
    if (!support.isDrop || !support.isDataFlavorSupported(localObjectFlavor) || tgt == null) {
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
    val cursor = if (canDrop) DragSource.DefaultMoveDrop else DragSource.DefaultMoveNoDrop
    val glassPane = tgt.rootPane.glassPane
    glassPane.cursor = cursor
    tgt.cursor = cursor

    support.setShowDropLocation(canDrop)
    // dl.canDrop = canDrop
    tgt.updateTabDropLocation(dl, canDrop)
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
    src.rootPane.glassPane = GhostGlassPane(src)
    return if (src.dragTabIndex < 0) {
      NONE
    } else {
      dragImage = makeDragTabImage(src)
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
    getDropLineRect().also { rect ->
      val g2 = g.create() as? Graphics2D ?: return
      val r = SwingUtilities.convertRectangle(tabbedPane, rect, this)
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f)
      g2.paint = Color.RED
      g2.fill(r)
      g2.dispose()
    }
  }

  fun getDropLineRect(): Rectangle {
    val index = tabbedPane.dropLocation?.index ?: -1
    if (index < 0) {
      RECT_LINE.setBounds(0, 0, 0, 0)
    } else {
      val a = minOf(index, 1)
      val r = tabbedPane.getBoundsAt(a * (index - 1))
      val tp = tabbedPane.tabPlacement
      if (tp == JTabbedPane.TOP || tp == JTabbedPane.BOTTOM) {
        RECT_LINE.setBounds(r.x - LINE_SIZE / 2 + r.width * a, r.y, LINE_SIZE, r.height)
      } else {
        RECT_LINE.setBounds(r.x, r.y - LINE_SIZE / 2 + r.height * a, r.width, LINE_SIZE)
      }
    }
    return RECT_LINE
  }

  companion object {
    private const val LINE_SIZE = 3
    private val RECT_LINE = Rectangle()
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
