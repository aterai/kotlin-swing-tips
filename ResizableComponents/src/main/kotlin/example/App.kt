package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.util.EnumSet
import java.util.function.BiFunction
import java.util.function.Function
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

class MainPanel : JPanel(BorderLayout()) {
  private val layeredPane = object : JLayeredPane() {
    override fun isOptimizedDrawingEnabled() = false
  }
  private val toolbar = JToolBar("Resizable Components")
  private val pt = Point()
  private fun createTree() {
    val tree = JTree()
    tree.visibleRowCount = 8
    val c = JScrollPane(tree)
    val d = c.getPreferredSize()
    val resizer = JResizer(BorderLayout())
    resizer.add(c)
    resizer.setBounds(pt.x, pt.y, d.width, d.height)
    layeredPane.add(resizer)
    layeredPane.moveToFront(resizer)
  }

  private fun createTable() {
    val table = JTable(12, 3)
    table.setPreferredScrollableViewportSize(Dimension(160, 160))
    val c = JScrollPane(table)
    val d = c.getPreferredSize()
    val resizer = JResizer(BorderLayout())
    resizer.add(c)
    resizer.setBounds(pt.x, pt.y, d.width, d.height)
    layeredPane.add(resizer)
    layeredPane.moveToFront(resizer)
  }

  init {
    val popup = object : JPopupMenu() {
      override fun show(c: Component?, x: Int, y: Int) {
        pt.setLocation(x, y)
        super.show(c, x, y)
      }
    }
    popup.add("table").addActionListener { createTable() }
    popup.add("tree").addActionListener { createTree() }
    layeredPane.componentPopupMenu = popup

    add(layeredPane)
    toolbar.add(object : AbstractAction("add table") {
      override fun actionPerformed(e: ActionEvent) {
        pt.setLocation(pt.x + 20, pt.y + 20)
        createTable()
      }
    })
    toolbar.addSeparator()
    toolbar.add(object : AbstractAction("add tree") {
      override fun actionPerformed(e: ActionEvent) {
        pt.setLocation(pt.x + 20, pt.y + 20)
        createTree()
      }
    })
    add(toolbar, BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
  }
}

class JResizer(layout: LayoutManager) : JPanel(layout) { // implements Serializable {
  @Transient
  private var resizeListener: MouseInputListener? = null

  override fun updateUI() {
    removeMouseListener(resizeListener)
    removeMouseMotionListener(resizeListener)
    super.updateUI()
    resizeListener = ResizeMouseListener()
    addMouseListener(resizeListener)
    addMouseMotionListener(resizeListener)
    setBorder(DefaultResizableBorder())
  }

  override fun setBorder(border: Border?) {
    removeMouseListener(resizeListener)
    removeMouseMotionListener(resizeListener)
    if (border is ResizableBorder) {
      addMouseListener(resizeListener)
      addMouseMotionListener(resizeListener)
    }
    super.setBorder(border)
  }
}

interface ResizableBorder : Border {
  fun getResizeCursor(e: MouseEvent): Cursor
}

class DefaultResizableBorder : ResizableBorder, SwingConstants {
  private enum class Locations(cursor: Int, private val location: Function<Rectangle, Point>) {
    NORTH(
      Cursor.N_RESIZE_CURSOR,
      Function { r ->
        Point(
          r.x + r.width / 2 - SIZE / 2,
          r.y
        )
      }
    ),
    SOUTH(
      Cursor.S_RESIZE_CURSOR,
      Function { r ->
        Point(
          r.x + r.width / 2 - SIZE / 2,
          r.y + r.height - SIZE
        )
      }
    ),
    WEST(
      Cursor.W_RESIZE_CURSOR,
      Function { r ->
        Point(
          r.x,
          r.y + r.height / 2 - SIZE / 2
        )
      }
    ),
    EAST(
      Cursor.E_RESIZE_CURSOR,
      Function { r ->
        Point(
          r.x + r.width - SIZE,
          r.y + r.height / 2 - SIZE / 2
        )
      }
    ),
    NORTH_WEST(
      Cursor.NW_RESIZE_CURSOR,
      Function { r ->
        Point(
          r.x,
          r.y
        )
      }
    ),
    NORTH_EAST(
      Cursor.NE_RESIZE_CURSOR,
      Function { r ->
        Point(
          r.x + r.width - SIZE,
          r.y
        )
      }
    ),
    SOUTH_WEST(
      Cursor.SW_RESIZE_CURSOR,
      Function { r ->
        Point(
          r.x,
          r.y + r.height - SIZE
        )
      }
    ),
    SOUTH_EAST(
      Cursor.SE_RESIZE_CURSOR,
      Function { r ->
        Point(
          r.x + r.width - SIZE,
          r.y + r.height - SIZE
        )
      }
    );

    val cursor = Cursor.getPredefinedCursor(cursor)

    fun getPoint(r: Rectangle) = location.apply(r)
  }

  override fun getBorderInsets(component: Component?) = Insets(SIZE, SIZE, SIZE, SIZE)

  override fun isBorderOpaque() = false

  override fun paintBorder(
    component: Component?,
    g: Graphics,
    x: Int,
    y: Int,
    w: Int,
    h: Int
  ) {
    g.setColor(Color.BLACK)
    g.drawRect(x + SIZE / 2, y + SIZE / 2, w - SIZE, h - SIZE)
    val rect = Rectangle(SIZE, SIZE)
    val r = Rectangle(x, y, w, h)
    for (loc in Locations.values()) {
      rect.setLocation(loc.getPoint(r))
      g.setColor(Color.WHITE)
      g.fillRect(rect.x, rect.y, rect.width - 1, rect.height - 1)
      g.setColor(Color.BLACK)
      g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1)
    }
  }

  override fun getResizeCursor(e: MouseEvent): Cursor {
    val c = e.getComponent()
    val w = c.width
    val h = c.height
    val pt = e.getPoint()
    val bounds = Rectangle(w, h)
    if (!bounds.contains(pt)) {
      return Cursor.getDefaultCursor()
    }
    val actualBounds = Rectangle(SIZE, SIZE, w - 2 * SIZE, h - 2 * SIZE)
    if (actualBounds.contains(pt)) {
      return Cursor.getDefaultCursor()
    }
    val rect = Rectangle(SIZE, SIZE)
    val r = Rectangle(0, 0, w, h)
    for (loc in Locations.values()) {
      rect.setLocation(loc.getPoint(r))
      if (rect.contains(pt)) {
        return loc.cursor
      }
    }
    return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
  }

  companion object {
    private const val SIZE = 6
  }
}

class ResizeMouseListener : MouseInputAdapter() {
  private var cursor = Cursor.getDefaultCursor()
  private val startPos = Point(-1, -1)
  private val startingBounds = Rectangle()
  override fun mouseMoved(e: MouseEvent) {
    val c = e.getComponent() as? JComponent ?: return
    (c.getBorder() as? ResizableBorder)?.also {
      c.setCursor(it.getResizeCursor(e))
    }
  }

  override fun mouseExited(e: MouseEvent) {
    e.component.cursor = Cursor.getDefaultCursor()
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    val border = (c as? JComponent)?.border as? ResizableBorder ?: return
    cursor = border.getResizeCursor(e)
    startPos.setLocation(SwingUtilities.convertPoint(c, e.x, e.y, null))
    startingBounds.setBounds(c.bounds)
    (SwingUtilities.getAncestorOfClass(JLayeredPane::class.java, c) as? JLayeredPane)?.moveToFront(c)
  }

  override fun mouseReleased(e: MouseEvent) {
    startingBounds.setSize(0, 0)
  }

  // @see %JAVA_HOME%/src/javax/swing/plaf/basic/BasicInternalFrameUI.java
  override fun mouseDragged(e: MouseEvent) {
    if (startingBounds.isEmpty()) {
      return
    }
    val c = e.component
    val p = SwingUtilities.convertPoint(c, e.x, e.y, null)
    val deltaX = startPos.x - p.x
    val deltaY = startPos.y - p.y
    val parent = SwingUtilities.getUnwrappedParent(c)
    Directions.getByCursorType(cursor.type)?.also {
      val delta = getLimitedDelta(parent.bounds, deltaX, deltaY)
      c.bounds = it.getBounds(startingBounds, delta)
    }
    parent.revalidate()
  }

  private fun getDeltaX(dx: Int): Int {
    val left = minOf(
      MAX.width - startingBounds.width,
      startingBounds.x
    )
    return dx.coerceIn(MIN.width - startingBounds.width, left)
  }

  private fun getDeltaX(dx: Int, parentBounds: Rectangle): Int {
    val right = maxOf(
      startingBounds.width - MAX.width,
      startingBounds.x + startingBounds.width - parentBounds.width
    )
    return dx.coerceIn(right, startingBounds.width - MIN.width)
  }

  private fun getDeltaY(dy: Int): Int {
    val top = minOf(
      MAX.height - startingBounds.height,
      startingBounds.y
    )
    return dy.coerceIn(MIN.height - startingBounds.height, top)
  }

  private fun getDeltaY(dy: Int, parentBounds: Rectangle): Int {
    val bottom = maxOf(
      startingBounds.height - MAX.height,
      startingBounds.y + startingBounds.height - parentBounds.height
    )
    return dy.coerceIn(bottom, startingBounds.height - MIN.height)
  }

  private fun getLimitedDelta(
    parentBounds: Rectangle,
    deltaX: Int,
    deltaY: Int
  ) = when (cursor.type) {
      Cursor.N_RESIZE_CURSOR -> Point(0, getDeltaY(deltaY))
      Cursor.S_RESIZE_CURSOR -> Point(0, getDeltaY(deltaY, parentBounds))
      Cursor.W_RESIZE_CURSOR -> Point(getDeltaX(deltaX), 0)
      Cursor.E_RESIZE_CURSOR -> Point(getDeltaX(deltaX, parentBounds), 0)
      Cursor.NW_RESIZE_CURSOR -> Point(getDeltaX(deltaX), getDeltaY(deltaY))
      Cursor.SW_RESIZE_CURSOR -> Point(getDeltaX(deltaX), getDeltaY(deltaY, parentBounds))
      Cursor.NE_RESIZE_CURSOR -> Point(getDeltaX(deltaX, parentBounds), getDeltaY(deltaY))
      Cursor.SE_RESIZE_CURSOR -> Point(getDeltaX(deltaX, parentBounds), getDeltaY(deltaY, parentBounds))
      else -> Point(deltaX, deltaY)
    }

  companion object {
    private val MIN = Dimension(50, 50)
    private val MAX = Dimension(500, 500)
  }
}

enum class Directions(
  private val cursor: Int,
  private val getBounds: BiFunction<Rectangle, Point, Rectangle>
) {
  NORTH(
    Cursor.N_RESIZE_CURSOR,
    BiFunction { r, d ->
      Rectangle(
        r.x,
        r.y - d.y,
        r.width,
        r.height + d.y
      )
    }
  ),
  SOUTH(
    Cursor.S_RESIZE_CURSOR,
    BiFunction { r, d ->
      Rectangle(
        r.x,
        r.y,
        r.width,
        r.height - d.y
      )
    }
  ),
  WEST(
    Cursor.W_RESIZE_CURSOR,
    BiFunction { r, d ->
      Rectangle(
        r.x - d.x,
        r.y,
        r.width + d.x,
        r.height
      )
    }
  ),
  EAST(
    Cursor.E_RESIZE_CURSOR,
    BiFunction { r, d ->
      Rectangle(
        r.x,
        r.y,
        r.width - d.x,
        r.height
      )
    }
  ),
  NORTH_WEST(
    Cursor.NW_RESIZE_CURSOR,
    BiFunction { r, d ->
      Rectangle(
        r.x - d.x,
        r.y - d.y,
        r.width + d.x,
        r.height + d.y
      )
    }
  ),
  NORTH_EAST(
    Cursor.NE_RESIZE_CURSOR,
    BiFunction { r, d ->
      Rectangle(
        r.x,
        r.y - d.y,
        r.width - d.x,
        r.height + d.y
      )
    }
  ),
  SOUTH_WEST(
    Cursor.SW_RESIZE_CURSOR,
    BiFunction { r, _ ->
      Rectangle(
        r.x,
        r.y,
        r.width,
        r.height
      )
    }
  ),
  SOUTH_EAST(
    Cursor.SE_RESIZE_CURSOR,
    BiFunction { r, d ->
      Rectangle(
        r.x,
        r.y,
        r.width - d.x,
        r.height - d.y
      )
    }
  ),
  MOVE(
    Cursor.MOVE_CURSOR,
    BiFunction { r, d ->
      Rectangle(
        r.x - d.x,
        r.y - d.y,
        r.width,
        r.height
      )
    }
  );

  fun getBounds(rect: Rectangle, delta: Point) = getBounds.apply(rect, delta)

  companion object {
    fun getByCursorType(cursor: Int): Directions? =
        EnumSet.allOf(Directions::class.java).first { d -> d.cursor == cursor }
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
