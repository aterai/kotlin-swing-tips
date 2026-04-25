package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.util.EnumSet
import javax.swing.*

private val COLOR_BODY = Color.WHITE
private val COLOR_CLICK = Color(173, 216, 230)
private val COLOR_WHEEL_IDLE = Color.LIGHT_GRAY
private val COLOR_WHEEL_CLICK = Color(255, 50, 50)
private val COLOR_WHEEL_MOVE = Color(100, 150, 255)
private val COLOR_LINE = Color(60, 60, 60)
private const val ICON_W = 100
private const val ICON_H = 120
private const val ICON_ARC = 60f
private const val WHEEL_W = 20
private const val WHEEL_H = 30
private const val WHEEL_ARC = 10f
private const val STROKE_W = 4f

private val pressedButtons = EnumSet.noneOf(MouseButton::class.java)
private val logArea = JTextArea()
private val visualizerLabel = JLabel(MouseVisualizer())
private val wheelResetTimer = Timer(100, null)
private var wheelOffset = 0

fun createUI(): Component {
  visualizerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
  visualizerLabel.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      updateButton(e, true)
    }

    override fun mouseReleased(e: MouseEvent) {
      updateButton(e, false)
    }
  })
  visualizerLabel.addMouseWheelListener { handleWheel(it) }
  wheelResetTimer.addActionListener {
    wheelOffset = 0
    visualizerLabel.repaint()
  }
  wheelResetTimer.isRepeats = false

  logArea.isEditable = false
  return JPanel(BorderLayout()).also {
    it.add(visualizerLabel, BorderLayout.NORTH)
    it.add(JScrollPane(logArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateButton(e: MouseEvent, isPressed: Boolean) {
  val btn = if (SwingUtilities.isLeftMouseButton(e)) {
    MouseButton.LEFT
  } else if (SwingUtilities.isMiddleMouseButton(e)) {
    MouseButton.MIDDLE
  } else if (SwingUtilities.isRightMouseButton(e)) {
    MouseButton.RIGHT
  } else {
    null
  }
  if (btn != null) {
    if (isPressed) {
      pressedButtons.add(btn)
    } else {
      pressedButtons.remove(btn)
    }
    logEvent((if (isPressed) "Pressed: " else "Released: ") + btn)
    visualizerLabel.repaint()
  }
}

private fun handleWheel(e: MouseWheelEvent) {
  val rotation = e.getWheelRotation()
  val direction = if (rotation < 0) "UP" else "DOWN"
  wheelOffset = if (rotation < 0) -5 else 5
  logEvent("Wheel Moved: $direction (Amount: $rotation)")
  visualizerLabel.repaint()
  wheelResetTimer.restart()
}

private fun logEvent(msg: String) {
  logArea.append(msg + "\n")
  logArea.setCaretPosition(logArea.document.length)
}

private class MouseVisualizer : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val xx = x + STROKE_W / 2f
    val yy = y + STROKE_W / 2f
    val w1 = iconWidth - STROKE_W
    val h1 = iconHeight - STROKE_W
    val body = Area(RoundRectangle2D.Float(xx, yy, w1, h1, ICON_ARC, ICON_ARC))
    g2.color = COLOR_BODY
    g2.fill(body)
    g2.color = COLOR_LINE
    g2.stroke = BasicStroke(STROKE_W)
    g2.draw(body)

    val w2 = w1 / 2f
    val h2 = h1 / 2f
    val left = Area(Rectangle2D.Float(xx, yy, w2, h2))
    left.intersect(body)
    drawPart(g2, left, MouseButton.LEFT)

    val right = Area(Rectangle2D.Float(xx + w2, yy, w2, h2))
    right.intersect(body)
    drawPart(g2, right, MouseButton.RIGHT)

    val wa = WHEEL_ARC
    val wx = body.bounds.centerX.toFloat() - WHEEL_W / 2f
    val wy = y + WHEEL_H / 2f + wheelOffset
    val wheel = RoundRectangle2D.Float(
      wx,
      wy,
      WHEEL_W.toFloat(),
      WHEEL_H.toFloat(),
      wa,
      wa,
    )
    g2.color = getWheelColor()
    g2.fill(wheel)
    g2.color = COLOR_LINE
    g2.draw(wheel)
    g2.dispose()
  }

  override fun getIconWidth() = ICON_W

  override fun getIconHeight() = ICON_H

  fun getWheelColor(): Color = if (pressedButtons.contains(MouseButton.MIDDLE)) {
    COLOR_WHEEL_CLICK
  } else if (wheelOffset != 0) {
    COLOR_WHEEL_MOVE
  } else {
    COLOR_WHEEL_IDLE
  }

  fun drawPart(g2: Graphics2D, s: Shape, target: MouseButton) {
    if (pressedButtons.contains(target)) {
      g2.color = COLOR_CLICK
      g2.fill(s)
    }
    g2.color = COLOR_LINE
    g2.draw(s)
  }
}

private enum class MouseButton {
  LEFT,
  MIDDLE,
  RIGHT,
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
