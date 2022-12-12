package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

private val popup = JPopupMenu()

fun makeUI(): Component {
  val group = ButtonGroup()
  val box = Box.createVerticalBox()
  for (info in UIManager.getInstalledLookAndFeels()) {
    val b = JRadioButton(info.name)
    LookAndFeelUtil.initLookAndFeelAction(info, b)
    b.addActionListener {
      EventQueue.invokeLater { SwingUtilities.updateComponentTreeUI(popup) }
    }
    group.add(b)
    box.add(b)
  }
  box.add(Box.createVerticalGlue())
  box.border = BorderFactory.createEmptyBorder(5, 25, 5, 25)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater {
      (it.topLevelAncestor as? Frame)?.also { frame ->
        initPopupMenu(frame)
      }
    }
    it.add(JLabel("SystemTray.isSupported(): " + SystemTray.isSupported()), BorderLayout.NORTH)
    it.add(box)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initPopupMenu(frame: Frame) {
  val dummy = JDialog()
  dummy.isUndecorated = true
  val cl = Thread.currentThread().contextClassLoader
  val path = "example/16x16.png"
  val img = cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeDefaultTrayImage()
  val icon = TrayIcon(img, "TRAY", null)
  icon.addMouseListener(TrayIconPopupMenuHandler(popup, dummy))
  runCatching {
    SystemTray.getSystemTray().add(icon)
  }
  val popupMenuHandler = object : PopupMenuListener {
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      /* not needed */
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      dummy.isVisible = false
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
      dummy.isVisible = false
    }
  }
  popup.addPopupMenuListener(popupMenuHandler)
  popup.add(JCheckBoxMenuItem("JCheckBoxMenuItem"))
  popup.add(JRadioButtonMenuItem("JRadioButtonMenuItem"))
  popup.add(JRadioButtonMenuItem("JRadioButtonMenuItem 1234567890"))
  popup.add("Open").addActionListener {
    frame.extendedState = Frame.NORMAL
    frame.isVisible = true
  }
  popup.add("Exit").addActionListener {
    val tray = SystemTray.getSystemTray()
    for (ti in tray.trayIcons) {
      tray.remove(ti)
    }
    for (f in Frame.getFrames()) {
      f.dispose()
    }
  }
}

private fun makeDefaultTrayImage(): Image {
  val icon = UIManager.getIcon("InternalFrame.icon")
  val w = icon.iconWidth
  val h = icon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  icon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private object TrayIconPopupMenuUtil {
  // Try to find GraphicsConfiguration, that includes mouse pointer position
  private fun getGraphicsConfiguration(p: Point): GraphicsConfiguration? {
    var gc: GraphicsConfiguration? = null
    for (gd in GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices) {
      if (gd.type == GraphicsDevice.TYPE_RASTER_SCREEN) {
        val dgc = gd.defaultConfiguration
        if (dgc.bounds.contains(p)) {
          gc = dgc
          break
        }
      }
    }
    return gc
  }

  fun adjustPopupLocation(popup: JPopupMenu, xpt: Int, ypt: Int): Point {
    val p = Point(xpt, ypt)
    if (GraphicsEnvironment.isHeadless()) {
      return p
    }
    var gc = getGraphicsConfiguration(p)
    if (gc == null && popup.invoker != null) {
      gc = popup.invoker.graphicsConfiguration
    }
    val screenBounds = gc?.bounds ?: Rectangle(Toolkit.getDefaultToolkit().screenSize)
    val size = popup.preferredSize
    val pw = p.x.toLong() + size.width.toLong()
    val ph = p.y.toLong() + size.height.toLong()
    if (pw > screenBounds.x + screenBounds.width) {
      p.x -= size.width
    }
    if (ph > screenBounds.y + screenBounds.height) {
      p.y -= size.height
    }
    p.x = p.x.coerceAtLeast(screenBounds.x)
    p.y = p.y.coerceAtLeast(screenBounds.y)
    return p
  }
}

private class TrayIconPopupMenuHandler(
  private val popup: JPopupMenu,
  private val dummy: Window
) : MouseAdapter() {
  private fun showPopupMenu(e: MouseEvent) {
    if (e.isPopupTrigger) {
      val p = TrayIconPopupMenuUtil.adjustPopupLocation(popup, e.x, e.y)
      dummy.location = p
      dummy.isVisible = true
      // dummy.toFront()
      popup.show(dummy, 0, 0)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    showPopupMenu(e)
  }

  override fun mousePressed(e: MouseEvent) {
    showPopupMenu(e)
  }
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
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
    UnsupportedLookAndFeelException::class
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

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      if (SystemTray.isSupported()) {
        defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
        addWindowStateListener { e ->
          if (e.newState == Frame.ICONIFIED) {
            e.window.dispose()
          }
        }
      } else {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      }
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
