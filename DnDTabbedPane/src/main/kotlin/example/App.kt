package example

import java.awt.* // ktlint-disable no-wildcard-imports
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
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalTabbedPaneUI

class MainPanel : JPanel(BorderLayout()) {
  init {
    val sub = DnDTabbedPane().also {
      it.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT)
      it.addTab("Title aa", JLabel("aaa"))
      it.addTab("Title bb", JScrollPane(JTree()))
      it.addTab("Title cc", JScrollPane(JTextArea("123412341234\n46746745\n245342\n")))
    }

    val tab = DnDTabbedPane().also {
      it.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT)
      it.addTab("JTree 00", JScrollPane(JTree()))
      it.addTab("JLabel 01", JLabel("Test"))
      it.addTab("JTable 02", JScrollPane(JTable(20, 3)))
      it.addTab("JTextArea 03", JScrollPane(JTextArea("111111111\n2222222222\n")))
      it.addTab("JLabel 04", JLabel("<html>33333333333333<br>13412341234123446745"))
      it.addTab("null 05", null)
      it.addTab("JTabbedPane 06", sub)
      it.addTab("Title 000000000000000007", JScrollPane(JTree()))
    }

    add(makeCheckBoxPanel(tab), BorderLayout.NORTH)
    add(tab)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeCheckBoxPanel(tab: DnDTabbedPane): Component {
    val check1 = JCheckBox("Tab Ghost", true)
    check1.addActionListener { tab.hasGhost = check1.isSelected() }

    val check2 = JCheckBox("Top", true)
    check2.addActionListener { e ->
      val b = (e.getSource() as? JCheckBox)?.isSelected() ?: false
      tab.setTabPlacement(if (b) JTabbedPane.TOP else JTabbedPane.RIGHT)
    }

    val check3 = JCheckBox("SCROLL_TAB_LAYOUT", true)
    check3.addActionListener { e ->
      val b = (e.getSource() as? JCheckBox)?.isSelected() ?: false
      tab.setTabLayoutPolicy(if (b) JTabbedPane.SCROLL_TAB_LAYOUT else JTabbedPane.WRAP_TAB_LAYOUT)
    }

    val check4 = JCheckBox("Debug Paint", true)
    check4.addActionListener { tab.isPaintScrollArea = check4.isSelected() }

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
}

class DnDTabbedPane : JTabbedPane() {
  private val glassPane = GhostGlassPane(this)
  var dragTabIndex = -1

  // For Debug: >>>
  var hasGhost = true
  var isPaintScrollArea = true
  // <<<

  var rectBackward = Rectangle()
  var rectForward = Rectangle()

  private fun clickArrowButton(actionKey: String) {
    var scrollForwardButton: JButton? = null
    var scrollBackwardButton: JButton? = null
    for (c in getComponents()) {
      if (c is JButton) {
        if (scrollForwardButton == null) {
          scrollForwardButton = c
        } else if (scrollBackwardButton == null) {
          scrollBackwardButton = c
        }
      }
    }
    val button = if ("scrollTabsForwardAction" == actionKey) scrollForwardButton else scrollBackwardButton
    button?.takeIf { it.isEnabled() }?.doClick()
  }

  fun autoScrollTest(glassPt: Point) {
    val r = getTabAreaBounds()
    if (isTopBottomTabPlacement(getTabPlacement())) {
      rectBackward.setBounds(r.x, r.y, RWH, r.height)
      rectForward.setBounds(r.x + r.width - RWH - BUTTON_SIZE, r.y, RWH + BUTTON_SIZE, r.height)
    } else {
      rectBackward.setBounds(r.x, r.y, r.width, RWH)
      rectForward.setBounds(r.x, r.y + r.height - RWH - BUTTON_SIZE, r.width, RWH + BUTTON_SIZE)
    }
    rectBackward = SwingUtilities.convertRectangle(getParent(), rectBackward, glassPane)
    rectForward = SwingUtilities.convertRectangle(getParent(), rectForward, glassPane)
    if (rectBackward.contains(glassPt)) {
      clickArrowButton("scrollTabsBackwardAction")
    } else if (rectForward.contains(glassPt)) {
      clickArrowButton("scrollTabsForwardAction")
    }
  }

  init {
    glassPane.setName("GlassPane")
    DropTarget(glassPane, DnDConstants.ACTION_COPY_OR_MOVE, TabDropTargetListener(), true)
    DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
      this, DnDConstants.ACTION_COPY_OR_MOVE, TabDragGestureListener())
  }

  fun getTargetTabIndex(glassPt: Point): Int {
    val tabPt = SwingUtilities.convertPoint(glassPane, glassPt, this)
    val d = if (isTopBottomTabPlacement(getTabPlacement())) Point(1, 0) else Point(0, 1)
//    return (0 until getTabCount().taleIf { i ->
//      val r = getBoundsAt(i)
//      r.translate(-r.width * d.x / 2, -r.height * d.y / 2)
//      r.contains(tabPt)
//    }.findFirst().orElseGet {
//      val count = getTabCount()
//      val r = getBoundsAt(count - 1)
//      r.translate(r.width * d.x / 2, r.height * d.y / 2)
//      if (r.contains(tabPt)) count else -1
//    }
    for (i in 0 until getTabCount()) {
      val r = getBoundsAt(i)
      r.translate(-r.width * d.x / 2, -r.height * d.y / 2)
      if (r.contains(tabPt)) {
        return i
      }
    }
    val r = getBoundsAt(getTabCount() - 1)
    r.translate(r.width * d.x / 2, r.height * d.y / 2)
    return if (r.contains(tabPt)) getTabCount() else -1
  }

  fun convertTab(prev: Int, next: Int) {
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
    // When you drag'n'drop a disabled tab, it finishes enabled and selected.
    // pointed out by dlorde
    if (isEnabled) {
      setSelectedIndex(tgtIndex)
    }
    // I have a component in all tabs (JLabel with an X to close the tab) and when i move a tab the component disappear.
    // pointed out by Daniel Dario Morales Salas
    setTabComponentAt(tgtIndex, tab)
  }

  fun initTargetLine(next: Int) {
    val isLeftOrRightNeighbor = next < 0 || dragTabIndex == next || next - dragTabIndex == 1
    if (isLeftOrRightNeighbor) {
      glassPane.setTargetRect(0, 0, 0, 0)
      return
    }
    getBoundsAt(maxOf(0, next - 1))?.also {
      val r = SwingUtilities.convertRectangle(this, it, glassPane)
      val a = minOf(next, 1) // a = (next == 0) ? 0 : 1;
      if (isTopBottomTabPlacement(getTabPlacement())) {
        glassPane.setTargetRect(r.x + r.width * a - LINE_SIZE / 2, r.y, LINE_SIZE, r.height)
      } else {
        glassPane.setTargetRect(r.x, r.y + r.height * a - LINE_SIZE / 2, r.width, LINE_SIZE)
      }
    }
  }

  fun initGlassPane(tabPt: Point) {
    getRootPane().setGlassPane(glassPane)
    if (hasGhost) {
      val c = getTabComponentAt(dragTabIndex) ?: JLabel(getTitleAt(dragTabIndex))
      val d = c.getPreferredSize()
      val image = BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB)
      val g2 = image.createGraphics()
      SwingUtilities.paintComponent(g2, c, glassPane, 0, 0, d.width, d.height)
      g2.dispose()
      glassPane.setImage(image)
      // Rectangle rect = getBoundsAt(dragTabIndex);
      // BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
      // Graphics2D g2 = image.createGraphics();
      // paint(g2);
      // g2.dispose();
      // if (rect.x < 0) {
      //   rect.translate(-rect.x, 0);
      // }
      // if (rect.y < 0) {
      //   rect.translate(0, -rect.y);
      // }
      // if (rect.x + rect.width > image.getWidth()) {
      //   rect.width = image.getWidth() - rect.x;
      // }
      // if (rect.y + rect.height > image.getHeight()) {
      //   rect.height = image.getHeight() - rect.y;
      // }
      // glassPane.setImage(image.getSubimage(rect.x, rect.y, rect.width, rect.height));
      // // rect.x = maxOf(0, rect.x); // rect.x < 0 ? 0 : rect.x;
      // // rect.y = maxOf(0, rect.y); // rect.y < 0 ? 0 : rect.y;
      // // image = image.getSubimage(rect.x, rect.y, rect.width, rect.height);
      // // glassPane.setImage(image);
    }
    val glassPt = SwingUtilities.convertPoint(this, tabPt, glassPane)
    glassPane.setPoint(glassPt)
    glassPane.setVisible(true)
  }

  private fun getTabAreaBounds(): Rectangle {
    val tabbedRect = getBounds()

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
    tabbedRect.grow(2, 2)
    return tabbedRect
  }

  private fun isTopBottomTabPlacement(tabPlacement: Int) = tabPlacement == TOP || tabPlacement == BOTTOM

  companion object {
    private const val LINE_SIZE = 3
    private const val RWH = 20
    private const val BUTTON_SIZE = 30 // XXX 30 is magic number of scroll button size
  }
}

class TabTransferable(private val tabbedPane: Component) : Transferable {
  override fun getTransferData(flavor: DataFlavor) = tabbedPane

  override fun getTransferDataFlavors() = arrayOf(FLAVOR)

  override fun isDataFlavorSupported(flavor: DataFlavor) = flavor.getHumanPresentableName() == NAME

  companion object {
    private const val NAME = "test"
    private val FLAVOR = DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME)
  }
}

internal class TabDragSourceListener : DragSourceListener {
  override fun dragEnter(e: DragSourceDragEvent) {
    e.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop)
  }

  override fun dragExit(e: DragSourceEvent) {
    e.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop)
    // glassPane.setTargetRect(0, 0, 0, 0);
    // glassPane.setPoint(new Point(-1000, -1000));
    // glassPane.repaint();
  }

  override fun dragOver(e: DragSourceDragEvent) { /* not needed */ }

  override fun dragDropEnd(e: DragSourceDropEvent) {
    // dragTabIndex = -1;
    // glassPane.setVisible(false);
  }

  override fun dropActionChanged(e: DragSourceDragEvent) { /* not needed */ }
}

internal class TabDragGestureListener : DragGestureListener {
  override fun dragGestureRecognized(e: DragGestureEvent) {
    // e.getComponent()?.takeIf { it is DnDTabbedPane }
    //   ?.let { it as DnDTabbedPane }

    (e.getComponent() as? DnDTabbedPane)?.takeIf { it.getTabCount() > 1 }?.also {
      val tabPt = e.getDragOrigin()
      val idx = it.indexAtLocation(tabPt.x, tabPt.y)
      val selIdx = it.getSelectedIndex()
      val isTabRunsRotated = it.getUI() !is MetalTabbedPaneUI
          && it.getTabLayoutPolicy() == JTabbedPane.WRAP_TAB_LAYOUT
          && idx != selIdx
      it.dragTabIndex = if (isTabRunsRotated) selIdx else idx
      if (it.dragTabIndex >= 0 && it.isEnabledAt(it.dragTabIndex)) {
        it.initGlassPane(tabPt)
        runCatching {
          e.startDrag(DragSource.DefaultMoveDrop, TabTransferable(it), TabDragSourceListener())
        }.onFailure {
          UIManager.getLookAndFeel().provideErrorFeedback(e.getComponent())
        }
      }
    }
  }
}

internal class TabDropTargetListener : DropTargetListener {
  override fun dragEnter(e: DropTargetDragEvent) {
    getGhostGlassPane(e.getDropTargetContext().getComponent())?.also {
      val t = e.getTransferable()
      val f = e.getCurrentDataFlavors()
      if (t.isDataFlavorSupported(f[0])) { // && tabbedPane.dragTabIndex >= 0) {
        e.acceptDrag(e.getDropAction())
      } else {
        e.rejectDrag()
      }
    }
  }

  override fun dragExit(e: DropTargetEvent) {
    // Component c = e.getDropTargetContext().getComponent();
    // System.out.println("DropTargetListener#dragExit: ${c.getName()}");
    getGhostGlassPane(e.getDropTargetContext().getComponent())?.also {
      it.setPoint(HIDDEN_POINT)
      it.setTargetRect(0, 0, 0, 0)
      it.repaint()
    }
  }

  override fun dropActionChanged(e: DropTargetDragEvent) { /* not needed */ }

  override fun dragOver(e: DropTargetDragEvent) {
    val c = e.getDropTargetContext().getComponent()
    getGhostGlassPane(c)?.also {
      val glassPt = e.getLocation()

      val tabbedPane = it.tabbedPane
      tabbedPane.initTargetLine(tabbedPane.getTargetTabIndex(glassPt))
      tabbedPane.autoScrollTest(glassPt)

      it.setPoint(glassPt)
      it.repaint()
    }
  }

  override fun drop(e: DropTargetDropEvent) {
    val c = e.getDropTargetContext().getComponent()
    getGhostGlassPane(c)?.also {
      val tabbedPane = it.tabbedPane
      val t = e.getTransferable()
      val f = t.getTransferDataFlavors()
      val prev = tabbedPane.dragTabIndex
      val next = tabbedPane.getTargetTabIndex(e.getLocation())
      if (t.isDataFlavorSupported(f[0]) && prev != next) {
        tabbedPane.convertTab(prev, next)
        e.dropComplete(true)
      } else {
        e.dropComplete(false)
      }
      it.setVisible(false)
    }
  }

  companion object {
    private val HIDDEN_POINT = Point(0, -1000)

    // private fun getGhostGlassPane(c: Component?) =
    //     c?.takeIf { it is GhostGlassPane }?.let { it as GhostGlassPane }
    private fun getGhostGlassPane(c: Component?) = c as? GhostGlassPane
  }
}

internal class GhostGlassPane(val tabbedPane: DnDTabbedPane) : JComponent() {
  private val lineRect = Rectangle()
  private val lineColor = Color(0, 100, 255)
  private val locPt = Point()
  @Transient
  private var draggingGhost: BufferedImage? = null

  init {
    setOpaque(false)
    // [JDK-6700748] Cursor flickering during D&D when using CellRendererPane with validation - Java Bug System
    // https://bugs.openjdk.java.net/browse/JDK-6700748
    // setCursor(null);
  }

  fun setTargetRect(x: Int, y: Int, width: Int, height: Int) {
    lineRect.setBounds(x, y, width, height)
  }

  fun setImage(draggingGhost: BufferedImage?) {
    this.draggingGhost = draggingGhost
  }

  fun setPoint(pt: Point) {
    this.locPt.setLocation(pt)
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
    g2.setComposite(ALPHA)
    if (tabbedPane.isPaintScrollArea && tabbedPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
      g2.setPaint(Color.RED)
      g2.fill(tabbedPane.rectBackward)
      g2.fill(tabbedPane.rectForward)
    }
    draggingGhost?.also { img ->
      val xx = locPt.getX() - img.getWidth(this) / 2.0
      val yy = locPt.getY() - img.getHeight(this) / 2.0
      g2.drawImage(img, xx.toInt(), yy.toInt(), null)
    }
    g2.setPaint(lineColor)
    g2.fill(lineRect)
    g2.dispose()
  }

  companion object {
    private val ALPHA = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f)
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
