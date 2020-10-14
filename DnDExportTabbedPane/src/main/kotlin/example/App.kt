package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.IOException
import javax.swing.* // ktlint-disable no-wildcard-imports
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

  val dropTargetListener = TabDropTargetAdapter()
  val handler = TabTransferHandler()
  listOf(tabbedPane, sub, sub2).forEach { tp ->
    tp.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
    tp.transferHandler = handler
    runCatching {
      tp.dropTarget.addDropTargetListener(dropTargetListener)
    }.onFailure { ex -> // catch (ex: TooManyListenersException) {
      ex.printStackTrace()
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
    tabs.tabLayoutPolicy = if (sc.isSelected) JTabbedPane.SCROLL_TAB_LAYOUT else JTabbedPane.WRAP_TAB_LAYOUT
  }
  return JPanel(FlowLayout(FlowLayout.LEFT)).also {
    it.add(tc)
    it.add(sc)
  }
}

private class DnDTabbedPane : JTabbedPane() {
  var dragTabIndex = -1
  @Transient var dropLocation: DropLocation? = null

  val tabAreaBounds: Rectangle
    get() {
      val tabbedRect = bounds
      val xx = tabbedRect.x
      val yy = tabbedRect.y
      val compRect = selectedComponent?.bounds ?: Rectangle()
      val tabPlacement = getTabPlacement()
      if (isTopBottomTabPlacement(tabPlacement)) {
        tabbedRect.height = tabbedRect.height - compRect.height
        if (tabPlacement == SwingConstants.BOTTOM) {
          tabbedRect.y += compRect.y + compRect.height
        }
      } else {
        tabbedRect.width = tabbedRect.width - compRect.width
        if (tabPlacement == SwingConstants.RIGHT) {
          tabbedRect.x += compRect.x + compRect.width
        }
      }
      tabbedRect.translate(-xx, -yy)
      return tabbedRect
    }

  class DropLocation(pt: Point, val index: Int) : TransferHandler.DropLocation(pt)

  init {
    val h = Handler()
    addMouseListener(h)
    addMouseMotionListener(h)
    addPropertyChangeListener(h)
  }

  fun getDropLineRect(): Rectangle {
    val index = dropLocation?.index ?: -1
    if (index < 0) {
      RECT_LINE.setBounds(0, 0, 0, 0)
      return RECT_LINE
    }
    val a = minOf(index, 1)
    val r = getBoundsAt(a * (index - 1))
    if (isTopBottomTabPlacement(getTabPlacement())) {
      RECT_LINE.setBounds(r.x - LINE_SIZE / 2 + r.width * a, r.y, LINE_SIZE, r.height)
    } else {
      RECT_LINE.setBounds(r.x, r.y - LINE_SIZE / 2 + r.height * a, r.width, LINE_SIZE)
    }
    return RECT_LINE
  }

  private fun clickArrowButton(actionKey: String) {
    var scrollForwardButton: JButton? = null
    var scrollBackwardButton: JButton? = null
    for (c in components) {
      val b = c as? JButton ?: continue
      if (scrollForwardButton == null) {
        scrollForwardButton = b
      } else if (scrollBackwardButton == null) {
        scrollBackwardButton = b
      }
    }
    val button = if ("scrollTabsForwardAction" == actionKey) scrollForwardButton else scrollBackwardButton
    button?.takeIf { it.isEnabled }?.doClick()
  }

  fun autoScrollTest(pt: Point) {
    val r = tabAreaBounds
    if (isTopBottomTabPlacement(getTabPlacement())) {
      RECT_BACKWARD.setBounds(r.x, r.y, SCROLL_SIZE, r.height)
      RECT_FORWARD.setBounds(r.x + r.width - SCROLL_SIZE - BUTTON_SIZE, r.y, SCROLL_SIZE + BUTTON_SIZE, r.height)
    } else { // if (tabPlacement == LEFT || tabPlacement == RIGHT) {
      RECT_BACKWARD.setBounds(r.x, r.y, r.width, SCROLL_SIZE)
      RECT_FORWARD.setBounds(r.x, r.y + r.height - SCROLL_SIZE - BUTTON_SIZE, r.width, SCROLL_SIZE + BUTTON_SIZE)
    }
    if (RECT_BACKWARD.contains(pt)) {
      clickArrowButton("scrollTabsBackwardAction")
    } else if (RECT_FORWARD.contains(pt)) {
      clickArrowButton("scrollTabsForwardAction")
    }
  }

  fun tabDropLocationForPoint(p: Point): DropLocation {
    for (i in 0 until tabCount) {
      if (getBoundsAt(i).contains(p)) {
        return DropLocation(p, i)
      }
    }
    return if (tabAreaBounds.contains(p)) {
      DropLocation(p, tabCount)
    } else DropLocation(p, -1)
  }

  fun updateTabDropLocation(location: DropLocation?, forDrop: Boolean): Any? {
    val old = dropLocation
    dropLocation = if (location == null || !forDrop) DropLocation(Point(), -1) else location
    firePropertyChange("dropLocation", old, dropLocation)
    return null
  }

  fun exportTab(dragIndex: Int, target: JTabbedPane, targetIndex: Int) {
    println("exportTab")
    val cmp = getComponentAt(dragIndex)
    val title = getTitleAt(dragIndex)
    val icon = getIconAt(dragIndex)
    val tip = getToolTipTextAt(dragIndex)
    val isEnabled = isEnabledAt(dragIndex)
    val tab = getTabComponentAt(dragIndex)
    // // ButtonTabComponent
    // if (tab is ButtonTabComponent) {
    //   tab = new ButtonTabComponent(target)
    // }

    remove(dragIndex)
    target.insertTab(title, icon, cmp, tip, targetIndex)
    target.setEnabledAt(targetIndex, isEnabled)
    target.setTabComponentAt(targetIndex, tab)
    target.selectedIndex = targetIndex
    (tab as? JComponent)?.also {
      it.scrollRectToVisible(it.bounds)
    }
  }

  fun convertTab(prev: Int, next: Int) {
    println("convertTab")
    val cmp = getComponentAt(prev)
    val tab = getTabComponentAt(prev)
    val title = getTitleAt(prev)
    val icon = getIconAt(prev)
    val tip = getToolTipTextAt(prev)
    val isEnabled = isEnabledAt(prev)
    val tgtIndex = if (prev > next) next else next - 1
    remove(prev)
    insertTab(title, icon, cmp, tip, tgtIndex)
    setEnabledAt(tgtIndex, isEnabled)
    if (isEnabled) {
      selectedIndex = tgtIndex
    }
    setTabComponentAt(tgtIndex, tab)
  }

  private inner class Handler : MouseAdapter(), PropertyChangeListener { // , BeforeDrag
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
        val isTabRunsRotated = src.ui !is MetalTabbedPaneUI &&
          src.tabLayoutPolicy == WRAP_TAB_LAYOUT && idx != selIdx
        dragTabIndex = if (isTabRunsRotated) selIdx else idx
        th.exportAsDrag(src, e, TransferHandler.MOVE)
        RECT_LINE.setBounds(0, 0, 0, 0)
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
    private const val LINE_SIZE = 3
    private val RECT_BACKWARD = Rectangle()
    private val RECT_FORWARD = Rectangle()
    private val RECT_LINE = Rectangle()
  }
}

private class TabDropTargetAdapter : DropTargetAdapter() {
  private fun clearDropLocationPaint(c: Component) {
    val t = c as? DnDTabbedPane ?: return
    t.updateTabDropLocation(null, false)
    t.cursor = Cursor.getDefaultCursor()
  }

  override fun drop(dtde: DropTargetDropEvent) {
    val c = dtde.dropTargetContext.component
    println("DropTargetListener#drop: ${c.name}")
    clearDropLocationPaint(c)
  }

  override fun dragExit(dte: DropTargetEvent) {
    val c = dte.dropTargetContext.component
    println("DropTargetListener#dragExit: ${c.name}")
    clearDropLocationPaint(c)
  }

  override fun dragEnter(dtde: DropTargetDragEvent) {
    val c = dtde.dropTargetContext.component
    println("DropTargetListener#dragEnter: ${c.name}")
  }
}

private data class DnDTabData(val tabbedPane: DnDTabbedPane)

private class TabTransferHandler : TransferHandler() {
  private val localObjectFlavor = DataFlavor(DnDTabData::class.java, "DnDTabData")
  private var source: DnDTabbedPane? = null

  override fun createTransferable(c: JComponent): Transferable? {
    println("createTransferable")
    val src: DnDTabbedPane? = c as? DnDTabbedPane
    source = src
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf(localObjectFlavor)

      override fun isDataFlavorSupported(flavor: DataFlavor) = localObjectFlavor == flavor

      @Throws(UnsupportedFlavorException::class, IOException::class)
      override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor) && src != null) {
          DnDTabData(src)
        } else {
          throw UnsupportedFlavorException(flavor)
        }
      }
    }
  }

  override fun canImport(support: TransferSupport): Boolean {
    // println("canImport")
    val target = support.component
    if (!support.isDrop || !support.isDataFlavorSupported(localObjectFlavor) || target !is DnDTabbedPane) {
      println("canImport: ${support.isDrop} ${support.isDataFlavorSupported(localObjectFlavor)}")
      return false
    }
    support.dropAction = MOVE
    val tdl = support.dropLocation
    val pt = tdl.dropPoint
    // val target = support.getComponent() as? DnDTabbedPane
    target.autoScrollTest(pt)
    val dl = target.tabDropLocationForPoint(pt)
    val idx = dl.index

    val isAreaContains = target.tabAreaBounds.contains(pt) && idx >= 0
    val canDrop = if (target == source) {
      isAreaContains && idx != target.dragTabIndex && idx != target.dragTabIndex + 1
    } else {
      source?.let { !it.isAncestorOf(target) } ?: false && isAreaContains
    }

    // [JDK-6700748] Cursor flickering during D&D when using CellRendererPane with validation - Java Bug System
    // https://bugs.openjdk.java.net/browse/JDK-6700748
    val cursor = if (canDrop) DragSource.DefaultMoveDrop else DragSource.DefaultMoveNoDrop
    val glassPane = target.rootPane.glassPane
    glassPane.cursor = cursor
    target.cursor = cursor

    support.setShowDropLocation(canDrop)
    // dl.canDrop = canDrop
    target.updateTabDropLocation(dl, canDrop)
    return canDrop
  }

  private fun makeDragTabImage(tabbedPane: DnDTabbedPane): BufferedImage {
    val rect = tabbedPane.getBoundsAt(tabbedPane.dragTabIndex)
    val image = BufferedImage(tabbedPane.width, tabbedPane.height, BufferedImage.TYPE_INT_ARGB)
    val g2 = image.createGraphics()
    tabbedPane.paint(g2)
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
    println("getSourceActions")
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
    println("importData")
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

  override fun exportDone(c: JComponent?, data: Transferable?, action: Int) {
    println("exportDone")
    val src = c as? DnDTabbedPane ?: return
    src.rootPane.glassPane.isVisible = false
    src.updateTabDropLocation(null, false)
    src.repaint()
    src.cursor = Cursor.getDefaultCursor()
  }
}

private class GhostGlassPane(private var tabbedPane: DnDTabbedPane) : JComponent() {
  init {
    isOpaque = false
  }

  fun setTargetTabbedPane(tab: DnDTabbedPane) {
    tabbedPane = tab
  }

  override fun paintComponent(g: Graphics) {
    tabbedPane.getDropLineRect().also { rect ->
      val g2 = g.create() as? Graphics2D ?: return
      val r = SwingUtilities.convertRectangle(tabbedPane, rect, this)
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f)
      g2.paint = Color.RED
      g2.fill(r)
      g2.dispose()
    }
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
