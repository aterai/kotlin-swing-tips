package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val list1 = listOf(
    "wi0009-16.png", "wi0054-16.png", "wi0062-16.png",
    "wi0063-16.png", "wi0064-16.png", "wi0096-16.png",
    "wi0111-16.png", "wi0122-16.png", "wi0124-16.png",
    "wi0126-16.png"
  ).map { makeIcon(it) }
  val slider1 = JSlider(SwingConstants.VERTICAL, 0, list1.size - 1, 0)
  slider1.snapToTicks = true
  slider1.majorTickSpacing = 1
  slider1.paintTicks = true
  slider1.paintLabels = true
  val labelTable1 = slider1.labelTable
  if (labelTable1 is Map<*, *>) {
    labelTable1.forEach { (key, value) ->
      if (key is Int && value is JLabel) {
        value.icon = list1[key]
        value.text = null
        value.border = BorderFactory.createEmptyBorder(0, 1, 0, 1)
      }
    }
  }
  slider1.labelTable = labelTable1



//  val labelTable2 = Hashtable<Int, Component>(11)
//  listOf("óÎ", "àÎ", "ìÛ", "éQ", "„Ê", "åﬁ", "ó§", "éΩ", "éJ", "ãË", "èE")
//    .map { JLabel(it) }
//    .forEachIndexed { i, label ->
//      label.foreground = Color(250, 100 - i * 10, 10)
//      labelTable2[i] = label
//    }
//
//  val slider2 = JSlider(0, 10, 0)
//  slider2.snapToTicks = true
//  slider2.labelTable = labelTable2
//  slider2.paintTicks = true
//  slider2.paintLabels = true
  val list2 = arrayOf("óÎ", "àÎ", "ìÛ", "éQ", "„Ê", "åﬁ", "ó§", "éΩ", "éJ", "ãË", "èE")
  val slider2 = JSlider(0, list2.size - 1, 0)
  // slider2.setForeground(Color.BLUE);
  // slider2.setForeground(Color.BLUE);
  slider2.majorTickSpacing = 1
  slider2.snapToTicks = true
  // slider2.setLabelTable(labelTable2);
  // slider2.setLabelTable(labelTable2);
  slider2.paintTicks = true
  slider2.paintLabels = true
  val labelTable2 = slider2.labelTable
  if (labelTable2 is Map<*, *>) {
    labelTable2.forEach { (key, value) ->
      if (key is Int && value is JLabel) {
        value.text = list2[key]
        value.foreground = Color(250, 100 - key * 10, 10)
      }
    }
  }
  slider2.labelTable = labelTable2

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(20, 20, 20, 0)
  box.add(JSlider(0, 100, 100))
  box.add(Box.createVerticalStrut(20))
  box.add(JSlider())
  box.add(Box.createVerticalStrut(20))
  box.add(slider2)
  box.add(Box.createHorizontalGlue())

  return JPanel(BorderLayout()).also {
    it.add(slider1, BorderLayout.WEST)
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(5, 20, 5, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeIcon(path: String): Icon {
  val url = Thread.currentThread().contextClassLoader.getResource("example/$path")
  val image = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  return ImageIcon(image)
}

private fun makeMissingImage(): BufferedImage {
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
