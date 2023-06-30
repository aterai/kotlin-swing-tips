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
  Vertex(-SIDE_LENGTH, -SIDE_LENGTH, SIDE_LENGTH)
)

fun makeUI(): Component {
  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val path = Path2D.Double()
      path.moveTo(cube[0].vx, cube[0].vy)
      path.lineTo(cube[1].vx, cube[1].vy)
      path.lineTo(cube[2].vx, cube[2].vy)
      path.lineTo(cube[3].vx, cube[3].vy)
      path.lineTo(cube[0].vx, cube[0].vy)
      path.lineTo(cube[4].vx, cube[4].vy)
      path.lineTo(cube[5].vx, cube[5].vy)
      path.lineTo(cube[6].vx, cube[6].vy)
      path.lineTo(cube[7].vx, cube[7].vy)
      path.lineTo(cube[4].vx, cube[4].vy)
      path.moveTo(cube[1].vx, cube[1].vy)
      path.lineTo(cube[5].vx, cube[5].vy)
      path.moveTo(cube[2].vx, cube[2].vy)
      path.lineTo(cube[6].vx, cube[6].vy)
      path.moveTo(cube[3].vx, cube[3].vy)
      path.lineTo(cube[7].vx, cube[7].vy)
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
      v.rotateTransformation(rotX, rotY, rotZ)
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

private class Vertex(private var dx: Double, private var dy: Double, private var dz: Double) {
  var vx = 0.0
  var vy = 0.0

  init {
    projectionTransformation()
  }

  private fun projectionTransformation() {
    val screenDistance = 500.0
    val depth = 1000.0
    val gz = dz + depth
    vx = screenDistance * dx / gz
    vy = screenDistance * dy / gz
  }

  fun rotateTransformation(kx: Double, ky: Double, kz: Double) {
    val x0 = dx * cos(ky) - dz * sin(ky)
    val y0 = dy
    val z0 = dx * sin(ky) + dz * cos(ky)
    val y1 = y0 * cos(kx) - z0 * sin(kx)
    val z1 = y0 * sin(kx) + z0 * cos(kx)
    dx = x0 * cos(kz) - y1 * sin(kz)
    dy = x0 * sin(kz) + y1 * cos(kz)
    dz = z1
    projectionTransformation()
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
