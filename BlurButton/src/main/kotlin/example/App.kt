package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("Apply a blur effect if disabled")

  val b0 = JButton("<html>Default <font color='red'>JButton")
  p.add(b0)

  val b1 = BlurredButton("Blurred JButton")
  p.add(b1)

  val b2 = BlurButton("Blurred JButton(ConvolveOp.EDGE_NO_OP)")
  p.add(b2)

  val b3 = JButton("<html>Blurred <font color='blue'>JLayer")
  p.add(JLayer(b3, BlurLayerUI()))

  val check = JCheckBox("setEnabled", true)
  check.addActionListener { e ->
    val f = (e.source as? JCheckBox)?.isSelected ?: false
    listOf(b0, b1, b2, b3).forEach { it.isEnabled = f }
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(check)

  return JPanel(BorderLayout(10, 10)).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
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

private class BlurLayerUI<V : AbstractButton?> : LayerUI<V>() {
  private var buf: BufferedImage? = null

  override fun paint(g: Graphics, c: JComponent) {
    if (c is JLayer<*>) {
      val view = c.view
      if (view.isEnabled) {
        view.paint(g)
      } else {
        val d = view.size
        val img = buf?.takeIf { it.width == d.width && it.height == d.height }
          ?: BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        view.paint(g2)
        g2.dispose()
        g.drawImage(CONVOLVE_OP.filter(img, null), 0, 0, c)
        buf = img
      }
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

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
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
