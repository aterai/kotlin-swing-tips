package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.util.EnumSet
import java.util.function.BiFunction
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter

private val left = SideLabel(Side.W)
private val right = SideLabel(Side.E)
private val top = SideLabel(Side.N)
private val bottom = SideLabel(Side.S)
private val topLeft = SideLabel(Side.NW)
private val topRight = SideLabel(Side.NE)
private val bottomLeft = SideLabel(Side.SW)
private val bottomRight = SideLabel(Side.SE)
private val resizePanel = object : JPanel(BorderLayout()) {
  private val borderColor = Color(0x64_64_64)
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = width
    val h = height
    g2.paint = Color.ORANGE
    g2.fillRect(0, 0, w, h)
    g2.paint = borderColor
    g2.drawRect(0, 0, w - 1, h - 1)
    g2.drawLine(0, 2, 2, 0)
    g2.drawLine(w - 3, 0, w - 1, 2)
    g2.clearRect(0, 0, 2, 1)
    g2.clearRect(0, 0, 1, 2)
    g2.clearRect(w - 2, 0, 2, 1)
    g2.clearRect(w - 1, 0, 1, 2)
    g2.dispose()
  }
}

fun makeUI() = JScrollPane(JTree()).also {
  it.preferredSize = Dimension(320, 240)
}

fun makeResizableContentPane(title: String): Container {
  val titleBar = JPanel(BorderLayout())
  val dwl = DragWindowListener()
  titleBar.addMouseListener(dwl)
  titleBar.addMouseMotionListener(dwl)
  titleBar.isOpaque = false

  val gap = 4
  titleBar.border = BorderFactory.createEmptyBorder(gap, gap, gap, gap)
  titleBar.add(JLabel(title, SwingConstants.CENTER))
  titleBar.add(makeCloseButton(), BorderLayout.EAST)

  val rwl = ResizeWindowListener()
  listOf(left, right, top, bottom, topLeft, topRight, bottomLeft, bottomRight)
    .forEach {
      it.addMouseListener(rwl)
      it.addMouseMotionListener(rwl)
    }

  val titlePanel = JPanel(BorderLayout()).also {
    it.add(top, BorderLayout.NORTH)
    it.add(titleBar, BorderLayout.CENTER)
    it.isOpaque = false
  }

  val northPanel = JPanel(BorderLayout()).also {
    it.add(topLeft, BorderLayout.WEST)
    it.add(titlePanel, BorderLayout.CENTER)
    it.add(topRight, BorderLayout.EAST)
    it.isOpaque = false
  }

  val southPanel = JPanel(BorderLayout()).also {
    it.add(bottomLeft, BorderLayout.WEST)
    it.add(bottom, BorderLayout.CENTER)
    it.add(bottomRight, BorderLayout.EAST)
    it.isOpaque = false
  }

  resizePanel.add(left, BorderLayout.WEST)
  resizePanel.add(right, BorderLayout.EAST)
  resizePanel.add(northPanel, BorderLayout.NORTH)
  resizePanel.add(southPanel, BorderLayout.SOUTH)
  resizePanel.isOpaque = false

  return resizePanel
}

private fun makeCloseButton() = JButton(CloseIcon()).also {
  it.isContentAreaFilled = false
  it.isFocusPainted = false
  it.border = BorderFactory.createEmptyBorder()
  it.isOpaque = true
  it.background = Color.ORANGE
  it.addActionListener { e ->
    ((e.source as? JComponent)?.topLevelAncestor as? Window)?.also { window ->
      window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }
  }
}

private enum class Side(
  val cursor: Int,
  val size: Dimension,
  val resize: BiFunction<Rectangle, Point, Rectangle>
) {
  N(
    Cursor.N_RESIZE_CURSOR,
    Dimension(0, 4),
    BiFunction { r, d ->
      r.y += d.y
      r.height -= d.y
      r
    }
  ),
  W(
    Cursor.W_RESIZE_CURSOR,
    Dimension(4, 0),
    BiFunction { r, d ->
      r.x += d.x
      r.width -= d.x
      r
    }
  ),
  E(
    Cursor.E_RESIZE_CURSOR,
    Dimension(4, 0),
    BiFunction { r, d ->
      r.width += d.x
      r
    }
  ),
  S(
    Cursor.S_RESIZE_CURSOR,
    Dimension(0, 4),
    BiFunction { r, d ->
      r.height += d.y
      r
    }
  ),
  NW(
    Cursor.NW_RESIZE_CURSOR,
    Dimension(4, 4),
    BiFunction { r, d ->
      r.y += d.y
      r.height -= d.y
      r.x += d.x
      r.width -= d.x
      r
    }
  ),
  NE(
    Cursor.NE_RESIZE_CURSOR,
    Dimension(4, 4),
    BiFunction { r, d ->
      r.y += d.y
      r.height -= d.y
      r.width += d.x
      r
    }
  ),
  SW(
    Cursor.SW_RESIZE_CURSOR,
    Dimension(4, 4),
    BiFunction { r, d ->
      r.height += d.y
      r.x += d.x
      r.width -= d.x
      r
    }
  ),
  SE(
    Cursor.SE_RESIZE_CURSOR,
    Dimension(4, 4),
    BiFunction { r, d ->
      r.height += d.y
      r.width += d.x
      r
    }
  );

  companion object {
    fun getByType(cursor: Int): Side? = EnumSet.allOf(Side::class.java).first { d -> d.cursor == cursor }
  }
}

private class SideLabel(private val side: Side) : JLabel() {
  init {
    cursor = Cursor.getPredefinedCursor(side.cursor)
  }

  override fun getPreferredSize() = side.size

  override fun getMinimumSize() = preferredSize

  override fun getMaximumSize() = preferredSize
}

private class ResizeWindowListener : MouseInputAdapter() {
  private val rect = Rectangle()
  override fun mousePressed(e: MouseEvent) {
    (SwingUtilities.getRoot(e.component) as? Window)?.also {
      rect.bounds = it.bounds
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    val p = SwingUtilities.getRoot(c)
    if (!rect.isEmpty && c is SideLabel && p is Window) {
      val side = Side.getByType(c.cursor.type) ?: return
      p.setBounds(side.resize.apply(rect, e.point))
    }
  }
}

private class DragWindowListener : MouseInputAdapter() {
  private val startPt = Point()
  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.location = e.point
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = SwingUtilities.getRoot(e.component)
    if (c is Window && SwingUtilities.isLeftMouseButton(e)) {
      val pt = c.location
      c.setLocation(pt.x - startPt.x + e.x, pt.y - startPt.y + e.y)
    }
  }
}

private class CloseIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.BLACK
    g2.drawLine(4, 4, 11, 11)
    g2.drawLine(4, 5, 10, 11)
    g2.drawLine(5, 4, 11, 10)
    g2.drawLine(11, 4, 4, 11)
    g2.drawLine(11, 5, 5, 11)
    g2.drawLine(10, 4, 4, 10)
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
      isUndecorated = true
      background = Color(0x0, true)
      contentPane = makeResizableContentPane("title")
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
