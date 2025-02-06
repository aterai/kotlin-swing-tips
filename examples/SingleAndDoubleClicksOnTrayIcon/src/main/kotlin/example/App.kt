package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

private val popup = JPopupMenu()

fun makeUI(): Component {
  val supported = SystemTray.isSupported()
  val scroll = JScrollPane(JTextArea("SystemTray.isSupported(): $supported"))
  return JPanel(BorderLayout()).also {
    it.add(scroll)
    EventQueue.invokeLater {
      (it.topLevelAncestor as? Frame)?.also { f ->
        val dialog = JPopupMenu()
        dialog.layout = BorderLayout()
        dialog.add(makeLookAndFeelBox())
        dialog.pack()
        initPopupMenu(dialog, f)
      }
    }
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeLookAndFeelBox(): Box {
  val group = ButtonGroup()
  val box = Box.createVerticalBox()
  for (info in UIManager.getInstalledLookAndFeels()) {
    val b = JRadioButton(info.name)
    LookAndFeelUtils.initLookAndFeelAction(info, b)
    b.addActionListener {
      EventQueue.invokeLater { SwingUtilities.updateComponentTreeUI(popup) }
    }
    group.add(b)
    box.add(b)
  }
  box.border = BorderFactory.createEmptyBorder(5, 25, 5, 25)
  return box
}

private fun openLookAndFeelBox(lnf: JPopupMenu, tmp: JDialog, pt: Point) {
  val p = TrayIconPopupMenuUtils.adjustPopupLocation(lnf, pt.x, pt.y)
  p.move(p.x - 20, p.y - 20)
  tmp.location = p
  tmp.isVisible = true
  tmp.toFront()
  lnf.show(tmp, 0, 0)
}

private fun initPopupMenu(lnf: JPopupMenu, frame: Frame) {
  val tmp = JDialog()
  tmp.isUndecorated = true
  tmp.isAlwaysOnTop = true
  val loc = Point()
  val image = makeDefaultTrayImage()
  val icon = TrayIcon(image, "TRAY", null)
  icon.addMouseListener(TrayIconPopupMenuHandler(popup, tmp))
  icon.addMouseListener(object : MouseAdapter() {
    private val timer = Timer(500) { e ->
      (e.source as? Timer)?.stop()
      openLookAndFeelBox(lnf, tmp, loc)
    }

    override fun mousePressed(e: MouseEvent) {
      loc.location = e.point
      if (SwingUtilities.isLeftMouseButton(e)) {
        timer.delay = 500
        timer.isRepeats = false
        timer.start()
      }
    }

    override fun mouseClicked(e: MouseEvent) {
      val isDoubleClick = e.clickCount >= 2
      if (SwingUtilities.isLeftMouseButton(e) && isDoubleClick) {
        timer.stop()
        lnf.isVisible = false
        frame.isVisible = true
      }
    }
  })
  runCatching {
    SystemTray.getSystemTray().add(icon)
  }
  popup.addPopupMenuListener(object : PopupMenuListener {
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      // not needed
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      tmp.isVisible = false
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
      tmp.isVisible = false
    }
  })
  popup.add(makeLabel("Quick access", "Left-click")).addActionListener {
    EventQueue.invokeLater {
      openLookAndFeelBox(
        lnf,
        tmp,
        loc,
      )
    }
  }
  popup.add(makeLabel("Settings", "Double-click")).addActionListener {
    frame.extendedState = Frame.NORMAL
    frame.isVisible = true
  }
  popup.addSeparator()
  popup.add(makeLabel("Documentation", null))
  popup.add(makeLabel("Report bug", null))
  popup.addSeparator()
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

private fun makeLabel(title: String?, help: String?): String {
  val width = 150
  val table = "<html><table width='$width'>"
  val left = title ?: ""
  val td1 = "<td style='text-align:left'>$left</td>"
  val right = help ?: ""
  val td2 = "<td style='text-align:right'>$right</td>"
  return table + td1 + td2
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

private object TrayIconPopupMenuUtils {
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

  fun adjustPopupLocation(
    popup: JPopupMenu,
    px: Int,
    py: Int,
  ): Point {
    val p = Point(px, py)
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
  private val tmp: Window,
) : MouseAdapter() {
  private fun showPopupMenu(e: MouseEvent) {
    if (e.isPopupTrigger) {
      val p = TrayIconPopupMenuUtils.adjustPopupLocation(popup, e.x, e.y)
      tmp.location = p
      tmp.isVisible = true
      // tmp.toFront()
      popup.show(tmp, 0, 0)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    showPopupMenu(e)
  }

  override fun mousePressed(e: MouseEvent) {
    showPopupMenu(e)
  }
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

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
