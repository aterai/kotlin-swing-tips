package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val button1 = JButton("default icon")
  button1.addActionListener {
    val fileChooser = JFileChooser()
    fileChooser.showOpenDialog(button1.rootPane)
  }

  val button2 = JButton("makeImage(16, Color.WHITE)")
  button2.addActionListener {
    val fileChooser = object : JFileChooser() {
      override fun createDialog(parent: Component) =
        super.createDialog(parent).also {
          it.setIconImage(makeImage(16, Color.WHITE))
        }
    }
    fileChooser.showOpenDialog(button2.rootPane)
  }

  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("JFileChooser#showOpenDialog(...)")
  p.add(button1)
  p.add(button2)
  val icons = DefaultListModel<Icon>()
  IMAGE_LIST.map { ImageIcon(it) }.forEach { icons.addElement(it) }
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(JList(icons)))
    it.preferredSize = Dimension(320, 240)
  }
}

private val IMAGE_LIST = listOf(
  makeImage(16, Color.RED),
  makeImage(18, Color.GREEN),
  makeImage(20, Color.YELLOW),
  makeImage(24, Color.PINK),
  makeImage(32, Color.ORANGE),
  makeImage(40, Color.CYAN),
  makeImage(64, Color.MAGENTA)
)

fun makeImage(size: Int, color: Color): Image {
  val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
  val g2 = image.createGraphics()
  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
  g2.paint = color
  g2.fill(Ellipse2D.Double(0.0, 0.0, size - 1.0, size - 1.0))
  val frc = g2.fontRenderContext
  val font = g2.font.deriveFont(AffineTransform.getScaleInstance(.8, 1.0))
  val s = TextLayout(size.toString(), font, frc).getOutline(null)
  g2.paint = Color.BLACK
  val r = s.bounds
  val cx = size / 2.0 - r.centerX
  val cy = size / 2.0 - r.centerY
  val toCenterAtf = AffineTransform.getTranslateInstance(cx, cy)
  g2.fill(toCenterAtf.createTransformedShape(s))
  g2.dispose()
  return image
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
