package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.ImageFilter
import java.awt.image.RGBImageFilter
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val bg1 = Color(0xFE_FF_32_00.toInt(), true)
  val bg2 = Color(0x32_FF_32_00, true).darker()

  val button1 = JButton("setBackground").also {
    it.background = bg1
  }

  val button2 = object : JButton("override paintComponent") {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      val r = Rectangle(0, 0, width, height)
      val m = getModel()
      g2.paint = if (m.isArmed || m.isPressed) bg1 else bg2
      g2.fill(r)
      g2.paint = Color.GRAY.brighter()
      r.width -= 1
      r.height -= 1
      g2.draw(r)
      g2.dispose()
      super.paintComponent(g)
    }
  }.also {
    it.isContentAreaFilled = false
  }

  val button3 = JButton("setIcon + setPressedIcon").also {
    it.background = bg1
    it.isBorderPainted = false
    it.icon = ButtonBackgroundIcon(bg2)
    it.pressedIcon = ButtonBackgroundIcon(bg1)
    // it.horizontalTextPosition = SwingConstants.CENTER
  }

  val button4 = JButton("JLayer + RGBImageFilter")

  val box = makePanel(
    JButton("Default"),
    button1,
    button2,
    button3,
    JLayer(button4, ImageFilterLayerUI<Component>(ColorFilter()))
  )

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePanel(vararg list: Component): Component {
  val c = GridBagConstraints()
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(20, 15, 0, 15)
  c.weightx = 1.0
  c.gridx = GridBagConstraints.REMAINDER
  val p = JPanel(GridBagLayout())
  list.forEach { p.add(it, c) }
  return p
}

private class ButtonBackgroundIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = color
    g2.fillRect(0, 0, c.width, c.height)
    g2.dispose()
  }

  override fun getIconWidth() = 0

  override fun getIconHeight() = 0
}

private class ImageFilterLayerUI<V : Component>(private val filter: ImageFilter) : LayerUI<V>() {
  private var buf: BufferedImage? = null

  override fun paint(g: Graphics, c: JComponent) {
    if (c is JLayer<*>) {
      val d = c.view.size
      val b = buf?.takeIf { it.width == d.width && it.height == d.height } ?: BufferedImage(
        d.width,
        d.height,
        BufferedImage.TYPE_INT_ARGB
      )
      buf = b
      val g2 = b.createGraphics()
      super.paint(g2, c)
      g2.dispose()
      val image = c.createImage(FilteredImageSource(b.source, filter))
      g.drawImage(image, 0, 0, null)
    } else {
      super.paint(g, c)
    }
  }
}

private class ColorFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int): Int {
    val r = 0xFF // argb shr 16 and 0xFF
    val g = argb shr 8 and 0xFF
    val b = argb and 0xFF
    return argb and 0xFF_00_00_00.toInt() or (r shl 16) or (g shl 8) or b
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
