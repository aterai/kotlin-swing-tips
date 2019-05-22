package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel() {
  init {
    EventQueue.invokeLater { getRootPane().setJMenuBar(createMenuBar()) }
    setPreferredSize(Dimension(320, 240))
  }

  private fun createMenuBar(): JMenuBar {
    val mb = object : JMenuBar() {
      @Transient private val texture = makeCheckerTexture()

      protected override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        g2.setPaint(texture)
        g2.fillRect(0, 0, getWidth(), getHeight())
        g2.dispose()
      }
    }
    mb.setOpaque(false)
    for (key in arrayOf("File", "Edit", "Help")) {
      mb.add(createMenu(key))
    }
    return mb
  }

  private fun createMenu(key: String): JMenu {
    val menu = object : JMenu(key) {
      protected override fun fireStateChanged() {
        val m = getModel()
        if (m.isPressed() && m.isArmed()) {
          setOpaque(true)
        } else if (m.isSelected()) {
          setOpaque(true)
        } else if (isRolloverEnabled() && m.isRollover()) {
          setOpaque(true)
        } else {
          setOpaque(false)
        }
        super.fireStateChanged()
      }

      override fun updateUI() {
        super.updateUI()
        setOpaque(false) // Motif lnf
      }
    }
    // System.out.println(System.getProperty("os.name"));
    // System.out.println(System.getProperty("os.version"));
    if ("Windows XP" == System.getProperty("os.name")) {
      menu.setBackground(Color(0x0, true)) // XXX Windows XP lnf?
    }
    menu.add("dummy1")
    menu.add("dummy2")
    menu.add("dummy3")
    return menu
  }

  protected fun makeCheckerTexture(): TexturePaint {
    val cs = 6
    val sz = cs * cs
    val img = BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    g2.setPaint(Color(200, 150, 100, 50))
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
    // TEST: UIManager.put(key, Boolean.FALSE);
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
