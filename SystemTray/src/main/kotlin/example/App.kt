package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

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

  val cl = Thread.currentThread().contextClassLoader
  val image = ImageIcon(cl.getResource("example/16x16.png")).image
  return TrayIcon(image, "TRAY", popup)
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
