package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private val dialog = JDialog()
private val animator = Timer(100, null)
private val cl = Thread.currentThread().contextClassLoader
private val images = arrayOf(
  ImageIcon(cl.getResource("example/16x16.png")).image,
  ImageIcon(cl.getResource("example/16x16l.png")).image,
  ImageIcon(cl.getResource("example/16x16.png")).image,
  ImageIcon(cl.getResource("example/16x16r.png")).image
)
private var idx = 0

private fun makeTrayPopupMenu(icon: TrayIcon, p: JComponent): PopupMenu {
  val item1 = MenuItem("Open:Frame")
  item1.addActionListener {
    val c = p.topLevelAncestor
    (c as? Window)?.isVisible = true
  }

  val item2 = MenuItem("Open:Dialog")
  item2.addActionListener { dialog.isVisible = true }

  val item3 = MenuItem("Animation:Start")
  item3.addActionListener { animator.start() }

  val item4 = MenuItem("Animation:Stop")
  item4.addActionListener {
    animator.stop()
    icon.image = images[0]
  }

  val item5 = MenuItem("Exit")
  item5.addActionListener {
    animator.stop()
    val tray = SystemTray.getSystemTray()
    tray.trayIcons.forEach { tray.remove(it) }
    Frame.getFrames().forEach { it.dispose() }
  }

  val popup = PopupMenu()
  popup.add(item1)
  popup.add(item2)
  popup.addSeparator()
  popup.add(item3)
  popup.add(item4)
  popup.addSeparator()
  popup.add(item5)
  return popup
}

fun makeUI(): Component {
  if (!SystemTray.isSupported()) {
    throw UnsupportedOperationException("SystemTray is not supported")
  }
  val p = JPanel(BorderLayout())
  p.add(JScrollPane(JTextArea("TEST: JFrame")))
  p.preferredSize = Dimension(320, 240)

  dialog.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
  dialog.size = Dimension(120, 100)
  dialog.setLocationRelativeTo(null)
  dialog.title = "TEST: JDialog"

  val icon = TrayIcon(images[0], "TRAY", null)
  icon.popupMenu = makeTrayPopupMenu(icon, p)
  animator.addActionListener {
    icon.image = images[idx]
    idx = (idx + 1) % images.size
  }
  runCatching {
    SystemTray.getSystemTray().add(icon)
  }
  return p
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
      defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
