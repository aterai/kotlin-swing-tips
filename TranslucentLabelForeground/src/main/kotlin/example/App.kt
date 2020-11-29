package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val label1 = JLabel("ABC", SwingConstants.CENTER)
  label1.foreground = Color(0x64_FF_AA_AA, true)
  label1.background = Color(0x64_64_C8)
  label1.font = Font(Font.MONOSPACED, Font.BOLD, 140)

  val style = "font-family:monospace;font-weight:bold;color:rgba(255,170,170,0.4);font-size:140pt"
  val label2 = JLabel("<html><span style='$style'>ABC")
  label2.background = Color(0x64_64_C8)
  label2.horizontalAlignment = SwingConstants.CENTER

  val p = object : JPanel(GridLayout(2, 1)) {
    private var texture: Paint? = null
    override fun updateUI() {
      super.updateUI()
      val tc = Color(0xEE_32_32_32.toInt(), true)
      texture = TextureUtils.createCheckerTexture(16, tc)
      isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = texture
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      super.paintComponent(g)
    }
  }
  p.add(label1)
  p.add(label2)

  val check1 = JCheckBox("setOpaque")
  check1.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    listOf(label1, label2).forEach {
      it.isOpaque = b
    }
    p.repaint()
  }

  val check2 = JCheckBox("Background has Alpha")
  check2.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    val bgc = Color(0x64_64_64_C8, b)
    listOf(label1, label2).forEach {
      it.background = bgc
    }
    p.repaint()
  }

  val box = Box.createHorizontalBox()
  box.add(check1)
  box.add(check2)

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private object TextureUtils {
  fun createCheckerTexture(cs: Int, color: Color): TexturePaint {
    val size = cs * cs
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    g2.paint = color
    g2.fillRect(0, 0, size, size)
    var i = 0
    while (i * cs < size) {
      var j = 0
      while (j * cs < size) {
        if ((i + j) % 2 == 0) {
          g2.fillRect(i * cs, j * cs, cs, cs)
        }
        j++
      }
      i++
    }
    g2.dispose()
    return TexturePaint(img, Rectangle(size, size))
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
