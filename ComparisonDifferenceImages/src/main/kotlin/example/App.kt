package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.awt.image.MemoryImageSource
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val iia = ImageIcon(cl.getResource("example/a.png"))
  val iib = ImageIcon(cl.getResource("example/b.png"))
  val label = JLabel(iia)
  val w = iia.iconWidth
  val h = iia.iconHeight
  val pixelsA = getData(iia, w, h)
  val pixelsB = getData(iib, w, h)
  for (i in pixelsA.indices) {
    if (pixelsA[i] == pixelsB[i]) {
      pixelsA[i] = pixelsA[i] and 0x44_FF_FF_FF
    }
  }
  val ra = JRadioButton("a.png", true)
  ra.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      label.icon = iia
    }
  }
  val rb = JRadioButton("b.png")
  rb.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      label.icon = iib
    }
  }
  val p = JPanel()
  val source = MemoryImageSource(w, h, pixelsA, 0, w)
  val rr = JRadioButton("diff")
  rr.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      label.icon = ImageIcon(p.createImage(source))
    }
  }

  val bg = ButtonGroup()
  listOf(ra, rb, rr).forEach {
    bg.add(it)
    p.add(it)
  }

  return JPanel(BorderLayout()).also {
    it.add(label)
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getData(imageIcon: ImageIcon, w: Int, h: Int): IntArray {
  val img = imageIcon.image
  val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
  val g = image.createGraphics()
  g.drawImage(img, 0, 0, null)
  g.dispose()
  return (image.raster.dataBuffer as DataBufferInt).data
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
