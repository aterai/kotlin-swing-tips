package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.net.URL
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer

fun makeUI(): Component {
  val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
  val label = JLabel(LocalTime.now(ZoneId.systemDefault()).format(formatter), SwingConstants.CENTER)
  val timer = Timer(100, null)
  timer.addActionListener {
    label.text = LocalTime.now(ZoneId.systemDefault()).format(formatter)
    val parent = SwingUtilities.getUnwrappedParent(label)
    if (parent != null && parent.isOpaque) {
      repaintWindowAncestor(label)
    }
  }
  val cl = Thread.currentThread().contextClassLoader
  val ttf = cl.getResource("example/YournameS7ScientificHalf.ttf")
  val tp = TextureUtil.makeTexturePanel(label, ttf)
  val digitalClock = JFrame()
  digitalClock.contentPane.add(tp)
  digitalClock.isUndecorated = true
  digitalClock.background = Color(0x0, true)
  digitalClock.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
  digitalClock.pack()
  digitalClock.setLocationRelativeTo(null)
  val combo = object : JComboBox<TexturePaints>(TexturePaints.values()) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 150.coerceAtLeast(d.width)
      return d
    }
  }
  combo.addItemListener { e ->
    val item = e.item
    if (e.stateChange == ItemEvent.SELECTED && item is TexturePaints) {
      tp.setTexturePaint(item.texturePaint)
      repaintWindowAncestor(tp)
    }
  }
  val button = JToggleButton("timer")
  button.addActionListener { e ->
    if ((e.source as? AbstractButton)?.isSelected == true) {
      val t = combo.getItemAt(combo.selectedIndex)
      tp.setTexturePaint(t.texturePaint)
      timer.start()
      digitalClock.isVisible = true
    } else {
      timer.stop()
      digitalClock.isVisible = false
    }
  }
  val p = JPanel()
  p.add(combo)
  p.add(button)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun repaintWindowAncestor(c: JComponent) {
  c.rootPane?.also {
    it.repaint(SwingUtilities.convertRectangle(c, c.bounds, it))
  }
}

private class TexturePanel(lm: LayoutManager) : JPanel(lm) {
  @Transient private var texture: Paint? = null
  fun setTexturePaint(texturePaint: Paint?) {
    texture = texturePaint
    isOpaque = texturePaint == null
  }

  override fun paintComponent(g: Graphics) {
    if (texture != null) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = texture
      g2.fillRect(0, 0, width, height)
      g2.dispose()
    }
    super.paintComponent(g)
  }
}

private enum class TexturePaints(private val description: String) {
  NULL("Color(.5f, .8f, .5f, .5f)"),
  IMAGE("Image TexturePaint"),
  CHECKER("Checker TexturePaint");

  val texturePaint
    get() = when (this) {
      IMAGE -> TextureUtil.makeImageTexture()
      CHECKER -> TextureUtil.makeCheckerTexture()
      NULL -> null
    }

  override fun toString() = description
}

private object TextureUtil {
  fun makeImageTexture(): TexturePaint {
    val path = "example/unkaku_w.png"
    val url = Thread.currentThread().contextClassLoader.getResource(path)
    val bi = url?.openStream().use(ImageIO::read) ?: makeMissingImage()
    return TexturePaint(bi, Rectangle(bi.width, bi.height))
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

  fun makeCheckerTexture(): TexturePaint {
    val cs = 6
    val sz = cs * cs
    val bi = BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB)
    val g2 = bi.createGraphics()
    g2.paint = Color(0x32C89664, true)
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
    return TexturePaint(bi, Rectangle(sz, sz))
  }

  fun makeTexturePanel(label: JLabel, url: URL?): TexturePanel {
    val font = makeFont(url) ?: label.font
    label.font = font.deriveFont(80f)
    label.background = Color(0x0, true)
    label.isOpaque = false
    val p = TexturePanel(BorderLayout(8, 8))
    p.add(label)
    p.add(JLabel("Digital display fonts by Yourname, Inc."), BorderLayout.NORTH)
    p.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
    p.background = Color(.5f, .8f, .5f, .5f)
    val dwl = DragWindowListener()
    p.addMouseListener(dwl)
    p.addMouseMotionListener(dwl)
    return p
  }

  private fun makeFont(url: URL?): Font? = runCatching {
    url?.openStream().use {
      Font.createFont(Font.TRUETYPE_FONT, it).deriveFont(12f)
    }
  }.getOrNull()
}

private class DragWindowListener : MouseAdapter() {
  private val startPt = Point()
  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.location = e.point
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = SwingUtilities.getRoot(e.component)
    if (c is Window && SwingUtilities.isLeftMouseButton(e)) {
      val pt = c.location
      c.setLocation(pt.x - startPt.x + e.x, pt.y - startPt.y + e.y)
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
