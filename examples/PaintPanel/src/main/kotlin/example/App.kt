package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(PaintPanel())
  it.preferredSize = Dimension(320, 240)
}

private class PaintPanel : JPanel() {
  private var handler: MouseInputListener? = null
  private val list = ArrayList<Shape>()

  override fun updateUI() {
    removeMouseMotionListener(handler)
    removeMouseListener(handler)
    super.updateUI()
    handler = object : MouseInputAdapter() {
      private var path: Path2D? = null

      override fun mousePressed(e: MouseEvent) {
        path = Path2D.Double().also {
          it.moveTo(e.x.toDouble(), e.y.toDouble())
          list.add(it)
        }
        e.component.repaint()
      }

      override fun mouseDragged(e: MouseEvent) {
        path?.lineTo(e.x.toDouble(), e.y.toDouble())
        e.component.repaint()
      }
    }
    addMouseMotionListener(handler)
    addMouseListener(handler)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = Color.BLACK
    g2.stroke = STROKE
    list.forEach { g2.draw(it) }
    g2.dispose()
  }

  companion object {
    private val STROKE = BasicStroke(
      3f,
      BasicStroke.CAP_ROUND,
      BasicStroke.JOIN_ROUND,
    )
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
