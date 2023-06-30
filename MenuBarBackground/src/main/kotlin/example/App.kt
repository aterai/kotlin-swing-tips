package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*

fun makeUI() = JPanel().also {
  EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
  it.preferredSize = Dimension(320, 240)
}

fun createMenuBar(): JMenuBar {
  val mb = object : JMenuBar() {
    private val texture = makeCheckerTexture()

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = texture
      g2.fillRect(0, 0, width, height)
      g2.dispose()
    }
  }
  mb.isOpaque = false
  for (key in arrayOf("File", "Edit", "Help")) {
    mb.add(createMenu(key))
  }
  return mb
}

fun createMenu(key: String): JMenu {
  val menu = object : JMenu(key) {
    override fun fireStateChanged() {
      val m = getModel()
      isOpaque = when {
        m.isPressed && m.isArmed -> true
        m.isSelected -> true
        else -> isRolloverEnabled && m.isRollover
      }
      super.fireStateChanged()
    }

    override fun updateUI() {
      super.updateUI()
      isOpaque = false // Motif lnf
    }
  }
  // println(System.getProperty("os.name"))
  // println(System.getProperty("os.version"))
  if ("Windows XP" == System.getProperty("os.name")) {
    menu.background = Color(0x0, true) // XXX Windows XP lnf?
  }
  menu.add("JMenuItem1")
  menu.add("JMenuItem2")
  menu.add("JMenuItem3")
  return menu
}

fun makeCheckerTexture(): TexturePaint {
  val cs = 6
  val sz = cs * cs
  val img = BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB)
  val g2 = img.createGraphics()
  g2.paint = Color(200, 150, 100, 50)
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

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    // val key = "Menu.useMenuBarBackgroundForTopLevel"
    // println("$key: ${UIManager.getBoolean(key)}")
    // TEST: UIManager.put(key, false)
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
