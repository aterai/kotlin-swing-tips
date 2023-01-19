package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p0 = JPanel()
  p0.border = BorderFactory.createTitledBorder("Default JButton")
  val b0 = JButton("Default JButton")
  p0.add(b0)

  val p1 = JPanel()
  p1.border = BorderFactory.createTitledBorder("Blurred JButton1")
  val b1 = BlurredButton("Blurred JButton1")
  p1.add(b1)

  val p2 = JPanel()
  p2.border = BorderFactory.createTitledBorder("Blurred JButton(ConvolveOp.EDGE_NO_OP)")
  val b2 = BlurButton("Blurred JButton2")
  p2.add(b2)

  val box = Box.createVerticalBox()
  listOf(p0, p1, p2).forEach {
    box.add(it)
    box.add(Box.createVerticalStrut(10))
  }

  val button = JToggleButton("setEnabled(false)")
  button.addActionListener { e ->
    val f = (e.source as? AbstractButton)?.isSelected ?: false
    listOf(b0, b1, b2).forEach { it.isEnabled = !f }
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

// https://www.oreilly.com/library/view/swing-hacks/0596009070/
// 9. Blur Disabled Components
private class BlurredButton(label: String) : JButton(label) {
  private var buf: BufferedImage? = null

  override fun paintComponent(g: Graphics) {
    if (isEnabled) {
      super.paintComponent(g)
    } else {
      val w = width
      val h = height
      val img = buf?.takeIf { it.width == w && it.height == h }
        ?: BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
      val g2 = img.createGraphics()
      g2.font = g.font
      super.paintComponent(g2)
      g2.dispose()
      g.drawImage(CONVOLVE_OP.filter(img, null), 0, 0, this)
      buf = img
    }
  }

  companion object {
    private val DATA = floatArrayOf(
      .05f, .05f, .05f,
      .05f, .60f, .05f,
      .05f, .05f, .05f
    )
    private val CONVOLVE_OP = ConvolveOp(Kernel(3, 3, DATA))
  }
}

private class BlurButton(label: String) : JButton(label) {
  private var buf: BufferedImage? = null

  override fun paintComponent(g: Graphics) {
    if (isEnabled) {
      super.paintComponent(g)
    } else {
      val w = width
      val h = height
      val img = buf?.takeIf { it.width == w && it.height == h }
        ?: BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
      val g2 = img.createGraphics()
      g2.font = g.font
      super.paintComponent(g2)
      g2.dispose()
      g.drawImage(CONVOLVE_OP.filter(img, null), 0, 0, this)
      buf = img
    }
  }

  companion object {
    private val DATA = floatArrayOf(
      .05f, .05f, .05f,
      .05f, .60f, .05f,
      .05f, .05f, .05f
    )
    private val CONVOLVE_OP = ConvolveOp(Kernel(3, 3, DATA), ConvolveOp.EDGE_NO_OP, null)
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
