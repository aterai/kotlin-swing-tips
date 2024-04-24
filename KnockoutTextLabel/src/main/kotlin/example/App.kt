package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val path = "example/test.jpg"
  val cl = Thread.currentThread().contextClassLoader
  val image = cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val label = object : JLabel("ABC") {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.drawImage(image, 0, 0, width, height, this)
      val frc = g2.fontRenderContext
      val text = text
      val gv = font.createGlyphVector(frc, text)
      val b = gv.visualBounds
      val w = width.toDouble()
      val h = height.toDouble()
      val cx = w / 2.0 - b.centerX
      val cy = h / 2.0 - b.centerY
      val toCenterAt = AffineTransform.getTranslateInstance(cx, cy)
      val s = toCenterAt.createTransformedShape(gv.outline)
      val bg = Area(Rectangle2D.Double(0.0, 0.0, w, h))
      bg.subtract(Area(s))
      g2.color = background
      g2.fill(bg)
      g2.dispose()
    }
  }
  val code = 0xE6_00_00_32.toInt()
  label.background = Color(code, true)
  label.font = Font(Font.SERIF, Font.BOLD, 140)
  val check = JCheckBox("hasAlpha", true)
  check.addActionListener { e ->
    label.background = Color(code, (e.source as? JCheckBox)?.isSelected == true)
  }
  return JPanel(BorderLayout()).also {
    it.add(label)
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMissingImage(): Image {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
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
