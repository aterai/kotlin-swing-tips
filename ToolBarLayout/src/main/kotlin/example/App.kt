package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val icon1 = makeIcon("toolbarButtonGraphics/general/Copy24.gif")
  val icon2 = makeIcon("toolbarButtonGraphics/general/Cut24.gif")
  val icon3 = makeIcon("toolbarButtonGraphics/general/Help24.gif")

  val toolBar1 = JToolBar("ToolBarButton")
  toolBar1.add(JButton(icon1))
  toolBar1.add(JButton(icon2))
  toolBar1.add(Box.createGlue())
  toolBar1.add(JButton(icon3))

  val toolBar2 = JToolBar("JButton")
  toolBar2.add(createToolBarButton(icon1))
  toolBar2.add(createToolBarButton(icon2))
  toolBar2.add(Box.createGlue())
  toolBar2.add(createToolBarButton(icon3))

  val p = JPanel(BorderLayout())
  p.add(toolBar1, BorderLayout.NORTH)
  p.add(JScrollPane(JTextArea()))
  p.add(toolBar2, BorderLayout.SOUTH)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun createToolBarButton(icon: Icon): JButton {
  val b = JButton(icon)
  b.isRequestFocusEnabled = false
  return b
}

private fun makeIcon(path: String): Icon {
  val cl = Thread.currentThread().contextClassLoader
  return cl.getResource(path)?.openStream()?.use { ImageIcon(ImageIO.read(it)) }
    ?: makeMissingIcon()
}

private fun makeMissingIcon(): Icon {
  val missingIcon = UIManager.getIcon("html.missingImage")
  val iw = missingIcon.iconWidth
  val ih = missingIcon.iconHeight
  val bi = BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, (24 - iw) / 2, (24 - ih) / 2)
  g2.dispose()
  return ImageIcon(bi)
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
