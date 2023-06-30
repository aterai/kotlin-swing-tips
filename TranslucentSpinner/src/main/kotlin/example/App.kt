package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.JSpinner.DefaultEditor

private val TEXTURE = TextureUtils.createCheckerTexture(4, Color(0xEE_EE_EE))

fun makeUI(): Component {
  val d = UIDefaults()
  val painter1 = Painter<Component> { g, _, w, h ->
    g.color = Color(100, 100, 100, 100)
    g.fillRect(0, 0, w, h)
  }

  val painter2 = Painter<Component> { g, _, w, h ->
    g.color = Color(100, 200, 200, 100)
    g.fillRect(0, 0, w, h)
  }
  d["Spinner:Panel:\"Spinner.formattedTextField\"[Enabled].backgroundPainter"] = painter1
  d["Spinner:Panel:\"Spinner.formattedTextField\"[Focused].backgroundPainter"] = painter2
  d["Spinner:Panel:\"Spinner.formattedTextField\"[Selected].backgroundPainter"] = painter2

  val painter3 = Painter<Component> { g, _, w, h ->
    g.color = Color(100, 100, 200, 100)
    g.fillRect(0, 0, w, h)
  }

  val painter4 = Painter<Component> { g, _, w, h ->
    g.color = Color(120, 120, 120, 100)
    g.fillRect(0, 0, w, h)
  }
  d["Spinner:\"Spinner.previousButton\"[Enabled].backgroundPainter"] = painter3
  d["Spinner:\"Spinner.previousButton\"[Focused+MouseOver].backgroundPainter"] = painter3
  d["Spinner:\"Spinner.previousButton\"[Focused+Pressed].backgroundPainter"] = painter3
  d["Spinner:\"Spinner.previousButton\"[Focused].backgroundPainter"] = painter3
  d["Spinner:\"Spinner.previousButton\"[MouseOver].backgroundPainter"] = painter3
  d["Spinner:\"Spinner.previousButton\"[Pressed].backgroundPainter"] = painter4
  d["Spinner:\"Spinner.nextButton\"[Enabled].backgroundPainter"] = painter3
  d["Spinner:\"Spinner.nextButton\"[Focused+MouseOver].backgroundPainter"] = painter3
  d["Spinner:\"Spinner.nextButton\"[Focused+Pressed].backgroundPainter"] = painter3
  d["Spinner:\"Spinner.nextButton\"[Focused].backgroundPainter"] = painter3
  d["Spinner:\"Spinner.nextButton\"[MouseOver].backgroundPainter"] = painter3
  d["Spinner:\"Spinner.nextButton\"[Pressed].backgroundPainter"] = painter4

  val model = SpinnerNumberModel(0, 0, 100, 5)
  val spinner1 = JSpinner(model)
  (spinner1.editor as? DefaultEditor)?.textField?.putClientProperty("Nimbus.Overrides", d)
  configureSpinnerButtons(spinner1, d)

  val spinner2 = object : JSpinner(model) {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = Color(0x64_FF_00_00, true)
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      super.paintComponent(g)
    }
  }
  spinner2.isOpaque = false
  spinner2.editor.isOpaque = false
  (spinner2.editor as? DefaultEditor)?.textField?.isOpaque = false

  val p = JPanel(GridLayout(0, 1, 20, 20))
  p.isOpaque = false
  p.add(JSpinner(model))
  p.add(spinner1)
  p.add(spinner2)

  val pp = object : JPanel(BorderLayout()) {
    public override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = TEXTURE
      g2.fillRect(0, 0, width, height)
      g2.dispose()
    }
  }
  pp.add(p, BorderLayout.NORTH)
  pp.border = BorderFactory.createEmptyBorder(20, 10, 10, 10)
  pp.preferredSize = Dimension(320, 240)
  return pp
}

private fun configureSpinnerButtons(comp: Container, d: UIDefaults) {
  for (c in comp.components) {
    val name = c.name
    if (c is JButton && name?.endsWith("Button") == true) {
      c.putClientProperty("Nimbus.Overrides", d)
    } else if (c is Container) {
      configureSpinnerButtons(c, d)
    }
  }
}

private object TextureUtils {
  fun createCheckerTexture(cs: Int, color: Color): TexturePaint {
    val size = cs * cs
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    g2.paint = color.brighter()
    g2.fillRect(0, 0, size, size)
    g2.paint = color
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
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
