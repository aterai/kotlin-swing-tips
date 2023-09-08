package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import javax.swing.*

fun makeUI() = MainPanel().also {
  it.add(JButton("JButton"))
  it.preferredSize = Dimension(320, 240)
}

class MainPanel : JPanel() {
  private val robot = runCatching { Robot() }.getOrNull()
  private val screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
  private val buf = Rectangle()
  private var backgroundImage: BufferedImage? = null
  private val data = floatArrayOf(
    .1f, .1f, .1f,
    .1f, .2f, .1f,
    .1f, .1f, .1f,
  )
  private val kernel = Kernel(3, 3, data)
  private val op = ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)
  private val bgc = Color(255, 255, 255, 100)

  init {
    updateBackground()
    EventQueue.invokeLater {
      val f = SwingUtilities.getWindowAncestor(this)
      f.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
          repaint()
        }

        override fun componentMoved(e: ComponentEvent) {
          repaint()
        }
      })
      f.addWindowListener(object : WindowAdapter() {
        override fun windowDeiconified(e: WindowEvent) {
          updateBackground()
        }
      })
    }
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    val pt = locationOnScreen
    buf.bounds = screenRect
    SwingUtilities.computeIntersection(pt.x, pt.y, width, height, buf)
    val img = backgroundImage?.getSubimage(buf.x, buf.y, buf.width, buf.height)
    g2.drawImage(img, -pt.x.coerceAtMost(0), -pt.y.coerceAtMost(0), this)
    g2.paint = bgc
    g2.fillRect(0, 0, width, height)
    g2.dispose()
  }

  fun updateBackground() {
    backgroundImage = op.filter(robot?.createScreenCapture(screenRect), null)
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
