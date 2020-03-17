package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.util.EnumSet
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
    tree.setVisibleRowCount(8)
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

    val cursor: Cursor = Cursor.getPredefinedCursor(cursor)

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
    val actualBounds = Rectangle(SIZE, SIZE, w - 2 * SIZE, h - 2 * SIZE)
    if (!bounds.contains(pt) || actualBounds.contains(pt)) {
      return Cursor.getDefaultCursor()
    }
    val rect = Rectangle(SIZE, SIZE)
    val r = Rectangle(0, 0, w, h)
    var cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
    for (loc in Locations.values()) {
      rect.setLocation(loc.getPoint(r))
      if (rect.contains(pt)) {
        cursor = loc.cursor
      }
    }
    return cursor
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
    e.getComponent().setCursor(Cursor.getDefaultCursor())
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.getComponent()
    val border = (c as? JComponent)?.border as? ResizableBorder ?: return
    cursor = border.getResizeCursor(e)
    startPos.setLocation(SwingUtilities.convertPoint(c, e.x, e.y, null))
    startingBounds.setBounds(c.getBounds())
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
    val c = e.getComponent()
    val p = SwingUtilities.convertPoint(c, e.x, e.y, null)
    val deltaX = startPos.x - p.x
    val deltaY = startPos.y - p.y
    val parent = SwingUtilities.getUnwrappedParent(c)
    Directions.getByCursorType(cursor.getType())?.also {
      val delta = it.getLimitedDelta(startingBounds, parent.getBounds(), deltaX, deltaY)
      c.setBounds(it.getBounds(startingBounds, delta))
    }
    parent.revalidate()
  }
}

enum class Directions(private val cursor: Int) {
  NORTH(Cursor.N_RESIZE_CURSOR) {
    override fun getLimitedDelta(
      startingBounds: Rectangle,
      parentBounds: Rectangle,
      deltaX: Int,
      deltaY: Int
    ) = Point(0, getDeltaY(deltaY, startingBounds))
    override fun getBounds(rect: Rectangle, delta: Point) = Rectangle(
      rect.x,
      rect.y - delta.y,
      rect.width,
      rect.height + delta.y
    )
  },
  SOUTH(Cursor.S_RESIZE_CURSOR) {
    override fun getLimitedDelta(
      startingBounds: Rectangle,
      parentBounds: Rectangle,
      deltaX: Int,
      deltaY: Int
    ) = Point(0, getDeltaY(deltaY, parentBounds, startingBounds))
    override fun getBounds(rect: Rectangle, delta: Point) = Rectangle(
      rect.x,
      rect.y,
      rect.width,
      rect.height - delta.y
    )
  },
  WEST(Cursor.W_RESIZE_CURSOR) {
    override fun getLimitedDelta(
      startingBounds: Rectangle,
      parentBounds: Rectangle,
      deltaX: Int,
      deltaY: Int
    ) = Point(getDeltaX(deltaX, startingBounds), 0)
    override fun getBounds(rect: Rectangle, delta: Point) = Rectangle(
      rect.x - delta.x,
      rect.y,
      rect.width + delta.x,
      rect.height
    )
  },
  EAST(Cursor.E_RESIZE_CURSOR) {
    override fun getLimitedDelta(
      startingBounds: Rectangle,
      parentBounds: Rectangle,
      deltaX: Int,
      deltaY: Int
    ) = Point(getDeltaX(deltaX, parentBounds, startingBounds), 0)
    override fun getBounds(rect: Rectangle, delta: Point) = Rectangle(
      rect.x,
      rect.y,
      rect.width - delta.x,
      rect.height
    )
  },
  NORTH_WEST(Cursor.NW_RESIZE_CURSOR) {
    override fun getLimitedDelta(
      startingBounds: Rectangle,
      parentBounds: Rectangle,
      deltaX: Int,
      deltaY: Int
    ) = Point(getDeltaX(deltaX, startingBounds), getDeltaY(deltaY, startingBounds))
    override fun getBounds(rect: Rectangle, delta: Point) = Rectangle(
      rect.x - delta.x,
      rect.y - delta.y,
      rect.width + delta.x,
      rect.height + delta.y
    )
  },
  NORTH_EAST(Cursor.NE_RESIZE_CURSOR) {
    override fun getLimitedDelta(
      startingBounds: Rectangle,
      parentBounds: Rectangle,
      deltaX: Int,
      deltaY: Int
    ) = Point(getDeltaX(deltaX, parentBounds, startingBounds), getDeltaY(deltaY, startingBounds))
    override fun getBounds(rect: Rectangle, delta: Point) = Rectangle(
      rect.x,
      rect.y - delta.y,
      rect.width - delta.x,
      rect.height + delta.y
    )
  },
  SOUTH_WEST(Cursor.SW_RESIZE_CURSOR) {
    override fun getLimitedDelta(
      startingBounds: Rectangle,
      parentBounds: Rectangle,
      deltaX: Int,
      deltaY: Int
    ) = Point(getDeltaX(deltaX, startingBounds), getDeltaY(deltaY, parentBounds, startingBounds))
    override fun getBounds(rect: Rectangle, delta: Point) = Rectangle(
      rect.x,
      rect.y,
      rect.width,
      rect.height
    )
  },
  SOUTH_EAST(Cursor.SE_RESIZE_CURSOR) {
    override fun getLimitedDelta(
      startingBounds: Rectangle,
      parentBounds: Rectangle,
      deltaX: Int,
      deltaY: Int
    ) = Point(getDeltaX(deltaX, parentBounds, startingBounds), getDeltaY(deltaY, parentBounds, startingBounds))
    override fun getBounds(rect: Rectangle, delta: Point) = Rectangle(
      rect.x,
      rect.y,
      rect.width - delta.x,
      rect.height - delta.y
    )
  },
  MOVE(Cursor.MOVE_CURSOR) {
    override fun getLimitedDelta(
      startingBounds: Rectangle,
      parentBounds: Rectangle,
      deltaX: Int,
      deltaY: Int
    ) = Point(deltaX, deltaY)
    override fun getBounds(rect: Rectangle, delta: Point) = Rectangle(
      rect.x - delta.x,
      rect.y - delta.y,
      rect.width,
      rect.height
    )
  };

  abstract fun getBounds(rect: Rectangle, delta: Point): Rectangle

  abstract fun getLimitedDelta(
    startingBounds: Rectangle,
    parentBounds: Rectangle,
    deltaX: Int,
    deltaY: Int
  ): Point

  companion object {
    private val MIN = Dimension(50, 50)
    private val MAX = Dimension(500, 500)

    fun getByCursorType(cursor: Int): Directions? =
      EnumSet.allOf(Directions::class.java).first { d -> d.cursor == cursor }

    private fun getDeltaX(dx: Int, startingBounds: Rectangle): Int {
      val left = minOf(
        MAX.width - startingBounds.width,
        startingBounds.x
      )
      return dx.coerceIn(MIN.width - startingBounds.width, left)
    }

    private fun getDeltaX(dx: Int, parentBounds: Rectangle, startingBounds: Rectangle): Int {
      val right = maxOf(
        startingBounds.width - MAX.width,
        startingBounds.x + startingBounds.width - parentBounds.width
      )
      return dx.coerceIn(right, startingBounds.width - MIN.width)
    }

    private fun getDeltaY(dy: Int, startingBounds: Rectangle): Int {
      val top = minOf(
        MAX.height - startingBounds.height,
        startingBounds.y
      )
      return dy.coerceIn(MIN.height - startingBounds.height, top)
    }

    private fun getDeltaY(dy: Int, parentBounds: Rectangle, startingBounds: Rectangle): Int {
      val bottom = maxOf(
        startingBounds.height - MAX.height,
        startingBounds.y + startingBounds.height - parentBounds.height
      )
      return dy.coerceIn(bottom, startingBounds.height - MIN.height)
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
