package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(GridLayout(2, 2))
  p.border = BorderFactory.createTitledBorder("ButtonGroup")

  // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
  val path = "example/wi0063-32.png"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val image = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val icon = ImageIcon(image)
  val t1 = JToggleButton(icon)
  val t2 = JToggleButton(icon, true)
  val ip = FilteredImageSource(image.source, SelectedImageFilter())
  val selectedIcon = ImageIcon(p.createImage(ip))
  t1.selectedIcon = selectedIcon
  t2.selectedIcon = selectedIcon

  val bg = ButtonGroup()
  listOf(
    JRadioButton("RadioButton1"),
    JRadioButton("RadioButton2"),
    t1,
    t2
  ).forEach {
    bg.add(it)
    p.add(it)
  }

  val clear = JButton("clearSelection")
  clear.addActionListener { bg.clearSelection() }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(clear, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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

private class SelectedImageFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int) =
    argb and 0xFF_FF_FF_00.toInt() or (argb and 0xFF shr 1)
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
