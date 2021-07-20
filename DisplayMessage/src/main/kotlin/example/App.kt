package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel()

  val openItem = MenuItem("OPEN")
  openItem.addActionListener {
    (p.topLevelAncestor as? Window)?.isVisible = true
  }
  val exitItem = MenuItem("EXIT")
  exitItem.addActionListener {
    (p.topLevelAncestor as? Window)?.dispose()
    val tray = SystemTray.getSystemTray()
    for (icon in tray.trayIcons) {
      tray.remove(icon)
    }
  }
  val popup = PopupMenu()
  popup.add(openItem)
  popup.add(exitItem)

  val path = "example/16x16.png"
  val cl = Thread.currentThread().contextClassLoader
  val image = cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeDefaultTrayImage()
  runCatching {
    SystemTray.getSystemTray().add(TrayIcon(image, "TRAY", popup))
  }

  // ERROR, WARNING, INFO, NONE
  val messageType = JComboBox(TrayIcon.MessageType.values())
  val messageButton = JButton("TrayIcon#displayMessage()")
  messageButton.addActionListener {
    val icons = SystemTray.getSystemTray().trayIcons
    if (icons.isNotEmpty()) {
      icons[0].displayMessage("caption", "text text", messageType.getItemAt(messageType.selectedIndex))
    }
  }

  p.add(messageType)
  p.add(messageButton)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeDefaultTrayImage(): BufferedImage {
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
