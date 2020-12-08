package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.ByteLookupTable
import java.awt.image.LookupOp
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border

val TEXTURE = ImageUtil.makeCheckerTexture()

fun makeUI(): Component {
  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = TEXTURE
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .6f)
      g2.fillRect(0, 0, width, height)
      g2.dispose()
    }
  }
  p.add(JButton("button"))
  val frame = JInternalFrame("InternalFrame", true, true, true, true)
  frame.contentPane = p
  frame.setSize(160, 80)
  frame.setLocation(10, 10)
  frame.isOpaque = false
  frame.isVisible = true

  val desktop: JDesktopPane = object : JDesktopPane() {
    override fun updateUI() {
      super.updateUI()
      isOpaque = false
    } // @Override protected void paintComponent(Graphics g) {
    //   super.paintComponent(g);
    //   Graphics2D g2 = (Graphics2D) g.create();
    //   g2.setPaint(new Color(100, 100, 100, 100));
    //   g2.fillRect(0, 0, getWidth(), getHeight());
    //   g2.dispose();
    // }
  }
  desktop.add(frame)
  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.isOpaque = false
    it.preferredSize = Dimension(320, 240)
  }
}

private object ImageUtil {
  fun createMenuBar(): JMenuBar {
    UIManager.put("Menu.background", Color(200, 0, 0, 0))
    UIManager.put("Menu.selectionBackground", Color(100, 100, 255, 100))
    UIManager.put("Menu.selectionForeground", Color(200, 200, 200))
    UIManager.put("Menu.useMenuBarBackgroundForTopLevel", java.lang.Boolean.TRUE)
    val mb = object : JMenuBar() {
      override fun paintComponent(g: Graphics) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = Color(100, 100, 100, 100)
        g2.fillRect(0, 0, width, height)
        g2.dispose()
      }
    }
    mb.isOpaque = false
    listOf("File", "Edit", "Help").map { createMenu(it) }
      .forEach { mb.add(it) }
    return mb
  }

  private fun createMenu(key: String): JMenu {
    val menu = TransparentMenu(key)
    menu.foreground = Color(200, 200, 200)
    menu.isOpaque = false // Motif lnf
    val sub: JMenu = TransparentMenu("Submenu")
    sub.add("JMenuItem")
    sub.add("L${"o".repeat(20)}ng")
    menu.add(sub)
    menu.add("dummy1")
    menu.add("dummy2")
    return menu
  }

  fun getFilteredImage(url: URL?): BufferedImage {
    val image = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
    val dest = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
    val b = ByteArray(256)
    for (i in b.indices) {
      b[i] = (i * .5).toInt().toByte()
    }
    val op = LookupOp(ByteLookupTable(0, b), null)
    op.filter(image, dest)
    return dest
  }

  private fun makeMissingImage(): BufferedImage {
    val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
    val w = missingIcon.iconWidth
    val h = missingIcon.iconHeight
    val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g2 = bi.createGraphics()
    missingIcon.paintIcon(null, g2, 0, 0)
    g2.dispose()
    return bi
  }

  fun makeCheckerTexture(): TexturePaint {
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
}

private class CentredBackgroundBorder(private val image: BufferedImage) : Border {
  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    val cx = (width - image.width) / 2
    val cy = (height - image.height) / 2
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.drawRenderedImage(image, AffineTransform.getTranslateInstance(cx.toDouble(), cy.toDouble()))
    g2.dispose()
  }

  override fun getBorderInsets(c: Component) = Insets(0, 0, 0, 0)

  override fun isBorderOpaque() = true
}

private class TranslucentPopupMenu : JPopupMenu() {
  override fun isOpaque() = false

  override fun add(c: Component) = c.also {
    (c as? JComponent)?.isOpaque = false
  }

  override fun add(menuItem: JMenuItem): JMenuItem = super.add(menuItem).also {
    menuItem.isOpaque = false
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = POPUP_LEFT
    g2.fillRect(0, 0, LEFT_WIDTH, height)
    g2.paint = POPUP_BACK
    g2.fillRect(LEFT_WIDTH, 0, width, height)
    g2.dispose()
  }

  companion object {
    private val POPUP_BACK = Color(250, 250, 250, 100)
    private val POPUP_LEFT = Color(230, 230, 230, 100)
    private const val LEFT_WIDTH = 24
  }
}

private class TransparentMenu(title: String?) : JMenu(title) {
  private var popupMenu: JPopupMenu? = null

  private fun ensurePopupMenuCreated2() {
    if (popupMenu == null) {
      this.popupMenu = TranslucentPopupMenu().also {
        it.invoker = this
        popupListener = createWinListener(it)
      }
    }
  }

  override fun getPopupMenu(): JPopupMenu {
    ensurePopupMenuCreated2()
    return popupMenu!!
  }

  override fun add(menuItem: JMenuItem): JMenuItem {
    ensurePopupMenuCreated2()
    menuItem.isOpaque = false
    return popupMenu!!.add(menuItem)
  }

  override fun add(c: Component): Component {
    ensurePopupMenuCreated2()
    if (c is JComponent) {
      c.isOpaque = false
    }
    popupMenu!!.add(c)
    return c
  }

  override fun addSeparator() {
    ensurePopupMenuCreated2()
    popupMenu!!.addSeparator()
  }

  override fun insert(s: String, pos: Int) {
    require(pos >= 0) { "index less than zero." }
    ensurePopupMenuCreated2()
    popupMenu!!.insert(JMenuItem(s), pos)
  }

  override fun insert(mi: JMenuItem, pos: Int): JMenuItem {
    require(pos >= 0) { "index less than zero." }
    ensurePopupMenuCreated2()
    popupMenu!!.insert(mi, pos)
    return mi
  }

  override fun insertSeparator(index: Int) {
    require(index >= 0) { "Separator index less than zero." }
    ensurePopupMenuCreated2()
    popupMenu!!.insert(JPopupMenu.Separator(), index)
  }

  override fun isPopupMenuVisible(): Boolean {
    ensurePopupMenuCreated2()
    return popupMenu!!.isVisible
  }
}

private class TranslucentPopupFactory : PopupFactory() {
  override fun getPopup(
    owner: Component,
    contents: Component,
    x: Int,
    y: Int
  ) = TranslucentPopup(owner, contents, x, y)
}

private class TranslucentPopup(
  owner: Component?,
  contents: Component,
  ownerX: Int,
  ownerY: Int
) : Popup(owner, contents, ownerX, ownerY) {
  private val popupWindow = JWindow()

  init {
    popupWindow.background = Color(0x0, true) // Java 1.7.0
    popupWindow.setLocation(ownerX, ownerY)
    popupWindow.contentPane.add(contents)
    contents.invalidate()
  }

  override fun show() {
    popupWindow.isVisible = true
    popupWindow.pack()
  }

  override fun hide() {
    popupWindow.isVisible = false
    popupWindow.removeAll()
    popupWindow.dispose()
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

    PopupFactory.setSharedInstance(TranslucentPopupFactory())
    val frame = object : JFrame() {
      override fun createRootPane() = object : JRootPane() {
        override fun paintComponent(g: Graphics) {
          super.paintComponent(g)
          val g2 = g.create() as? Graphics2D ?: return
          g2.paint = TEXTURE
          g2.fillRect(0, 0, width, height)
          g2.dispose()
        }

        override fun updateUI() {
          super.updateUI()
          val cl = Thread.currentThread().contextClassLoader
          val url = cl.getResource("example/test.jpg")
          val bi = ImageUtil.getFilteredImage(url)
          border = CentredBackgroundBorder(bi)
          isOpaque = false
        }
      }
    }
    val contentPane = frame.contentPane
    (contentPane as? JComponent)?.isOpaque = false
    frame.jMenuBar = ImageUtil.createMenuBar()
    frame.contentPane.add(makeUI())
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
  }
}
