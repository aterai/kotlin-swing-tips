package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.awt.image.ByteLookupTable
import java.awt.image.LookupOp
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border

private val texture = makeCheckerTexture()

private fun makeTitleWithIcon(url: URL?, title: String, align: String) =
  "<html><p align='$align'><img src='$url' align='$align' />&nbsp;$title</p></html>"

private fun makeButton(title: String): AbstractButton {
  return object : JButton(title) {
    override fun updateUI() {
      super.updateUI()
      verticalAlignment = SwingConstants.CENTER
      verticalTextPosition = SwingConstants.CENTER
      horizontalAlignment = SwingConstants.CENTER
      horizontalTextPosition = SwingConstants.CENTER
      border = BorderFactory.createEmptyBorder(2, 8, 2, 8)
      margin = Insets(2, 8, 2, 8)
      isBorderPainted = false
      isContentAreaFilled = false
      isFocusPainted = false
      isOpaque = false
      foreground = Color.WHITE
      icon = TranslucentButtonIcon(this)
    }
  }
}

private fun getFilteredImage(url: URL?): BufferedImage {
  val img = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val dest = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
  val b = ByteArray(256)
  for (i in b.indices) {
    b[i] = (i * .5).toInt().toByte()
  }
  val op = LookupOp(ByteLookupTable(0, b), null)
  op.filter(img, dest)
  return dest
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private fun makeCheckerTexture(): TexturePaint {
  val cs = 6
  val sz = cs * cs
  val img = BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB)
  val g2 = img.createGraphics()
  g2.paint = Color(120, 120, 120)
  g2.fillRect(0, 0, sz, sz)
  g2.paint = Color(200, 200, 200, 20)
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

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  // Icon: refer to http://chrfb.deviantart.com/art/quot-ecqlipse-2-quot-PNG-59941546
  val url = cl.getResource("example/RECYCLE BIN - EMPTY_16x16-32.png")
  val panel = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = texture
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      super.paintComponent(g)
    }
  }

  panel.add(makeButton(makeTitleWithIcon(url, "align=top", "top")))
  panel.add(makeButton(makeTitleWithIcon(url, "align=middle", "middle")))
  panel.add(makeButton(makeTitleWithIcon(url, "align=bottom", "bottom")))
  val icon = ImageIcon(url)
  val label = JLabel("JLabel", icon, SwingConstants.CENTER)
  label.foreground = Color.WHITE
  label.alignmentX = Component.CENTER_ALIGNMENT
  val b = makeButton("")
  b.alignmentX = Component.CENTER_ALIGNMENT
  val p = JPanel()
  p.layout = OverlayLayout(p)
  p.isOpaque = false
  p.add(label)
  p.add(b)
  panel.add(p)
  panel.add(makeButton("? text"))
  panel.add(TranslucentButton("TranslucentButton", icon))
  panel.add(makeButton("1"))
  panel.add(makeButton("22222222"))
  panel.add(makeButton("333333333333333333"))
  panel.add(makeButton("44444444444444444444444444444"))
  val bi = getFilteredImage(cl.getResource("example/test.jpg"))
  panel.border = CentredBackgroundBorder(bi)
  // setBackground(new Color(50, 50, 50));
  panel.isOpaque = false
  panel.preferredSize = Dimension(320, 240)
  return panel
}

private class TranslucentButton(text: String?, icon: Icon?) : JButton(text, icon) {
  override fun updateUI() {
    super.updateUI()
    isContentAreaFilled = false
    isFocusPainted = false
    isBorderPainted = false
    isOpaque = false
    foreground = Color.WHITE
  }

  override fun paintComponent(g: Graphics) {
    val x = 0f
    val y = 0f
    val w = width.toFloat()
    val h = height.toFloat()
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val area = RoundRectangle2D.Float(x, y, w - 1f, h - 1f, R, R)
    var ssc = TL
    var bgc = BR
    val m = getModel()
    if (m.isPressed) {
      ssc = SB
      bgc = ST
    } else if (m.isRollover) {
      ssc = ST
      bgc = SB
    }
    g2.paint = GradientPaint(x, y, ssc, x, y + h, bgc, true)
    g2.fill(area)
    g2.paint = BR
    g2.draw(area)
    g2.dispose()
    super.paintComponent(g)
  }

  companion object {
    private val TL = Color(1f, 1f, 1f, .2f)
    private val BR = Color(0f, 0f, 0f, .4f)
    private val ST = Color(1f, 1f, 1f, .2f)
    private val SB = Color(1f, 1f, 1f, .1f)
    private const val R = 8f
  }
}

private class TranslucentButtonIcon(c: JComponent) : Icon {
  private var width = 100
  private var height = 20

  init {
    val i = c.insets
    val d = c.preferredSize
    width = d.width - i.left - i.right
    height = d.height - i.top - i.bottom
  }

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    if (c is AbstractButton) {
      // val i = c.insets
      val w = c.getWidth()
      val h = c.getHeight()
      val r = SwingUtilities.calculateInnerArea(c, null)
      width = r.width
      height = r.height
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val fx = (x - r.minX).toFloat()
      val fy = (y - r.minY).toFloat()
      val area = RoundRectangle2D.Float(fx, fy, w - 1f, h - 1f, R, R)
      var ssc = TL
      var bgc = BR
      val m = c.model
      if (m.isPressed) {
        ssc = SB
        bgc = ST
      } else if (m.isRollover) {
        ssc = ST
        bgc = SB
      }
      g2.paint = GradientPaint(0f, 0f, ssc, 0f, h.toFloat(), bgc, true)
      g2.fill(area)
      g2.paint = BR
      g2.draw(area)
      g2.dispose()
    }
  }

  override fun getIconWidth() = width.coerceAtLeast(100)

  override fun getIconHeight() = height.coerceAtLeast(20)

  companion object {
    private val TL = Color(1f, 1f, 1f, .2f)
    private val BR = Color(0f, 0f, 0f, .4f)
    private val ST = Color(1f, 1f, 1f, .2f)
    private val SB = Color(1f, 1f, 1f, .1f)
    private const val R = 8f
  }
}

// https://community.oracle.com/thread/1395763 How can I use TextArea with Background Picture ?
// https://ateraimemo.com/Swing/CentredBackgroundBorder.html
private class CentredBackgroundBorder(private val image: BufferedImage) : Border {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int
  ) {
    val cx = (width - image.width) / 2.0
    val cy = (height - image.height) / 2.0
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.drawRenderedImage(image, AffineTransform.getTranslateInstance(cx, cy))
    g2.dispose()
  }

  override fun getBorderInsets(c: Component) = Insets(0, 0, 0, 0)

  override fun isBorderOpaque() = true
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
