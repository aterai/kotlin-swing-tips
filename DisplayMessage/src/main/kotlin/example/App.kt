package example

import java.awt.* // ktlint-disable no-wildcard-imports
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

  val cl = Thread.currentThread().contextClassLoader
  val image = ImageIcon(cl.getResource("example/16x16.png")).image
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
