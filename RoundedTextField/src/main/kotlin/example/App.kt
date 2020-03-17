package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.AbstractBorder

class MainPanel : JPanel(BorderLayout()) {
  init {
    val textField01 = object : JTextField(20) {
      // Unleash Your Creativity with Swing and the Java 2D API!
      // http://java.sun.com/products/jfc/tsc/articles/swing2d/index.html
      // http://web.archive.org/web/20091205092230/http://java.sun.com/products/jfc/tsc/articles/swing2d/index.html
      protected override fun paintComponent(g: Graphics) {
        if (!isOpaque()) {
          val w = getWidth() - 1
          val h = getHeight() - 1
          val g2 = g.create() as Graphics2D
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
          g2.setPaint(UIManager.getColor("TextField.background"))
          g2.fillRoundRect(0, 0, w, h, h, h)
          g2.setPaint(Color.GRAY)
          g2.drawRoundRect(0, 0, w, h, h, h)
          g2.dispose()
        }
        super.paintComponent(g)
      }

      override fun updateUI() {
        super.updateUI()
        setOpaque(false)
        // setBackground(new Color(0x0, true))
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8))
      }
    }
    textField01.setText("aaaaaaaaaaa")

    val textField02 = object : JTextField(20) {
      protected override fun paintComponent(g: Graphics) {
        if (!isOpaque()) {
          (getBorder() as? RoundedCornerBorder)?.also {
            val g2 = g.create() as Graphics2D
            g2.setPaint(getBackground())
            g2.fill(it.getBorderShape(0, 0, getWidth() - 1, getHeight() - 1))
            g2.dispose()
          }
        }
        super.paintComponent(g)
      }

      override fun updateUI() {
        super.updateUI()
        setOpaque(false)
        setBorder(RoundedCornerBorder())
      }
    }
    textField02.setText("bbbbbbbbbbbb")

    val r1 = JRadioButton("default", true)
    val r2 = JRadioButton("setOpaque(false) + TexturePaint")
    val l = ActionListener { e ->
      setOpaque(e.getSource() === r1)
      repaint()
    }
    val bg = ButtonGroup()
    val box = Box.createHorizontalBox()
    box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    listOf(r1, r2).forEach {
      it.addActionListener(l)
      it.setOpaque(false)
      bg.add(it)
      box.add(it)
    }
    add(JPanel(GridLayout(2, 1, 5, 5)).also {
      it.setOpaque(false)
      it.add(makeTitledPanel("Override: JTextField#paintComponent(...)", textField01))
      it.add(makeTitledPanel("setBorder(new RoundedCornerBorder())", textField02))
    })
    add(box, BorderLayout.NORTH)
    setBorder(BorderFactory.createEmptyBorder(2, 20, 20, 20))
    setPreferredSize(Dimension(320, 240))
  }

  protected override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (!isOpaque()) {
      val g2 = g.create() as Graphics2D
      g2.setPaint(TEXTURE)
      g2.fillRect(0, 0, getWidth(), getHeight())
      g2.dispose()
    }
  }

  companion object {
    private val TEXTURE = makeCheckerTexture()

    private fun makeTitledPanel(title: String, cmp: Component): Component {
      val p = JPanel(GridBagLayout())
      p.setBorder(BorderFactory.createTitledBorder(title))
      p.setOpaque(false)
      val c = GridBagConstraints()
      c.weightx = 1.0
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = Insets(5, 5, 5, 5)
      p.add(cmp, c)
      return p
    }

    private fun makeCheckerTexture(): TexturePaint {
      val cs = 6
      val sz = cs * cs
      val img = BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB)
      val g2 = img.createGraphics()
      g2.setPaint(Color(100, 100, 100, 50))
      g2.fillRect(0, 0, sz, sz)
      var i = 0
      while (i * cs < sz) {
        var j = 0
        while (j * cs < sz) {
          if ((i + j) % 2 == 0) {
            g2.fillRect(i * cs, j * cs, cs, cs)
          }
          j++
        }
        i++
      }
      g2.dispose()
      return TexturePaint(img, Rectangle(sz, sz))
    }
  }
}

class RoundedCornerBorder : AbstractBorder() {
  override fun paintBorder(c: Component?, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val border = getBorderShape(x, y, width - 1, height - 1)

    g2.setPaint(ALPHA_ZERO)
    // val corner = new Area(border.getBounds2D())
    val dx = x.toDouble()
    val dy = y.toDouble()
    val dw = width.toDouble()
    val dh = height.toDouble()
    val corner = Area(Rectangle2D.Double(dx, dy, dw, dh))
    corner.subtract(Area(border))
    g2.fill(corner)

    g2.setPaint(Color.GRAY)
    g2.draw(border)
    g2.dispose()
  }

  fun getBorderShape(x: Int, y: Int, w: Int, h: Int): Shape {
    val dx = x.toDouble()
    val dy = y.toDouble()
    val dw = w.toDouble()
    val dh = h.toDouble()
    return RoundRectangle2D.Double(dx, dy, dw, dh, dh, dh)
  }

  override fun getBorderInsets(c: Component) = Insets(4, 8, 4, 8)

  override fun getBorderInsets(c: Component?, insets: Insets): Insets {
    insets.set(4, 8, 4, 8)
    return insets
  }

  companion object {
    private val ALPHA_ZERO = Color(0x0, true)
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
