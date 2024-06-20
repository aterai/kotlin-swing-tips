package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import javax.swing.*
import javax.swing.plaf.nimbus.AbstractRegionPainter
import javax.swing.tree.DefaultTreeCellRenderer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sqrt

fun makeUI(): Component {
  val tree = JTree()
  tree.rowHeight = 20
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(GridLayout(1, 0, 2, 2)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(makeScrollPane(tree))
    it.add(makeScrollPane(RoundedSelectionTree0()))
    it.add(makeScrollPane(RoundedSelectionTree()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeScrollPane(view: Component): JScrollPane {
  val scroll = JScrollPane(view)
  scroll.background = Color.WHITE
  scroll.viewport.background = Color.WHITE
  scroll.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  return scroll
}

private class RoundedSelectionTree : JTree() {
  override fun paintComponent(g: Graphics) {
    val sr = selectionRows
    if (sr?.isNotEmpty() == true) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = SELECTED_COLOR
      val area = Area()
      sr.map { getRowBounds(it) }.forEach { area.add(Area(it)) }
      val arc = 4.0
      for (a in GeomUtils.singularization(area)) {
        val lst = GeomUtils.convertAreaToPoint2DList(a)
        GeomUtils.flatteningStepsOnRightSide(lst, arc * 2.0)
        g2.fill(GeomUtils.convertRoundedPath(lst, arc))
      }
      g2.dispose()
    }
    super.paintComponent(g)
  }

  override fun updateUI() {
    super.updateUI()
    setCellRenderer(TransparentTreeCellRenderer())
    isOpaque = false
    setRowHeight(20)
    val d = UIDefaults()
    d["Tree:TreeCell[Enabled+Selected].backgroundPainter"] = TransparentTreeCellPainter()
    putClientProperty("Nimbus.Overrides", d)
    putClientProperty("Nimbus.Overrides.InheritDefaults", false)
    addTreeSelectionListener { repaint() }
  }

  companion object {
    private val SELECTED_COLOR = Color(0xC8_00_78_D7.toInt(), true)
  }
}

private class RoundedSelectionTree0 : JTree() {
  override fun paintComponent(g: Graphics) {
    val sr = selectionRows
    if (sr?.isNotEmpty() == true) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = SELECTED_COLOR
      val area = Area()
      sr.map { getRowBounds(it) }.forEach { area.add(Area(it)) }
      val arc = 4.0
      for (a in GeomUtils.singularization(area)) {
        g2.fill(GeomUtils.convertRoundedPath(GeomUtils.convertAreaToPoint2DList(a), arc))
      }
      g2.dispose()
    }
    super.paintComponent(g)
  }

  override fun updateUI() {
    super.updateUI()
    setCellRenderer(TransparentTreeCellRenderer())
    isOpaque = false
    setRowHeight(20)
    val d = UIDefaults()
    d["Tree:TreeCell[Enabled+Selected].backgroundPainter"] = TransparentTreeCellPainter()
    putClientProperty("Nimbus.Overrides", d)
    putClientProperty("Nimbus.Overrides.InheritDefaults", false)
    addTreeSelectionListener { repaint() }
  }

  companion object {
    private val SELECTED_COLOR = Color(0xC8_00_78_D7.toInt(), true)
  }
}

private class TransparentTreeCellRenderer : DefaultTreeCellRenderer() {
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    val c = super.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      false,
    )
    (c as? JComponent)?.isOpaque = false
    return c
  }

  override fun getBackgroundNonSelectionColor() = ALPHA_OF_ZERO

  override fun getBackgroundSelectionColor() = ALPHA_OF_ZERO

  companion object {
    private val ALPHA_OF_ZERO = Color(0x0, true)
  }
}

private class TransparentTreeCellPainter : AbstractRegionPainter() {
  override fun doPaint(
    g: Graphics2D,
    c: JComponent,
    width: Int,
    height: Int,
    extendedCacheKeys: Array<Any>?,
  ) {
    // Do nothing
  }

  override fun getPaintContext() = null
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
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

private object GeomUtils {
  fun convertAreaToPoint2DList(area: Area): MutableList<Point2D> {
    val list = mutableListOf<Point2D>()
    val pi = area.getPathIterator(null)
    val coords = DoubleArray(6)
    while (!pi.isDone) {
      val pathSegmentType = pi.currentSegment(coords)
      when (pathSegmentType) {
        PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO ->
          list.add(Point2D.Double(coords[0], coords[1]))
      }
      pi.next()
    }
    return list
  }

  fun flatteningStepsOnRightSide(list: MutableList<Point2D>, arc: Double): List<Point2D> {
    val sz = list.size
    for (i in 0..<sz) {
      val i1 = (i + 1) % sz
      val i2 = (i + 2) % sz
      val i3 = (i + 3) % sz
      val pt0 = list[i]
      val pt1 = list[i1]
      val pt2 = list[i2]
      val pt3 = list[i3]
      val dx1 = pt2.x - pt1.x
      if (abs(dx1) > 1.0e-1 && abs(dx1) < arc) {
        val max = max(pt0.x, pt2.x)
        replace(list, i, max, pt0.y)
        replace(list, i1, max, pt1.y)
        replace(list, i2, max, pt2.y)
        replace(list, i3, max, pt3.y)
      }
    }
    return list
  }

  private fun replace(list: MutableList<Point2D>, i: Int, x: Double, y: Double) {
    list.removeAt(i)
    list.add(i, Point2D.Double(x, y))
  }

  fun convertRoundedPath(list: List<Point2D>, arc: Double): Path2D {
    val kappa = 4.0 * (sqrt(2.0) - 1.0) / 3.0
    val akv = arc - arc * kappa
    val pt0 = list[0]
    val path = Path2D.Double()
    val sz = list.size
    path.moveTo(pt0.x + arc, pt0.y)
    for (i in 0..<sz) {
      val prv = list[(i - 1 + sz) % sz]
      val cur = list[i]
      val nxt = list[(i + 1) % sz]
      val dx0 = sign(cur.x - prv.x)
      val dy0 = sign(cur.y - prv.y)
      val dx1 = sign(nxt.x - cur.x)
      val dy1 = sign(nxt.y - cur.y)
      path.curveTo(
        cur.x - dx0 * akv,
        cur.y - dy0 * akv,
        cur.x + dx1 * akv,
        cur.y + dy1 * akv,
        cur.x + dx1 * arc,
        cur.y + dy1 * arc,
      )
      path.lineTo(nxt.x - dx1 * arc, nxt.y - dy1 * arc)
    }
    path.closePath()
    return path
  }

  fun singularization(rect: Area): List<Area> {
    val list = mutableListOf<Area>()
    val path = Path2D.Double()
    val pi = rect.getPathIterator(null)
    val coords = DoubleArray(6)
    while (!pi.isDone) {
      val pathSegmentType = pi.currentSegment(coords)
      when (pathSegmentType) {
        PathIterator.SEG_MOVETO -> path.moveTo(
          coords[0],
          coords[1],
        )

        PathIterator.SEG_LINETO -> path.lineTo(
          coords[0],
          coords[1],
        )

        PathIterator.SEG_QUADTO -> path.quadTo(
          coords[0],
          coords[1],
          coords[2],
          coords[3],
        )

        PathIterator.SEG_CUBICTO -> path.curveTo(
          coords[0],
          coords[1],
          coords[2],
          coords[3],
          coords[4],
          coords[5],
        )

        PathIterator.SEG_CLOSE -> path.also {
          it.closePath()
          list.add(Area(it))
          it.reset()
        }
      }
      pi.next()
    }
    return list
  }
}
