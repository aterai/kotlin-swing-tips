package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

private val viewRect = Rectangle()
private val iconRect = Rectangle()
private val textRect = Rectangle()

fun makeUI(): Component {
  val field = JTextField("#FFFFFF")
  field.isEditable = false
  field.font = Font(Font.MONOSPACED, Font.PLAIN, 11)
  field.columns = 8
  val sample = JLabel(ColorIcon(Color.WHITE))
  val box = JPanel()
  box.add(sample)
  box.add(field)
  val url = Thread.currentThread().contextClassLoader.getResource("example/duke.gif")
  val image = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val label = JLabel(ImageIcon(image))
  label.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      updateViewRect(label)
      val pt = e.point
      if (iconRect.contains(pt)) {
        val argb = image.getRGB(pt.x - iconRect.x, pt.y - iconRect.y)
        field.text = "#%06X".format(argb and 0x00_FF_FF_FF)
        sample.icon = ColorIcon(Color(argb, true))
      }
    }
  })

  return JPanel(BorderLayout()).also {
    it.add(label)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateViewRect(c: JLabel) {
  iconRect.setBounds(0, 0, 0, 0)
  textRect.setBounds(0, 0, 0, 0)
  SwingUtilities.calculateInnerArea(c, viewRect)
  SwingUtilities.layoutCompoundLabel(
    c,
    c.getFontMetrics(c.font),
    c.text,
    c.icon,
    c.verticalAlignment,
    c.horizontalAlignment,
    c.verticalTextPosition,
    c.horizontalTextPosition,
    viewRect,
    iconRect,
    textRect,
    c.iconTextGap,
  )
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class ColorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.paint = Color.BLACK
    g2.drawRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 32
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
