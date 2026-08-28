package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import javax.swing.*
import kotlin.math.cos
import kotlin.math.sin

private const val SIDE_LENGTH = 100.0
private val cube = listOf(
  Vertex(SIDE_LENGTH, SIDE_LENGTH, SIDE_LENGTH),
  Vertex(SIDE_LENGTH, SIDE_LENGTH, -SIDE_LENGTH),
  Vertex(-SIDE_LENGTH, SIDE_LENGTH, -SIDE_LENGTH),
  Vertex(-SIDE_LENGTH, SIDE_LENGTH, SIDE_LENGTH),
  Vertex(SIDE_LENGTH, -SIDE_LENGTH, SIDE_LENGTH),
  Vertex(SIDE_LENGTH, -SIDE_LENGTH, -SIDE_LENGTH),
  Vertex(-SIDE_LENGTH, -SIDE_LENGTH, -SIDE_LENGTH),
  Vertex(-SIDE_LENGTH, -SIDE_LENGTH, SIDE_LENGTH),
)

fun createUI(): Component {
  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      val path = Path2D.Double()
      path.moveTo(cube[0].screenX, cube[0].screenY)
      path.lineTo(cube[1].screenX, cube[1].screenY)
      path.lineTo(cube[2].screenX, cube[2].screenY)
      path.lineTo(cube[3].screenX, cube[3].screenY)
      path.lineTo(cube[0].screenX, cube[0].screenY)
      path.lineTo(cube[4].screenX, cube[4].screenY)
      path.lineTo(cube[5].screenX, cube[5].screenY)
      path.lineTo(cube[6].screenX, cube[6].screenY)
      path.lineTo(cube[7].screenX, cube[7].screenY)
      path.lineTo(cube[4].screenX, cube[4].screenY)
      path.moveTo(cube[1].screenX, cube[1].screenY)
      path.lineTo(cube[5].screenX, cube[5].screenY)
      path.moveTo(cube[2].screenX, cube[2].screenY)
      path.lineTo(cube[6].screenX, cube[6].screenY)
      path.moveTo(cube[3].screenX, cube[3].screenY)
      path.lineTo(cube[7].screenX, cube[7].screenY)
      val r = SwingUtilities.calculateInnerArea(this, null)
      g2.paint = Color.WHITE
      g2.fill(r)
      g2.translate(r.centerX, r.centerY)
      g2.paint = Color.BLACK
      g2.draw(path)
      g2.dispose()
    }
  }
  val handler = DragRotateHandler()
  p.addMouseListener(handler)
  p.addMouseMotionListener(handler)
  p.preferredSize = Dimension(320, 240)
  return p
}

private class DragRotateHandler : MouseAdapter() {
  private val defCursor = Cursor.getDefaultCursor()
  private val hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val pp = Point()

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.point
    val rotY = (pt.x - pp.x) * .03
    val rotX = (pt.y - pp.y) * .03
    val rotZ = 0.0
    for (v in cube) {
      v.rotate(rotX, rotY, rotZ)
    }
    pp.location = pt
    e.component.repaint()
  }

  override fun mousePressed(e: MouseEvent) {
    e.component.cursor = hndCursor
    pp.location = e.point
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = defCursor
  }
}

private class Vertex(
  private var worldX: Double,
  private var worldY: Double,
  private var worldZ: Double,
) {
  var screenX = 0.0
  var screenY = 0.0

  init {
    applyProjection()
  }

  private fun applyProjection() {
    val screenDistance = 500.0
    val depth = 1000.0
    val distanceZ = worldZ + depth
    screenX = screenDistance * worldX / distanceZ
    screenY = screenDistance * worldY / distanceZ
  }

  fun rotate(
    angleX: Double,
    angleY: Double,
    angleZ: Double,
  ) {
    // yaw: rotation around the y-axis
    val yawX = worldX * cos(angleY) - worldZ * sin(angleY)
    val yawZ = worldX * sin(angleY) + worldZ * cos(angleY)
    // pitch: rotation around the x-axis
    val pitchY = worldY * cos(angleX) - yawZ * sin(angleX)
    val pitchZ = worldY * sin(angleX) + yawZ * cos(angleX)
    // roll: rotation around the z-axis
    worldX = yawX * cos(angleZ) - pitchY * sin(angleZ)
    worldY = yawX * sin(angleZ) + pitchY * cos(angleZ)
    worldZ = pitchZ
    applyProjection()
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
