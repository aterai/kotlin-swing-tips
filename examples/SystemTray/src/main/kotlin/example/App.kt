package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI() = JPanel().also {
  it.preferredSize = Dimension(320, 240)
}

private fun makeTrayIcon(frame: JFrame): TrayIcon {
  val item1 = MenuItem("OPEN")
  item1.addActionListener {
    frame.extendedState = Frame.NORMAL
    frame.isVisible = true
  }
  val item2 = MenuItem("EXIT")
  item2.addActionListener {
    val tray = SystemTray.getSystemTray()
    for (icon in tray.trayIcons) {
      tray.remove(icon)
    }
    frame.dispose()
  }
  val popup = PopupMenu()
  popup.add(item1)
  popup.add(item2)

  val path = "example/16x16.png"
  val cl = Thread.currentThread().contextClassLoader
  val img = cl
    .getResource(path)
    ?.openStream()
    ?.use(ImageIO::read)
    ?: makeDefaultTrayImage()
  return TrayIcon(img, "TRAY", popup)
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

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }

    val frame = JFrame()
    if (SystemTray.isSupported()) {
      frame.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
      frame.addWindowStateListener { e ->
        if (e.newState == Frame.ICONIFIED) {
          e.window.dispose()
        }
      }
      runCatching {
        SystemTray.getSystemTray().add(makeTrayIcon(frame))
      }
    } else {
      frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    }
    frame.contentPane.add(makeUI())
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
  }
}
