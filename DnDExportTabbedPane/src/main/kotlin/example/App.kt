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
import java.util.TooManyListenersException
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
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
      it.addTab("JLabel 04", JLabel("<html>asfasfdasdfasdfsa<br>asfdd13412341234123446745fgh"))
      it.addTab("null 05", null)
      it.addTab("JTabbedPane 06", sub)
      it.addTab("Title 000000000000000007", JScrollPane(JTree()))
    }

    val sub2 = DnDTabbedPane().also {
      it.addTab("Title aaa", JLabel("aaa"))
      it.addTab("Title bbb", JScrollPane(JTree()))
      it.addTab("Title ccc", JScrollPane(JTextArea("JTextArea ccc")))
    }

    tabbedPane.setName("JTabbedPane#main")
    sub.setName("JTabbedPane#sub1")
    sub2.setName("JTabbedPane#sub2")

    val dropTargetListener = TabDropTargetAdapter()
    val handler = TabTransferHandler()
    listOf(tabbedPane, sub, sub2).forEach {
      it.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT)
      it.setTransferHandler(handler)
      try {
        it.getDropTarget().addDropTargetListener(dropTargetListener)
      } catch (ex: TooManyListenersException) {
        ex.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
      }
    }

    val p = JPanel(GridLayout(2, 1))
    p.add(tabbedPane)
    p.add(sub2)
    add(p)
    add(makeCheckBoxPanel(tabbedPane), BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeCheckBoxPanel(tabs: JTabbedPane): Component {
    val tc = JCheckBox("Top", true)
    tc.addActionListener {
      tabs.setTabPlacement(if (tc.isSelected()) JTabbedPane.TOP else JTabbedPane.RIGHT)
    }
    val sc = JCheckBox("SCROLL_TAB_LAYOUT", true)
    sc.addActionListener {
      tabs.setTabLayoutPolicy(if (sc.isSelected()) JTabbedPane.SCROLL_TAB_LAYOUT else JTabbedPane.WRAP_TAB_LAYOUT)
    }
    return JPanel(FlowLayout(FlowLayout.LEFT)).also {
      it.add(tc)
      it.add(sc)
    }
  }
}

internal class DnDTabbedPane : JTabbedPane() {
  private val dropMode = DropMode.INSERT
  var dragTabIndex = -1
  @Transient
  var dropLocation: DnDTabbedPane.DropLocation? = null

  fun getDropLineRect(): Rectangle {
    val index = dropLocation?.takeIf { it.isDroppable }?.let { it.index } ?: -1
    if (index < 0) {
      RECT_LINE.setBounds(0, 0, 0, 0)
      return RECT_LINE
    }
    val a = Math.min(index, 1)
    val r = getBoundsAt(a * (index - 1))
    if (isTopBottomTabPlacement(getTabPlacement())) {
      RECT_LINE.setBounds(r.x - LINE_WIDTH / 2 + r.width * a, r.y, LINE_WIDTH, r.height)
    } else {
      RECT_LINE.setBounds(r.x, r.y - LINE_WIDTH / 2 + r.height * a, r.width, LINE_WIDTH)
    }
    return RECT_LINE
  }

  // tabbedRect.grow(2, 2)
  val tabAreaBounds: Rectangle
    get() {
      val tabbedRect = getBounds()
      val xx = tabbedRect.x
      val yy = tabbedRect.y
      val compRect = getSelectedComponent()?.getBounds() ?: Rectangle()
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

  class DropLocation(pt: Point, val index: Int) : TransferHandler.DropLocation(pt) {
    var isDroppable = true
  }

  private fun clickArrowButton(actionKey: String) {
    var scrollForwardButton: JButton? = null
    var scrollBackwardButton: JButton? = null
    for (c in getComponents()) {
      val b = c as? JButton ?: continue
      if (scrollForwardButton == null && scrollBackwardButton == null) {
        scrollForwardButton = b
      } else if (scrollBackwardButton == null) {
        scrollBackwardButton = b
      }
    }
    val button = if ("scrollTabsForwardAction" == actionKey) scrollForwardButton else scrollBackwardButton
    button?.takeIf { it.isEnabled() }?.doClick()
  }

  fun autoScrollTest(pt: Point) {
    val r = tabAreaBounds
    // int tabPlacement = getTabPlacement()
    // if (tabPlacement == TOP || tabPlacement == BOTTOM) {
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

  init {
    val h = Handler()
    addMouseListener(h)
    addMouseMotionListener(h)
    addPropertyChangeListener(h)
  }

  // @Override TransferHandler.DropLocation dropLocationForPoint(Point p) {
  fun tabDropLocationForPoint(p: Point): DnDTabbedPane.DropLocation {
    if (dropMode != DropMode.INSERT) {
      assert(false) { "Unexpected drop mode" }
    }
    for (i in 0 until getTabCount()) {
      if (getBoundsAt(i).contains(p)) {
        return DropLocation(p, i)
      }
    }
    return if (tabAreaBounds.contains(p)) {
      DropLocation(p, getTabCount())
    } else DropLocation(p, -1)
  }

  fun updateTabDropLocation(location: DnDTabbedPane.DropLocation?, forDrop: Boolean): Any? {
    val old = dropLocation
    if (location == null || !forDrop) {
      dropLocation = DropLocation(Point(), -1)
    } else {
      dropLocation = location
    }
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
    // if (tab instanceof ButtonTabComponent) {
    //   tab = new ButtonTabComponent(target)
    // }

    remove(dragIndex)
    target.insertTab(title, icon, cmp, tip, targetIndex)
    target.setEnabledAt(targetIndex, isEnabled)
    target.setTabComponentAt(targetIndex, tab)
    target.setSelectedIndex(targetIndex)
    (tab as? JComponent)?.also {
      it.scrollRectToVisible(it.getBounds())
    }
  }

  fun convertTab(prev: Int, next: Int) {
    println("convertTab")
    // if (next < 0 || prev == next) {
    //   return
    // }
    val cmp = getComponentAt(prev)
    val tab = getTabComponentAt(prev)
    val title = getTitleAt(prev)
    val icon = getIconAt(prev)
    val tip = getToolTipTextAt(prev)
    val isEnabled = isEnabledAt(prev)
    val tgtindex = if (prev > next) next else next - 1
    remove(prev)
    insertTab(title, icon, cmp, tip, tgtindex)
    setEnabledAt(tgtindex, isEnabled)
    // When you drag'n'drop a disabled tab, it finishes enabled and selected.
    // pointed out by dlorde
    if (isEnabled) {
      setSelectedIndex(tgtindex)
    }
    // I have a component in all tabs (jlabel with an X to close the tab) and when i move a tab the component disappear.
    // pointed out by Daniel Dario Morales Salas
    setTabComponentAt(tgtindex, tab)
  }

  private inner class Handler : MouseAdapter(), PropertyChangeListener { // , BeforeDrag
    private var startPt: Point? = null
    private val gestureMotionThreshold = DragSource.getDragThreshold()

    private fun repaintDropLocation() {
      (getRootPane().getGlassPane() as? GhostGlassPane)?.also {
        it.setTargetTabbedPane(this@DnDTabbedPane)
        it.repaint()
      }
    }

    // PropertyChangeListener
    override fun propertyChange(e: PropertyChangeEvent) {
      val propertyName = e.getPropertyName()
      if ("dropLocation" == propertyName) {
        // System.out.println("propertyChange: dropLocation")
        repaintDropLocation()
      }
    }

    // MouseListener
    override fun mousePressed(e: MouseEvent) {
      val src = e.getComponent() as DnDTabbedPane
      val isOnlyOneTab = src.getTabCount() <= 1
      if (isOnlyOneTab) {
        startPt = null
        return
      }
      val tabPt = e.getPoint() // e.getDragOrigin()
      val idx = src.indexAtLocation(tabPt.x, tabPt.y)
      // disabled tab, null component problem.
      // pointed out by daryl. NullPointerException: i.e. addTab("Tab", null)
      val flag = idx < 0 || !src.isEnabledAt(idx) || src.getComponentAt(idx) == null
      startPt = if (flag) null else tabPt
    }

    override fun mouseDragged(e: MouseEvent) {
      val tabPt = e.getPoint() // e.getDragOrigin()
      if (tabPt.distance(startPt) > gestureMotionThreshold) {
        val src = e.getComponent() as DnDTabbedPane
        val th = src.getTransferHandler()
        dragTabIndex = src.indexAtLocation(tabPt.x, tabPt.y)
        th.exportAsDrag(src, e, TransferHandler.MOVE)
        RECT_LINE.setBounds(0, 0, 0, 0)
        src.getRootPane().getGlassPane().setVisible(true)
        src.updateTabDropLocation(DropLocation(tabPt, -1), true)
        startPt = null
      }
    }
  }

  companion object {
    private val SCROLL_SIZE = 20 // Test
    private val BUTTON_SIZE = 30 // XXX 30 is magic number of scroll button size
    private val LINE_WIDTH = 3
    private val RECT_BACKWARD = Rectangle()
    private val RECT_FORWARD = Rectangle()
    protected val RECT_LINE = Rectangle()

    fun isTopBottomTabPlacement(tabPlacement: Int): Boolean {
      return tabPlacement == JTabbedPane.TOP || tabPlacement == JTabbedPane.BOTTOM
    }
  }
}

internal class TabDropTargetAdapter : DropTargetAdapter() {
  private fun clearDropLocationPaint(c: Component) {
    val t = c as? DnDTabbedPane ?: return
    t.updateTabDropLocation(null, false)
    t.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
  }

  override fun drop(dtde: DropTargetDropEvent) {
    val c = dtde.getDropTargetContext().getComponent()
    println("DropTargetListener#drop: " + c.getName())
    clearDropLocationPaint(c)
  }

  override fun dragExit(dte: DropTargetEvent) {
    val c = dte.getDropTargetContext().getComponent()
    println("DropTargetListener#dragExit: " + c.getName())
    clearDropLocationPaint(c)
  }

  override fun dragEnter(dtde: DropTargetDragEvent) {
    val c = dtde.getDropTargetContext().getComponent()
    println("DropTargetListener#dragEnter: " + c.getName())
  }
  // @Override public void dragOver(DropTargetDragEvent dtde) {
  //   // System.out.println("dragOver")
  // }
  // @Override public void dropActionChanged(DropTargetDragEvent dtde) {
  //   System.out.println("dropActionChanged")
  // }
}

internal class DnDTabData(val tabbedPane: DnDTabbedPane)

internal class TabTransferHandler : TransferHandler() {
  protected val localObjectFlavor: DataFlavor
  protected var source: DnDTabbedPane? = null

  init {
    println("TabTransferHandler")
    localObjectFlavor = DataFlavor(DnDTabData::class.java, "DnDTabData")
  }

  protected override fun createTransferable(c: JComponent): Transferable? {
    println("createTransferable")
    val src: DnDTabbedPane? = c as? DnDTabbedPane
    source = src
    return object : Transferable {
      override fun getTransferDataFlavors() = arrayOf<DataFlavor>(localObjectFlavor)

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

  override fun canImport(support: TransferHandler.TransferSupport): Boolean {
    // System.out.println("canImport")
    if (!support.isDrop() || !support.isDataFlavorSupported(localObjectFlavor)) {
      println("canImport:" + support.isDrop() + " " + support.isDataFlavorSupported(localObjectFlavor))
      return false
    }
    support.setDropAction(TransferHandler.MOVE)
    val tdl = support.getDropLocation()
    val pt = tdl.getDropPoint()
    val target = support.getComponent() as DnDTabbedPane
    target.autoScrollTest(pt)
    val dl = target.tabDropLocationForPoint(pt)
    val idx = dl.index

    val isAreaContains = target.tabAreaBounds.contains(pt) && idx >= 0
    val isDroppable = if (target == source) {
      isAreaContains && idx != target.dragTabIndex && idx != target.dragTabIndex + 1
    } else {
      source?.let { !it.isAncestorOf(target) } ?: false && isAreaContains
    }

    // [JDK-6700748] Cursor flickering during D&D when using CellRendererPane with validation - Java Bug System
    // https://bugs.openjdk.java.net/browse/JDK-6700748
    val cursor = if (isDroppable) DragSource.DefaultMoveDrop else DragSource.DefaultMoveNoDrop
    val glassPane = target.getRootPane().getGlassPane()
    glassPane.setCursor(cursor)
    target.setCursor(cursor)

    support.setShowDropLocation(isDroppable)
    dl.isDroppable = isDroppable
    target.updateTabDropLocation(dl, isDroppable)
    return isDroppable
  }

  private fun makeDragTabImage(tabbedPane: DnDTabbedPane): BufferedImage {
    val rect = tabbedPane.getBoundsAt(tabbedPane.dragTabIndex)
    val image = BufferedImage(tabbedPane.getWidth(), tabbedPane.getHeight(), BufferedImage.TYPE_INT_ARGB)
    val g2 = image.createGraphics()
    tabbedPane.paint(g2)
    g2.dispose()
    if (rect.x < 0) {
      rect.translate(-rect.x, 0)
    }
    if (rect.y < 0) {
      rect.translate(0, -rect.y)
    }
    if (rect.x + rect.width > image.getWidth()) {
      rect.width = image.getWidth() - rect.x
    }
    if (rect.y + rect.height > image.getHeight()) {
      rect.height = image.getHeight() - rect.y
    }
    return image.getSubimage(rect.x, rect.y, rect.width, rect.height)
  }

  override fun getSourceActions(c: JComponent): Int {
    println("getSourceActions")
    val src = c as? DnDTabbedPane ?: return TransferHandler.NONE
    src.getRootPane().setGlassPane(GhostGlassPane(src))
    return if (src.dragTabIndex < 0) {
      TransferHandler.NONE
    } else {
      setDragImage(makeDragTabImage(src))
      src.getRootPane().getGlassPane().setVisible(true)
      TransferHandler.MOVE
    }
  }

  override fun importData(support: TransferHandler.TransferSupport): Boolean {
    println("importData")
    val target = support.getComponent() as? DnDTabbedPane
    if (!canImport(support) || target == null) {
      return false
    }
    // val target = support.getComponent() as? DnDTabbedPane ?: return false
    // val dl = target.dropLocation
    return try {
      val data = support.getTransferable().getTransferData(localObjectFlavor) as DnDTabData
      val src = data.tabbedPane
      val index = target.dropLocation?.index ?: -1
      if (target == src) {
        src.convertTab(src.dragTabIndex, index) // getTargetTabIndex(e.getLocation()))
      } else {
        src.exportTab(src.dragTabIndex, target, index)
      }
      true
    } catch (ex: UnsupportedFlavorException) {
      false
    } catch (ex: IOException) {
      false
    }
  }

  protected override fun exportDone(c: JComponent?, data: Transferable?, action: Int) {
    println("exportDone")
    val src = c as? DnDTabbedPane ?: return
    src.getRootPane().getGlassPane().setVisible(false)
    src.updateTabDropLocation(null, false)
    src.repaint()
    src.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
  }
}

internal class GhostGlassPane(private var tabbedPane: DnDTabbedPane) : JComponent() {

  init {
    setOpaque(false)
  }

  fun setTargetTabbedPane(tab: DnDTabbedPane) {
    tabbedPane = tab
  }

  protected override fun paintComponent(g: Graphics) {
    tabbedPane.getDropLineRect().also { rect ->
      val g2 = g.create() as Graphics2D
      val r = SwingUtilities.convertRectangle(tabbedPane, rect, this)
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f))
      g2.setPaint(Color.RED)
      g2.fill(r)
      g2.dispose()
    }
  }
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
