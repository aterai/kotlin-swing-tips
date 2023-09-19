package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/duke.running.gif")
  val icon = url?.let { ImageIcon(it) } ?: UIManager.getIcon("html.missingImage")
  val label1 = JLabel(icon)
  label1.isEnabled = false
  label1.border = BorderFactory.createTitledBorder("Default")

  val label2 = object : JLabel(icon) {
    override fun imageUpdate(
      img: Image,
      infoflags: Int,
      x: Int,
      y: Int,
      w: Int,
      h: Int,
    ): Boolean {
      var info = infoflags
      if (!isEnabled) {
        info = info and FRAMEBITS.inv()
      }
      return super.imageUpdate(img, info, x, y, w, h)
    }
  }
  label2.isEnabled = false
  label2.border = BorderFactory.createTitledBorder("Override imageUpdate(...)")

  val label3 = JLabel(icon)
  label3.isEnabled = false
  label3.border = BorderFactory.createTitledBorder("setDisabledIcon")
  val url2 = cl.getResource("example/duke.running_frame_0001.gif")
  val image = url2?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  label3.disabledIcon = makeDisabledIcon(image)

  val check = JCheckBox("setEnabled")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    label1.isEnabled = b
    label2.isEnabled = b
    label3.isEnabled = b
  }

  val p = JPanel(GridLayout(2, 2))
  p.add(label1)
  p.add(label2)
  p.add(label3)

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMissingImage(): Image {
  val missingIcon = UIManager.getIcon("html.missingImage")
  val iw = missingIcon.iconWidth
  val ih = missingIcon.iconHeight
  val bi = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, (16 - iw) / 2, (16 - ih) / 2)
  g2.dispose()
  return bi
}

private fun makeDisabledIcon(img: Image) = ImageIcon(GrayFilter.createDisabledImage(img))

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
