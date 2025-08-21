package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/16x16.png")
  val image = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val check = JCheckBox("setIconImage")
  check.addActionListener { e ->
    val c = e.source
    if (c is JCheckBox) {
      val img = if (c.isSelected) image else null
      (c.topLevelAncestor as? Window)?.setIconImage(img)
    }
  }

  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("Window#setIconImage(Image)")
  p.add(check)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
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
