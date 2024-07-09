package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicComboBoxUI

private const val GAP = 4
private val left = SideLabel(Side.W)
private val right = SideLabel(Side.E)
private val top = SideLabel(Side.N)
private val bottom = SideLabel(Side.S)
private val topLeft = SideLabel(Side.NW)
private val topRight = SideLabel(Side.NE)
private val bottomLeft = SideLabel(Side.SW)
private val bottomRight = SideLabel(Side.SE)
private val contentPanel = JPanel(BorderLayout())
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
val mainContentPane get() = contentPanel

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(JScrollPane(JTree()))
  it.preferredSize = Dimension(320, 240)
}

fun makeFrame(str: String): JFrame {
  val frame = object : JFrame(str) {
    override fun getContentPane() = mainContentPane
  }
  frame.isUndecorated = true
  frame.background = Color(0x0, true)

  val title = JPanel(BorderLayout(GAP, GAP))
  title.isOpaque = false
  title.background = Color.ORANGE
  title.border = BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP)

  val check = JCheckBox("JCheckBox")
  check.isOpaque = false
  check.isFocusable = false

  val titleBox = Box.createHorizontalBox()
  titleBox.add(makeComboBox(str))
  titleBox.add(Box.createHorizontalGlue())
  titleBox.add(check)

  val button = makeTitleButton(ApplicationIcon())
  button.addActionListener { Toolkit.getDefaultToolkit().beep() }
  title.add(button, BorderLayout.WEST)
  title.add(titleBox)

  val close = makeTitleButton(CloseIcon())
  close.addActionListener { e ->
    val w = (e.source as? JComponent)?.topLevelAncestor
    if (w is Window) {
      w.dispatchEvent(WindowEvent(w, WindowEvent.WINDOW_CLOSING))
    }
  }
  title.add(close, BorderLayout.EAST)

  val rwl = ResizeWindowListener()
  listOf(left, right, top, bottom, topLeft, topRight, bottomLeft, bottomRight).forEach {
    it.addMouseListener(rwl)
    it.addMouseMotionListener(rwl)
  }

  val titlePanel = JPanel(BorderLayout())
  titlePanel.add(top, BorderLayout.NORTH)
  titlePanel.add(JLayer(title, TitleBarDragLayerUI()), BorderLayout.CENTER)

  val northPanel = JPanel(BorderLayout())
  northPanel.add(topLeft, BorderLayout.WEST)
  northPanel.add(titlePanel, BorderLayout.CENTER)
  northPanel.add(topRight, BorderLayout.EAST)

  val southPanel = JPanel(BorderLayout())
  southPanel.add(bottomLeft, BorderLayout.WEST)
  southPanel.add(bottom, BorderLayout.CENTER)
  southPanel.add(bottomRight, BorderLayout.EAST)
  resizePanel.add(left, BorderLayout.WEST)
  resizePanel.add(right, BorderLayout.EAST)
  resizePanel.add(northPanel, BorderLayout.NORTH)
  resizePanel.add(southPanel, BorderLayout.SOUTH)
  resizePanel.add(contentPanel, BorderLayout.CENTER)
  titlePanel.isOpaque = false
  northPanel.isOpaque = false
  southPanel.isOpaque = false
  contentPanel.isOpaque = false
  resizePanel.isOpaque = false
  frame.contentPane = resizePanel
  return frame
}

private fun makeTitleButton(icon: Icon): JButton {
  val button = JButton(icon)
  button.isContentAreaFilled = false
  button.isFocusPainted = false
  button.border = BorderFactory.createEmptyBorder()
  button.isOpaque = true
  button.background = Color.ORANGE
  return button
}

private fun makeComboBox(title: String): JComboBox<String> {
  val items = arrayOf(title, "$title (1)", "$title (2)", "$title (3)")
  val combo = object : JComboBox<String>(items) {
    override fun updateUI() {
      super.updateUI()
      val cui = object : BasicComboBoxUI() {
        override fun createArrowButton() = JButton().also {
          it.border = BorderFactory.createEmptyBorder()
          it.isVisible = false
        }
      }
      setUI(cui)
      isOpaque = true
      foreground = Color.BLACK
      background = Color.ORANGE
      border = BorderFactory.createEmptyBorder()
      isFocusable = false
      font = font.deriveFont(18f)
    }
  }
  val ml = object : MouseAdapter() {
    private fun getButtonModel(e: MouseEvent) =
      ((e.component as? JComboBox<*>)?.getComponent(0) as? JButton)?.model

    override fun mouseEntered(e: MouseEvent) {
      getButtonModel(e)?.isRollover = true
      e.component.background = Color.ORANGE.darker()
    }

    override fun mouseExited(e: MouseEvent) {
      getButtonModel(e)?.isRollover = false
      e.component.background = Color.ORANGE
    }

    override fun mousePressed(e: MouseEvent) {
      getButtonModel(e)?.isPressed = true
      combo.isPopupVisible = false
    }

    override fun mouseReleased(e: MouseEvent) {
      getButtonModel(e)?.isPressed = false
    }

    override fun mouseClicked(e: MouseEvent) {
      combo.isPopupVisible = true
    }
  }
  combo.addMouseListener(ml)
  return combo
}

private enum class Side(
  private val cursor: Int,
  private val width: Int,
  private val height: Int,
) {
  N(Cursor.N_RESIZE_CURSOR, 0, 4) {
    override fun getBounds(
      rect: Rectangle,
      delta: Point,
    ): Rectangle {
      rect.y += delta.y
      rect.height -= delta.y
      return rect
    }
  },
  W(Cursor.W_RESIZE_CURSOR, 4, 0) {
    override fun getBounds(
      rect: Rectangle,
      delta: Point,
    ): Rectangle {
      rect.x += delta.x
      rect.width -= delta.x
      return rect
    }
  },
  E(Cursor.E_RESIZE_CURSOR, 4, 0) {
    override fun getBounds(
      rect: Rectangle,
      delta: Point,
    ): Rectangle {
      rect.width += delta.x
      return rect
    }
  },
  S(Cursor.S_RESIZE_CURSOR, 0, 4) {
    override fun getBounds(
      rect: Rectangle,
      delta: Point,
    ): Rectangle {
      rect.height += delta.y
      return rect
    }
  },
  NW(Cursor.NW_RESIZE_CURSOR, 4, 4) {
    override fun getBounds(
      rect: Rectangle,
      delta: Point,
    ): Rectangle {
      rect.y += delta.y
      rect.height -= delta.y
      rect.x += delta.x
      rect.width -= delta.x
      return rect
    }
  },
  NE(Cursor.NE_RESIZE_CURSOR, 4, 4) {
    override fun getBounds(
      rect: Rectangle,
      delta: Point,
    ): Rectangle {
      rect.y += delta.y
      rect.height -= delta.y
      rect.width += delta.x
      return rect
    }
  },
  SW(Cursor.SW_RESIZE_CURSOR, 4, 4) {
    override fun getBounds(
      rect: Rectangle,
      delta: Point,
    ): Rectangle {
      rect.height += delta.y
      rect.x += delta.x
      rect.width -= delta.x
      return rect
    }
  },
  SE(Cursor.SE_RESIZE_CURSOR, 4, 4) {
    override fun getBounds(
      rect: Rectangle,
      delta: Point,
    ): Rectangle {
      rect.height += delta.y
      rect.width += delta.x
      return rect
    }
  }, ;

  val size get() = Dimension(width, height)

  fun getCursor(): Cursor = Cursor.getPredefinedCursor(cursor)

  abstract fun getBounds(
    rect: Rectangle,
    delta: Point,
  ): Rectangle
}

private class SideLabel(
  val side: Side,
) : JLabel() {
  init {
    cursor = side.getCursor()
  }

  override fun getPreferredSize() = side.size

  override fun getMinimumSize() = preferredSize

  override fun getMaximumSize() = preferredSize
}

private class ResizeWindowListener : MouseInputAdapter() {
  private val rect = Rectangle()

  override fun mousePressed(e: MouseEvent) {
    val p = SwingUtilities.getRoot(e.component)
    if (p is Window) {
      rect.bounds = p.getBounds()
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    val p = SwingUtilities.getRoot(c)
    if (!rect.isEmpty && c is SideLabel && p is Window) {
      val side = c.side
      p.setBounds(side.getBounds(rect, e.point))
    }
  }
}

private class TitleBarDragLayerUI : LayerUI<JComponent>() {
  private val startPt = Point()

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(
    e: MouseEvent,
    l: JLayer<out JComponent>,
  ) {
    if (e.id == MouseEvent.MOUSE_PRESSED && SwingUtilities.isLeftMouseButton(e)) {
      startPt.location = e.point
    }
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out JComponent>,
  ) {
    val c = SwingUtilities.getRoot(e.component)
    val isLeftButton = SwingUtilities.isLeftMouseButton(e)
    if (e.id == MouseEvent.MOUSE_DRAGGED && c is Window && isLeftButton) {
      val pt = c.getLocation()
      c.setLocation(pt.x - startPt.x + e.x, pt.y - startPt.y + e.y)
    }
  }
}

private class ApplicationIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.translate(x, y)
    g2.paint = Color.BLUE
    g2.fillOval(4, 4, 11, 11)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
}

private class CloseIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
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
    makeFrame("title").apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      minimumSize = Dimension(100, 100)
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
