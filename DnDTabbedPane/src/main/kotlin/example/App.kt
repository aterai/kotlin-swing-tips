package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DragSource
import java.awt.dnd.DragSourceDragEvent
import java.awt.dnd.DragSourceDropEvent
import java.awt.dnd.DragSourceEvent
import java.awt.dnd.DragSourceListener
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.plaf.metal.MetalTabbedPaneUI

fun makeUI(): Component {
  val sub = DnDTabbedPane().also {
    it.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
    it.addTab("Title aa", JLabel("aaa"))
    it.addTab("Title bb", JScrollPane(JTree()))
    val txt = """
      123412341234
      46746745
      245342
    """.trimIndent()
    it.addTab("Title cc", JScrollPane(JTextArea(txt)))
  }

  val p = JPanel(BorderLayout()).also {
    val check = JCheckBox()
    check.isOpaque = false
    check.isSelected = true
    it.isOpaque = false
    it.add(check, BorderLayout.WEST)
    it.add(JLabel("JTree 00"))
  }

  val tab = DnDTabbedPane().also {
    it.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
    it.addTab("JTree 00", JScrollPane(JTree()))
    it.setTabComponentAt(0, p)
    it.addTab("JLabel 01", ColorIcon(Color.RED), JLabel("Test"))
    it.addTab("JTable 02", ColorIcon(Color.GREEN), JScrollPane(JTable(20, 3)))
    it.addTab("JTabbedPane 03", ColorIcon(Color.BLUE), sub)
    it.addTab("JTextArea 04", JScrollPane(JTextArea("1\n22\n")))
    it.addTab("JLabel 05", JLabel("<html>33333333333<br>13412341234123446745"))
    it.addTab("null 06", null)
    it.addTab("Title 000000000000000007", JScrollPane(JTree()))
  }

  return JPanel(BorderLayout()).also {
    it.add(makeCheckBoxPanel(tab), BorderLayout.NORTH)
    it.add(tab)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeCheckBoxPanel(tab: DnDTabbedPane): Component {
  val check1 = JCheckBox("Tab Ghost", true)
  check1.addActionListener { tab.hasGhost = check1.isSelected }

  val check2 = JCheckBox("Top", true)
  check2.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected ?: false
    tab.tabPlacement = if (b) JTabbedPane.TOP else JTabbedPane.RIGHT
  }

  val check3 = JCheckBox("SCROLL_TAB_LAYOUT", true)
  check3.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected ?: false
    tab.tabLayoutPolicy = if (b) {
      JTabbedPane.SCROLL_TAB_LAYOUT
    } else {
      JTabbedPane.WRAP_TAB_LAYOUT
    }
  }

  val check4 = JCheckBox("Debug Paint", true)
  check4.addActionListener { tab.isPaintScrollArea = check4.isSelected }

  val p1 = JPanel(FlowLayout(FlowLayout.LEFT)).also {
    it.add(check1)
    it.add(check2)
  }

  val p2 = JPanel(FlowLayout(FlowLayout.LEFT)).also {
    it.add(check3)
    it.add(check4)
  }

  return JPanel(BorderLayout()).also {
    it.add(p1, BorderLayout.NORTH)
    it.add(p2, BorderLayout.SOUTH)
  }
}

private class DnDTabbedPane : JTabbedPane() {
  private val glassPane = GhostGlassPane(this)
  var dragTabIndex = -1

  // For Debug: >>>
  var hasGhost = true
  var isPaintScrollArea = true
  // <<<

  var rectBackward = Rectangle()
  var rectForward = Rectangle()

  init {
    glassPane.name = "GlassPane"
    DropTarget(glassPane, DnDConstants.ACTION_COPY_OR_MOVE, TabDropTargetListener(), true)
    DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
      this,
      DnDConstants.ACTION_COPY_OR_MOVE,
      TabDragGestureListener(),
    )
  }

  private fun clickArrowButton(actionKey: String) {
    var forwardButton: JButton? = null
    var backwardButton: JButton? = null
    for (c in components) {
      if (c is JButton) {
        if (forwardButton == null) {
          forwardButton = c
        } else if (backwardButton == null) {
          backwardButton = c
        }
      }
    }
    val button = if ("scrollTabsForwardAction" == actionKey) forwardButton else backwardButton
    button?.takeIf { it.isEnabled }?.doClick()
  }

  fun autoScrollTest(glassPt: Point) {
    val r = getTabAreaBounds()
    if (isTopBottomTabPlacement(getTabPlacement())) {
      rectBackward.setBounds(r.x, r.y, RWH, r.height)
      rectForward.setBounds(
        r.x + r.width - RWH - BUTTON_SIZE,
        r.y,
        RWH + BUTTON_SIZE,
        r.height,
      )
    } else {
      rectBackward.setBounds(r.x, r.y, r.width, RWH)
      rectForward.setBounds(
        r.x,
        r.y + r.height - RWH - BUTTON_SIZE,
        r.width,
        RWH + BUTTON_SIZE,
      )
    }
    rectBackward = SwingUtilities.convertRectangle(parent, rectBackward, glassPane)
    rectForward = SwingUtilities.convertRectangle(parent, rectForward, glassPane)
    if (rectBackward.contains(glassPt)) {
      clickArrowButton("scrollTabsBackwardAction")
    } else if (rectForward.contains(glassPt)) {
      clickArrowButton("scrollTabsForwardAction")
    }
  }

  fun getTargetTabIndex(glassPt: Point): Int {
    val tabPt = SwingUtilities.convertPoint(glassPane, glassPt, this)
    val horiz = isTopBottomTabPlacement(getTabPlacement())
    return (0..<tabCount)
      .map { if (horiz) getHorizontalIndex(it, tabPt) else getVerticalIndex(it, tabPt) }
      .firstOrNull { it >= 0 }
      ?: -1
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

  // https://github.com/aterai/java-swing-tips/pull/24
  // fun getTargetTabIndex(glassPt: Point): Int {
  //   val count = tabCount
  //   if (count == 0) {
  //     return -1
  //   }
  //
  //   val tabPt = SwingUtilities.convertPoint(glassPane, glassPt, this)
  //   val isHorizontal = isTopBottomTabPlacement(getTabPlacement())
  //   for (i in 0..<count) {
  //     val r = getBoundsAt(i)
  //
  //     // First half.
  //     if (isHorizontal) {
  //       r.width = r.width / 2 + 1
  //     } else {
  //       r.height = r.height / 2 + 1
  //     }
  //     if (r.contains(tabPt)) {
  //       return i
  //     }
  //
  //     // Second half.
  //     if (isHorizontal) {
  //       r.x += r.width
  //     } else {
  //       r.y += r.height
  //     }
  //     if (r.contains(tabPt)) {
  //       return i + 1
  //     }
  //   }
  //
  //   val lastRect = getBoundsAt(count - 1)
  //   val d = if (isHorizontal) Point(1, 0) else Point(0, 1)
  //   lastRect.translate(lastRect.width * d.x, lastRect.height * d.y)
  //   return if (lastRect.contains(tabPt)) count else -1
  // }

  fun convertTab(
    prev: Int,
    next: Int,
  ) {
    if (next < 0 || prev == next) {
      // This check is needed if tab content is null.
      return
    }
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
    // When you drag and drop a disabled tab, it finishes enabled and selected.
    // pointed out by dlorde
    if (isEnabled) {
      selectedIndex = tgtIndex
    }
    // I have a component in all tabs (JLabel with an X to close the tab)
    // and when I move a tab the component disappear.
    // pointed out by Daniel Dario Morales Salas
    setTabComponentAt(tgtIndex, tab)
  }

  fun initTargetLine(next: Int) {
    val isSideNeighbor = next < 0 || dragTabIndex == next || next - dragTabIndex == 1
    if (isSideNeighbor) {
      glassPane.setTargetRect(0, 0, 0, 0)
      return
    }
    getBoundsAt(maxOf(0, next - 1))?.also {
      val r = SwingUtilities.convertRectangle(this, it, glassPane)
      val a = minOf(next, 1) // a = (next == 0) ? 0 : 1
      if (isTopBottomTabPlacement(getTabPlacement())) {
        glassPane.setTargetRect(r.x + r.width * a - LINE_SIZE / 2, r.y, LINE_SIZE, r.height)
      } else {
        glassPane.setTargetRect(r.x, r.y + r.height * a - LINE_SIZE / 2, r.width, LINE_SIZE)
      }
    }
  }

  fun initGlassPane(tabPt: Point) {
    rootPane.glassPane = glassPane
    if (hasGhost) {
      val c = getTabComponentAt(dragTabIndex)
      val copy = c ?: JLabel().also {
        it.text = getTitleAt(dragTabIndex)
        it.icon = getIconAt(dragTabIndex)
        it.iconTextGap = UIManager.getInt("TabbedPane.textIconGap")
        it.horizontalAlignment = LEADING
      }
      val d = copy.preferredSize
      val image = BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB)
      val g2 = image.createGraphics()
      SwingUtilities.paintComponent(g2, copy, glassPane, 0, 0, d.width, d.height)
      g2.dispose()
      glassPane.setImage(image)
      if (c != null) {
        setTabComponentAt(dragTabIndex, c)
      }
    }
    val glassPt = SwingUtilities.convertPoint(this, tabPt, glassPane)
    glassPane.setPoint(glassPt)
    glassPane.isVisible = true
  }

  private fun getTabAreaBounds(): Rectangle {
    val tabbedRect = bounds
    val compRect = selectedComponent?.bounds ?: Rectangle()
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
    tabbedRect.grow(2, 2)
    return tabbedRect
  }

  private fun isTopBottomTabPlacement(tabPlacement: Int) =
    tabPlacement == TOP || tabPlacement == BOTTOM

  companion object {
    private const val LINE_SIZE = 3
    private const val RWH = 20
    private const val BUTTON_SIZE = 30 // XXX 30 is magic number of scroll button size
  }
}

private class TabTransferable(
  private val tabbedPane: Component,
) : Transferable {
  override fun getTransferData(flavor: DataFlavor) = tabbedPane

  override fun getTransferDataFlavors() = arrayOf(FLAVOR)

  override fun isDataFlavorSupported(flavor: DataFlavor) = flavor.humanPresentableName == NAME

  companion object {
    private const val NAME = "test"
    private val FLAVOR = DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME)
  }
}

private class TabDragSourceListener : DragSourceListener {
  override fun dragEnter(e: DragSourceDragEvent) {
    e.dragSourceContext.cursor = DragSource.DefaultMoveDrop
  }

  override fun dragExit(e: DragSourceEvent) {
    e.dragSourceContext.cursor = DragSource.DefaultMoveNoDrop
  }

  override fun dragOver(e: DragSourceDragEvent) {
    // not needed
  }

  override fun dragDropEnd(e: DragSourceDropEvent) {
    val gp = (e.dragSourceContext.component as? JComponent)?.rootPane?.glassPane ?: return
    gp.isVisible = false
  }

  override fun dropActionChanged(e: DragSourceDragEvent) {
    // not needed
  }
}

private class TabDragGestureListener : DragGestureListener {
  private val handler = TabDragSourceListener()

  override fun dragGestureRecognized(e: DragGestureEvent) {
    (e.component as? DnDTabbedPane)?.takeIf { it.tabCount > 1 }?.also {
      val tabPt = e.dragOrigin
      val idx = it.indexAtLocation(tabPt.x, tabPt.y)
      val selIdx = it.selectedIndex
      val isWrapLayout = it.tabLayoutPolicy == JTabbedPane.WRAP_TAB_LAYOUT
      val isTabRunsRotated = it.ui !is MetalTabbedPaneUI && isWrapLayout && idx != selIdx
      it.dragTabIndex = if (isTabRunsRotated) selIdx else idx
      if (it.dragTabIndex >= 0 && it.isEnabledAt(it.dragTabIndex)) {
        it.initGlassPane(tabPt)
        runCatching {
          e.startDrag(DragSource.DefaultMoveDrop, TabTransferable(it), handler)
        }.onFailure {
          UIManager.getLookAndFeel().provideErrorFeedback(e.component)
        }
      }
    }
  }
}

private class TabDropTargetListener : DropTargetListener {
  override fun dragEnter(e: DropTargetDragEvent) {
    getGhostGlassPane(e.dropTargetContext.component)?.also {
      val t = e.transferable
      val f = e.currentDataFlavors
      if (t.isDataFlavorSupported(f[0])) { // && tabbedPane.dragTabIndex >= 0) {
        e.acceptDrag(e.dropAction)
      } else {
        e.rejectDrag()
      }
    }
  }

  override fun dragExit(e: DropTargetEvent) {
    getGhostGlassPane(e.dropTargetContext.component)?.also {
      it.setPoint(HIDDEN_POINT)
      it.setTargetRect(0, 0, 0, 0)
      it.repaint()
    }
  }

  override fun dropActionChanged(e: DropTargetDragEvent) {
    // not needed
  }

  override fun dragOver(e: DropTargetDragEvent) {
    val c = e.dropTargetContext.component
    getGhostGlassPane(c)?.also {
      val glassPt = e.location

      val tabbedPane = it.tabbedPane
      tabbedPane.initTargetLine(tabbedPane.getTargetTabIndex(glassPt))
      tabbedPane.autoScrollTest(glassPt)

      it.setPoint(glassPt)
      it.repaint()
    }
  }

  override fun drop(e: DropTargetDropEvent) {
    val c = e.dropTargetContext.component
    getGhostGlassPane(c)?.also {
      val tabbedPane = it.tabbedPane
      val t = e.transferable
      val f = t.transferDataFlavors
      val prev = tabbedPane.dragTabIndex
      val next = tabbedPane.getTargetTabIndex(e.location)
      if (t.isDataFlavorSupported(f[0]) && prev != next) {
        tabbedPane.convertTab(prev, next)
        e.dropComplete(true)
      } else {
        e.dropComplete(false)
      }
      it.isVisible = false
    }
  }

  companion object {
    private val HIDDEN_POINT = Point(0, -1000)

    private fun getGhostGlassPane(c: Component?) = c as? GhostGlassPane
  }
}

private class GhostGlassPane(
  val tabbedPane: DnDTabbedPane,
) : JComponent() {
  private val lineRect = Rectangle()
  private val lineColor = Color(0, 100, 255)
  private val locPt = Point()
  private var draggingGhost: BufferedImage? = null

  // init {
  //   [JDK-6700748]
  //   Cursor flickering during D&D when using CellRendererPane with validation - Java Bug System
  //   https://bugs.openjdk.org/browse/JDK-6700748
  //   setCursor(null)
  // }

  override fun isOpaque() = false

  fun setTargetRect(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    lineRect.setBounds(x, y, width, height)
  }

  fun setImage(draggingGhost: BufferedImage?) {
    this.draggingGhost = draggingGhost
  }

  fun setPoint(pt: Point) {
    this.locPt.location = pt
  }

  override fun setVisible(v: Boolean) {
    super.setVisible(v)
    if (!v) {
      setTargetRect(0, 0, 0, 0)
      setImage(null)
    }
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.composite = ALPHA
    val policy = tabbedPane.tabLayoutPolicy
    if (tabbedPane.isPaintScrollArea && policy == JTabbedPane.SCROLL_TAB_LAYOUT) {
      g2.paint = Color.RED
      g2.fill(tabbedPane.rectBackward)
      g2.fill(tabbedPane.rectForward)
    }
    draggingGhost?.also { img ->
      val xx = locPt.getX() - img.getWidth(this) / 2.0
      val yy = locPt.getY() - img.getHeight(this) / 2.0
      g2.drawImage(img, xx.toInt(), yy.toInt(), null)
    }
    g2.paint = lineColor
    g2.fill(lineRect)
    g2.dispose()
  }

  companion object {
    private val ALPHA = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f)
  }
}

private class ColorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(2, 2, iconWidth - 4, iconHeight - 4)
    g2.paint = Color.BLACK
    g2.drawRect(2, 2, iconWidth - 4, iconHeight - 4)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
