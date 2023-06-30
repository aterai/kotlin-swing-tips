package example

import java.awt.*
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import javax.swing.*

fun makeUI(): Component {
  val font = Font(Font.MONOSPACED, Font.BOLD, 200)
  val at = AffineTransform.getScaleInstance(-1.0, 1.0)
  val c = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = Color.BLACK
      val frc = g2.fontRenderContext
      val copyright = TextLayout("c", font, frc).getOutline(null)
      val copyleft = at.createTransformedShape(copyright)
      val b = copyleft.bounds2D
      val cx = width / 2.0 - b.centerX
      val cy = height / 2.0 - b.centerY
      val toCenterAtf = AffineTransform.getTranslateInstance(cx, cy)
      g2.fill(toCenterAtf.createTransformedShape(copyleft))
      g2.dispose()
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(c)
    it.preferredSize = Dimension(320, 240)
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
