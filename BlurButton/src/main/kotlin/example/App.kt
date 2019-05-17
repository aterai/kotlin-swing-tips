// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val p0 = JPanel()
    p0.setBorder(BorderFactory.createTitledBorder("Default JButton"))
    val b0 = JButton("Default JButton")
    p0.add(b0)

    val p1 = JPanel()
    p1.setBorder(BorderFactory.createTitledBorder("Blurred JButton1"))
    val b1 = BlurJButton("Blurred JButton1")
    p1.add(b1)

    val p2 = JPanel()
    p2.setBorder(BorderFactory.createTitledBorder("Blurred JButton(ConvolveOp.EDGE_NO_OP)"))
    val b2 = BlurButton("Blurred JButton2")
    p2.add(b2)

    val box = Box.createVerticalBox()
    listOf(p0, p1, p2).forEach {
      box.add(it)
      box.add(Box.createVerticalStrut(10))
    }

    val button = JToggleButton("setEnabled(false)")
    button.addActionListener { e ->
      val f = (e.getSource() as? AbstractButton)?.isSelected() ?: false
      listOf(b0, b1, b2).forEach { it.setEnabled(!f) }
    }

    add(box, BorderLayout.NORTH)
    add(button, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }
}

// http://shop.oreilly.com/product/9780596009076.do
// 9. Blur Disabled Components
// http://code.google.com/p/filthy-rich-clients/source/browse/trunk/swing-hacks-examples-20060109/Ch01-JComponents/09/swinghacks/ch01/JComponents/hack09/BlurJButton.java?r=11
internal class BlurJButton (label: String) : JButton(label) {
  @Transient
  private var buf: BufferedImage? = null

  protected override fun paintComponent(g: Graphics) {
    if (isEnabled()) {
      super.paintComponent(g)
    } else {
      val w = getWidth()
      val h = getHeight()
      val img = buf?.takeIf { it.getWidth() == w && it.getHeight() == h }
        ?: BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
      val g2 = img.createGraphics()
      g2.setFont(g.getFont())
      super.paintComponent(g2)
      g2.dispose()
      g.drawImage(CONVOLVE_OP.filter(img, null), 0, 0, this)
      buf = img
    }
  }

  companion object {
    private val CONVOLVE_OP = ConvolveOp(Kernel(3, 3, floatArrayOf(
      .05f, .05f, .05f,
      .05f, .60f, .05f,
      .05f, .05f, .05f)))
  }
}

internal class BlurButton(label: String) : JButton(label) {
  @Transient
  private var buf: BufferedImage? = null

  protected override fun paintComponent(g: Graphics) {
    if (isEnabled()) {
      super.paintComponent(g)
    } else {
      val w = getWidth()
      val h = getHeight()
      val img = buf?.takeIf { it.getWidth() == w && it.getHeight() == h }
        ?: BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
      val g2 = img.createGraphics()
      g2.setFont(g.getFont())
      super.paintComponent(g2)
      g2.dispose()
      g.drawImage(CONVOLVE_OP.filter(img, null), 0, 0, this)
      buf = img
    }
  }

  companion object {
    private val CONVOLVE_OP = ConvolveOp(Kernel(3, 3, floatArrayOf(
      .05f, .05f, .05f,
      .05f, .60f, .05f,
      .05f, .05f, .05f)), ConvolveOp.EDGE_NO_OP, null)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
