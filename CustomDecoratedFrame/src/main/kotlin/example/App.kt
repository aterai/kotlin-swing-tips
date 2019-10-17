package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.util.EnumSet
import java.util.function.BiFunction
import javax.swing.*
import javax.swing.event.MouseInputAdapter

class MainPanel : JPanel(BorderLayout()) {
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
      val w = getWidth()
      val h = getHeight()
      g2.setPaint(Color.ORANGE)
      g2.fillRect(0, 0, w, h)
      g2.setPaint(borderColor)
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

  init {
    add(JScrollPane(JTree()))
    setPreferredSize(Dimension(320, 240))
  }

  fun makeFrame(title: String): JFrame {
    val titleBar = JPanel(BorderLayout())
    val dwl = DragWindowListener()
    titleBar.addMouseListener(dwl)
    titleBar.addMouseMotionListener(dwl)
    titleBar.setOpaque(false)

    titleBar.setBorder(BorderFactory.createEmptyBorder(W, W, W, W))
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
      it.setOpaque(false)
    }

    val northPanel = JPanel(BorderLayout()).also {
      it.add(topLeft, BorderLayout.WEST)
      it.add(titlePanel, BorderLayout.CENTER)
      it.add(topRight, BorderLayout.EAST)
      it.setOpaque(false)
    }

    val southPanel = JPanel(BorderLayout()).also {
      it.add(bottomLeft, BorderLayout.WEST)
      it.add(bottom, BorderLayout.CENTER)
      it.add(bottomRight, BorderLayout.EAST)
      it.setOpaque(false)
    }

    resizePanel.add(left, BorderLayout.WEST)
    resizePanel.add(right, BorderLayout.EAST)
    resizePanel.add(northPanel, BorderLayout.NORTH)
    resizePanel.add(southPanel, BorderLayout.SOUTH)
    resizePanel.setOpaque(false)

    val frame = JFrame(title)
    frame.setUndecorated(true)
    frame.setBackground(Color(0x0, true))
    frame.setContentPane(resizePanel)
    return frame
  }

  private fun makeCloseButton() = JButton(CloseIcon()).also {
    it.setContentAreaFilled(false)
    it.setFocusPainted(false)
    it.setBorder(BorderFactory.createEmptyBorder())
    it.setOpaque(true)
    it.setBackground(Color.ORANGE)
    it.addActionListener { e ->
      val b = e.getSource() as? JComponent ?: return@addActionListener
      (b.getTopLevelAncestor() as? Window)?.also { window ->
        window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
      }
    }
  }

  companion object {
    private const val W = 4
  }
}

enum class Side(
  val cursor: Int,
  val size: Dimension,
  val resize: BiFunction<Rectangle, Point, Rectangle>
) {
  N(
    Cursor.N_RESIZE_CURSOR, Dimension(0, 4),
    BiFunction<Rectangle, Point, Rectangle> { r, d ->
      r.y += d.y
      r.height -= d.y
      r
    }
  ),
  W(
    Cursor.W_RESIZE_CURSOR, Dimension(4, 0),
    BiFunction<Rectangle, Point, Rectangle> { r, d ->
      r.x += d.x
      r.width -= d.x
      r
    }
  ),
  E(
    Cursor.E_RESIZE_CURSOR, Dimension(4, 0),
    BiFunction<Rectangle, Point, Rectangle> { r, d ->
      r.width += d.x
      r
    }
  ),
  S(
    Cursor.S_RESIZE_CURSOR, Dimension(0, 4),
    BiFunction<Rectangle, Point, Rectangle> { r, d ->
      r.height += d.y
      r
    }
  ),
  NW(
    Cursor.NW_RESIZE_CURSOR, Dimension(4, 4),
    BiFunction<Rectangle, Point, Rectangle> { r, d ->
      r.y += d.y
      r.height -= d.y
      r.x += d.x
      r.width -= d.x
      r
    }
  ),
  NE(
    Cursor.NE_RESIZE_CURSOR, Dimension(4, 4),
    BiFunction<Rectangle, Point, Rectangle> { r, d ->
      r.y += d.y
      r.height -= d.y
      r.width += d.x
      r
    }
  ),
  SW(
    Cursor.SW_RESIZE_CURSOR, Dimension(4, 4),
    BiFunction<Rectangle, Point, Rectangle> { r, d ->
      r.height += d.y
      r.x += d.x
      r.width -= d.x
      r
    }
  ),
  SE(
    Cursor.SE_RESIZE_CURSOR, Dimension(4, 4),
    BiFunction<Rectangle, Point, Rectangle> { r, d ->
        r.height += d.y
        r.width += d.x
        r
    }
  );

  companion object {
    fun getByType(cursor: Int): Side? = EnumSet.allOf(Side::class.java).first { d -> d.cursor == cursor }
  }
}

class SideLabel(private val side: Side) : JLabel() {
  override fun getPreferredSize() = side.size

  override fun getMinimumSize() = getPreferredSize()

  override fun getMaximumSize() = getPreferredSize()

  init {
    setCursor(Cursor.getPredefinedCursor(side.cursor))
  }
}

class ResizeWindowListener : MouseInputAdapter() {
  private val rect = Rectangle()
  override fun mousePressed(e: MouseEvent) {
    (SwingUtilities.getRoot(e.getComponent()) as? Window)?.also {
      rect.setBounds(it.getBounds())
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = e.getComponent()
    val p = SwingUtilities.getRoot(c)
    if (!rect.isEmpty() && c is SideLabel && p is Window) {
      val side = Side.getByType(c.cursor.getType()) ?: return
      p.setBounds(side.resize.apply(rect, e.getPoint()))
    }
  }
}

class DragWindowListener : MouseInputAdapter() {
  private val startPt = Point()
  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.setLocation(e.getPoint())
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = SwingUtilities.getRoot(e.getComponent())
    if (c is Window && SwingUtilities.isLeftMouseButton(e)) {
      val pt = c.getLocation()
      c.setLocation(pt.x - startPt.x + e.x, pt.y - startPt.y + e.y)
    }
  }
}

class CloseIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.setPaint(Color.BLACK)
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
    val p = MainPanel()
    p.makeFrame("title").apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(p)
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
