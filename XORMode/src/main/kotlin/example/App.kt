package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val icon = ImageIcon(cl.getResource("example/test.png"))
  return JPanel(BorderLayout()).also {
    it.add(ImageIconPanel(icon))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ImageIconPanel(private val icon: ImageIcon) : JPanel() {
  private var rbl: RubberBandingListener? = null
  private val stroke = BasicStroke(2f)
  private val rubberBand: Path2D = Path2D.Double()

  override fun updateUI() {
    removeMouseListener(rbl)
    removeMouseMotionListener(rbl)
    super.updateUI()
    rbl = RubberBandingListener()
    addMouseMotionListener(rbl)
    addMouseListener(rbl)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    val iw = icon.iconWidth
    val ih = icon.iconHeight
    val dim = size
    val x = (dim.width - iw) / 2
    val y = (dim.height - ih) / 2
    g.drawImage(icon.image, x, y, iw, ih, this)

    g2.paint = Color.RED
    g2.fillOval(10, 10, 32, 32)

    g2.paint = Color.GREEN
    g2.fillOval(50, 10, 32, 32)

    g2.paint = Color.BLUE
    g2.fillOval(90, 10, 32, 32)

    g2.paint = Color.PINK
    g2.fillOval(130, 10, 32, 32)

    g2.paint = Color.CYAN
    g2.fillOval(170, 10, 32, 32)

    g2.paint = Color.ORANGE
    g2.fillOval(210, 10, 32, 32)

    g2.setXORMode(Color.PINK)
    g2.fill(rubberBand)

    g2.setPaintMode()
    g2.stroke = stroke
    g2.paint = Color.WHITE
    g2.draw(rubberBand)
    g2.dispose()
  }

  private inner class RubberBandingListener : MouseAdapter() {
    private val srcPoint = Point()
    override fun mouseDragged(e: MouseEvent) {
      val destPoint = e.point
      val rb = rubberBand
      rb.reset()
      rb.moveTo(srcPoint.x.toDouble(), srcPoint.y.toDouble())
      rb.lineTo(destPoint.x.toDouble(), srcPoint.y.toDouble())
      rb.lineTo(destPoint.x.toDouble(), destPoint.y.toDouble())
      rb.lineTo(srcPoint.x.toDouble(), destPoint.y.toDouble())
      rb.closePath()
      e.component.repaint()
    }

    override fun mouseReleased(e: MouseEvent) {
      e.component.repaint()
    }

    override fun mousePressed(e: MouseEvent) {
      rubberBand.reset()
      srcPoint.location = e.point
      e.component.repaint()
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
