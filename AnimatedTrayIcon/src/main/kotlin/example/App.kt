package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer

private val dialog = JDialog()
private val animator = Timer(100, null)
private var idx = 0

private fun makeTrayIcon(p: JComponent): TrayIcon {
  val images = arrayOf(
    makeImage("example/16x16.png"),
    makeImage("example/16x16l.png"),
    makeImage("example/16x16.png"),
    makeImage("example/16x16r.png")
  )

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
    SystemTray.getSystemTray().trayIcons.forEach { it.image = images[0] }
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

  val icon = TrayIcon(images[0], "TRAY", popup)
  animator.addActionListener {
    icon.image = images[idx]
    idx = (idx + 1) % images.size
  }
  return icon
}

private fun makeImage(path: String): Image {
  val cl = Thread.currentThread().contextClassLoader
  return cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeDefaultTrayImage()
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

  val icon = makeTrayIcon(p)
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
