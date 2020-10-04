package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel().also {
  val cl = Thread.currentThread().contextClassLoader
  val icon = ImageIcon(cl.getResource("example/duke.gif"))
  // val icon = UIManager.getIcon("OptionPane.warningIcon")
  it.add(makeLabel("Default", icon))
  it.add(makeLabel("Rotate: 180", RotateIcon(icon, 180)))
  it.add(makeLabel("Rotate:  90", RotateIcon(icon, 90)))
  it.add(makeLabel("Rotate: -90", RotateIcon(icon, -90)))
  it.border = BorderFactory.createEmptyBorder(0, 32, 0, 32)
  it.preferredSize = Dimension(320, 240)
}

private fun makeLabel(title: String, icon: Icon): JLabel {
  val l = JLabel(title, icon, SwingConstants.CENTER)
  l.verticalTextPosition = SwingConstants.BOTTOM
  l.horizontalTextPosition = SwingConstants.CENTER
  l.border = BorderFactory.createLineBorder(Color.GRAY)
  return l
}

private class RotateIcon(icon: Icon, rotate: Int) : Icon {
  private val dim = Dimension()
  private val image: Image
  private var trans: AffineTransform? = null
  init {
    require(rotate % 90 == 0) { "$rotate: Rotate must be (rotate % 90 == 0)" }
    dim.setSize(icon.iconWidth, icon.iconHeight)
    image = BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB)
    val g = image.getGraphics()
    icon.paintIcon(null, g, 0, 0)
    g.dispose()
    val numQuadrants = rotate / 90 % 4
    when (numQuadrants) {
      3, -1 -> {
        trans = AffineTransform.getTranslateInstance(0.0, dim.width.toDouble())
        dim.setSize(icon.iconHeight, icon.iconWidth)
      }
      1, -3 -> {
        trans = AffineTransform.getTranslateInstance(dim.height.toDouble(), 0.0)
        dim.setSize(icon.iconHeight, icon.iconWidth)
      }
      2 -> trans = AffineTransform.getTranslateInstance(dim.width.toDouble(), dim.height.toDouble())
      else -> trans = AffineTransform.getTranslateInstance(0.0, 0.0)
    }
    trans?.quadrantRotate(numQuadrants)
  }

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.drawImage(image, trans, c)
    g2.dispose()
  }

  override fun getIconWidth() = dim.width

  override fun getIconHeight() = dim.height
}

// private enum class QuadrantRotate(val numQuadrants: Int) {
//   CLOCKWISE(1), VERTICAL_FLIP(2), COUNTER_CLOCKWISE(-1)
// }
//
// private class QuadrantRotateIcon(private val icon: Icon, private val rotate: QuadrantRotate) : Icon {
//   override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
//     val w = icon.iconWidth
//     val h = icon.iconHeight
//     val g2 = g.create() as Graphics2D
//     g2.translate(x, y)
//     when (rotate) {
//       QuadrantRotate.CLOCKWISE -> g2.translate(h, 0)
//       QuadrantRotate.VERTICAL_FLIP -> g2.translate(w, h)
//       QuadrantRotate.COUNTER_CLOCKWISE -> g2.translate(0, w)
//       //  else -> throw AssertionError("Unknown QuadrantRotateIcon")
//     }
//     g2.rotate(Math.toRadians(90 * rotate.numQuadrants.toDouble()))
//     icon.paintIcon(c, g2, 0, 0)
//     g2.dispose()
//   }
//
//   override fun getIconWidth() = if (rotate == QuadrantRotate.VERTICAL_FLIP) icon.iconWidth else icon.iconHeight
//
//   override fun getIconHeight() = if (rotate == QuadrantRotate.VERTICAL_FLIP) icon.iconHeight else icon.iconWidth
// }

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
