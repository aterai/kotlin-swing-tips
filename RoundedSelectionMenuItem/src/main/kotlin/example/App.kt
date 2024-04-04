package example

import com.sun.java.swing.plaf.windows.WindowsMenuItemUI
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.awt.image.VolatileImage
import javax.swing.*
import javax.swing.plaf.basic.BasicMenuItemUI
import javax.swing.plaf.synth.SynthMenuItemUI

fun makeUI(): Component {
  UIManager.put("MenuItem.borderPainted", false)

  val sub = makeMenu()
  sub.setMnemonic('M')
  val ks1 = KeyStroke.getKeyStroke(KeyEvent.VK_1, 0)
  val ks2 = KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK)
  val ks3 = KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.SHIFT_DOWN_MASK)
  sub.add("MenuItem1").accelerator = ks1
  sub.add("MenuItem2").accelerator = ks2
  sub.add("MenuItem3").accelerator = ks3

  val menu = LookAndFeelUtils.createLookAndFeelMenu()
  menu.setMnemonic('L')
  menu.add(sub)

  val mb = JMenuBar()
  mb.add(menu)
  mb.add(sub)

  val popup = makePopupMenu()
  popup.add("MenuItem4").accelerator = ks1
  popup.add("MenuItem5").accelerator = ks2
  popup.add("MenuItem6").accelerator = ks3

  val tree = JTree()
  tree.componentPopupMenu = popup

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePopupMenu(): JPopupMenu {
  return object : JPopupMenu() {
    override fun add(s: String): JMenuItem {
      return add(object : JMenuItem(s) {
        override fun updateUI() {
          super.updateUI()
          val ui = getUI()
          if (ui is WindowsMenuItemUI) {
            setUI(WindowsRoundMenuItemUI())
          } else if (ui !is SynthMenuItemUI) {
            setUI(BasicRoundMenuItemUI())
          }
        }
      })
    }
  }
}

private fun makeMenu(): JMenu {
  return object : JMenu("JMenu(M)") {
    override fun add(s: String): JMenuItem {
      return add(object : JMenuItem(s) {
        override fun updateUI() {
          super.updateUI()
          val ui = getUI()
          if (ui is WindowsMenuItemUI) {
            setUI(WindowsRoundMenuItemUI2())
          } else if (ui !is SynthMenuItemUI) {
            setUI(BasicRoundMenuItemUI())
          }
        }
      })
    }
  }
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
}

private class BasicRoundMenuItemUI : BasicMenuItemUI() {
  override fun paintBackground(g: Graphics, menuItem: JMenuItem, bgColor: Color) {
    val model = menuItem.model
    val oldColor = g.color
    val menuWidth = menuItem.width
    val menuHeight = menuItem.height
    if (menuItem.isOpaque) {
      if (model.isArmed || menuItem is JMenu && model.isSelected) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON,
        )
        g2.paint = menuItem.background
        g2.fillRect(0, 0, menuWidth, menuHeight)
        g2.color = bgColor
        g2.fillRoundRect(2, 2, menuWidth - 4, menuHeight - 4, 8, 8)
        g2.dispose()
      } else {
        g.color = menuItem.background
        g.fillRect(0, 0, menuWidth, menuHeight)
      }
      g.color = oldColor
    } else if (model.isArmed || menuItem is JMenu && model.isSelected) {
      g.color = bgColor
      g.fillRect(0, 0, menuWidth, menuHeight)
      g.color = oldColor
    }
  }
}

private class WindowsRoundMenuItemUI : WindowsMenuItemUI() {
  private var buffer: BufferedImage? = null

  override fun paintBackground(g: Graphics, menuItem: JMenuItem, bgColor: Color) {
    val model = menuItem.model
    if (model.isArmed || menuItem is JMenu && model.isSelected) {
      val width = menuItem.width
      val height = menuItem.height
      val buf = buffer
        ?.takeIf { b -> b.width == width && b.height == height }
        ?: BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      buffer = buf
      val g2 = buf.createGraphics()
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 8f, 8f))
      g2.composite = AlphaComposite.SrcAtop
      super.paintBackground(g2, menuItem, bgColor)
      g2.dispose()
      g.drawImage(buf, 0, 0, menuItem)
    } else {
      super.paintBackground(g, menuItem, bgColor)
    }
  }
}

private class WindowsRoundMenuItemUI2 : WindowsMenuItemUI() {
  private var buffer: VolatileImage? = null

  override fun paintBackground(g: Graphics, menuItem: JMenuItem, bgColor: Color) {
    val model = menuItem.model
    if (model.isArmed || menuItem is JMenu && model.isSelected) {
      paintSelectedBackground(g, menuItem, bgColor)
    } else {
      super.paintBackground(g, menuItem, bgColor)
    }
  }

  private fun paintSelectedBackground(g: Graphics, menuItem: JMenuItem, bgColor: Color) {
    val width = menuItem.width
    val height = menuItem.height
    val config = (g as? Graphics2D)?.deviceConfiguration ?: return
    do {
      var status = buffer?.let {
        it.validate(config)
      } ?: VolatileImage.IMAGE_INCOMPATIBLE
      if (status == VolatileImage.IMAGE_INCOMPATIBLE ||
        status == VolatileImage.IMAGE_RESTORED
      ) {
        val buf = buffer
          ?.takeIf { b -> b.width == width && b.height == height }
          ?.takeIf { status != VolatileImage.IMAGE_INCOMPATIBLE }
          ?: let {
            buffer?.flush()
            config.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT)
          }
        buffer = buf
        val g2 = buf.createGraphics()
        g2.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON,
        )
        g2.composite = AlphaComposite.Clear
        g2.fillRect(0, 0, width, height)
        g2.setPaintMode()
        g2.paint = Color.WHITE
        g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 8f, 8f))
        g2.composite = AlphaComposite.SrcAtop
        super.paintBackground(g2, menuItem, bgColor)
        g2.dispose()
      }
    } while (buffer?.contentsLost() ?: false)
    g.drawImage(buffer, 0, 0, menuItem)
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      UIManager.put("PopupMenuUI", "example.RoundedPopupMenuUI")
      // UIManager.put("MenuItemUI", "example.WindowsRoundedMenuItemUI")
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
