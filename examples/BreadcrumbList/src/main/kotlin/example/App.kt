package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

fun createUI(): Component {
  val breadcrumb = createContainer(10 + 1)
  val tree = JTree()
  tree.setSelectionRow(0)
  tree.addTreeSelectionListener {
    val o = tree.lastSelectedPathComponent
    // A leaf node is not added to the breadcrumb trail, keep the current one
    if (o is TreeNode && !o.isLeaf) {
      initBreadcrumbList(breadcrumb, tree)
    }
  }
  initBreadcrumbList(breadcrumb, tree)
  return JPanel(BorderLayout()).also {
    it.add(JLayer(breadcrumb, BreadcrumbLayerUI<Component>()), BorderLayout.NORTH)
    val c = createBreadcrumbList(listOf("aaa", "bb", "c"))
    it.add(c, BorderLayout.SOUTH)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createContainer(overlap: Int): Container {
  val p = object : JPanel(FlowLayout(FlowLayout.LEADING, -overlap, 0)) {
    // The child buttons overlap each other
    override fun isOptimizedDrawingEnabled() = false
  }
  p.border = BorderFactory.createEmptyBorder(4, overlap + 4, 4, 4)
  p.isOpaque = false
  return p
}

private fun initBreadcrumbList(
  p: Container,
  tree: JTree,
) {
  p.removeAll()
  val bg = ButtonGroup()
  val nodes = tree.selectionPath?.path ?: emptyArray()
  for (i in nodes.indices) {
    val path = TreePath(nodes.copyOf(i + 1))
    val b = createButton(nodes[i].toString(), Color.ORANGE)
    b.addActionListener { tree.selectionPath = path }
    p.add(b)
    bg.add(b)
    // Highlight the button of the current (last) node
    b.isSelected = i == nodes.lastIndex
  }
  p.revalidate()
  p.repaint()
}

private fun createBreadcrumbList(list: List<String>): Component {
  val p = createContainer(5 + 1)
  val bg = ButtonGroup()
  list
    .map { createButton(it, Color.PINK) }
    .forEach {
      p.add(it)
      bg.add(it)
    }
  return p
}

private fun createButton(
  title: String,
  color: Color,
): AbstractButton {
  val b = object : JRadioButton(title) {
    // Respond to the mouse only inside the arrow shape of the icon
    override fun contains(
      x: Int,
      y: Int,
    ) = (icon as? ArrowToggleButtonBarCellIcon)?.let {
      it.shape?.contains(Point(x, y))
    } ?: super.contains(x, y)
  }
  b.icon = ArrowToggleButtonBarCellIcon()
  b.isContentAreaFilled = false
  b.border = BorderFactory.createEmptyBorder()
  b.verticalAlignment = SwingConstants.CENTER
  b.verticalTextPosition = SwingConstants.CENTER
  b.horizontalAlignment = SwingConstants.CENTER
  b.horizontalTextPosition = SwingConstants.CENTER
  b.isFocusPainted = false
  b.isOpaque = false
  b.background = color
  return b
}

// https://ateraimemo.com/Swing/ToggleButtonBar.html
// https://java-swing-tips.blogspot.com/2012/11/make-togglebuttonbar-with-jradiobuttons.html
private class ArrowToggleButtonBarCellIcon : Icon {
  var shape: Shape? = null
    private set

  private fun createShape(
    parent: Container,
    c: Component,
    x: Int,
    y: Int,
  ): Shape {
    val w = c.width - 1.0
    val h = c.height - 1.0
    val h2 = h * .5
    val w2 = TH.toDouble()
    val p = Path2D.Double()
    p.moveTo(0.0, 0.0)
    p.lineTo(w - w2, 0.0)
    p.lineTo(w, h2)
    p.lineTo(w - w2, h)
    p.lineTo(0.0, h)
    if (c !== parent.getComponent(0)) {
      p.lineTo(w2, h2)
    }
    p.closePath()
    val tx = x.toDouble()
    val ty = y.toDouble()
    return AffineTransform.getTranslateInstance(tx, ty).createTransformedShape(p)
  }

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val parent = c.parent ?: return
    shape = createShape(parent, c, x, y)
    var bgc = parent.background
    var borderColor = Color.GRAY.brighter()
    if (c is AbstractButton) {
      val m = c.model
      if (m.isSelected || m.isRollover) {
        bgc = c.background
        borderColor = Color.GRAY
      }
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = bgc
    g2.fill(shape)
    g2.paint = borderColor
    g2.draw(shape)
    g2.dispose()
  }

  override fun getIconWidth() = WIDTH

  override fun getIconHeight() = HEIGHT

  companion object {
    const val TH = 10 // The height of a triangle
    private const val HEIGHT = TH * 2 + 1
    private const val WIDTH = 100
  }
}

private class BreadcrumbLayerUI<V : Component> : LayerUI<V>() {
  private var shape: Shape? = null

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    shape?.also {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = Color.GRAY
      g2.draw(it)
      g2.dispose()
    }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask =
        AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  private fun update(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
    val s = when (e.id) {
      MouseEvent.MOUSE_ENTERED, MouseEvent.MOUSE_MOVED -> {
        (e.component as? AbstractButton)?.let { button ->
          (button.icon as? ArrowToggleButtonBarCellIcon)?.let { icon ->
            val r = button.bounds
            val at = AffineTransform.getTranslateInstance(
              r.x.toDouble(),
              r.y.toDouble(),
            )
            at.createTransformedShape(icon.shape)
          }
        }
      }

      else -> {
        null
      }
    }
    if (s != shape) {
      shape = s
      l.view.repaint()
    }
  }

  override fun processMouseEvent(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
    update(e, l)
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
    update(e, l)
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
