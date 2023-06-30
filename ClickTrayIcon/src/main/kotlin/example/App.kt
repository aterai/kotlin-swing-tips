package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val TEXT = """
  icon.addMouseListener(new MouseAdapter() {
    public void mouseClicked(MouseEvent e) {
      boolean isDoubleClick = e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2;
      if (isDoubleClick) {
        frame.setVisible(true);
      } else if (frame.isVisible()) {
        frame.setExtendedState(Frame.NORMAL);
        frame.toFront();
      }
    }
  });
""".trimIndent()

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(JScrollPane(JTextArea(TEXT)))
  it.preferredSize = Dimension(320, 240)
}

private fun makeTrayIcon(frame: JFrame): TrayIcon {
  val open = MenuItem("Option")
  open.addActionListener { frame.isVisible = true }
  val exit = MenuItem("Exit")
  exit.addActionListener {
    val tray = SystemTray.getSystemTray()
    for (icon in tray.trayIcons) {
      tray.remove(icon)
    }
    // frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    // frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
    frame.dispose()
  }
  val popup = PopupMenu()
  popup.add(open)
  popup.add(exit)

  val d = SystemTray.getSystemTray().trayIconSize
  val image = makePreferredSizeImage(StarIcon(), d.width, d.height)
  val icon = TrayIcon(image, "Click Test", popup)
  val ml = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      val isDoubleClick = e.button == MouseEvent.BUTTON1 && e.clickCount >= 2
      if (isDoubleClick) {
        frame.isVisible = true
      } else if (frame.isVisible) {
        frame.extendedState = Frame.NORMAL
        frame.toFront()
      }
    }
  }
  icon.addMouseListener(ml)
  return icon
}

private fun makePreferredSizeImage(icon: Icon, w: Int, h: Int): Image {
  val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = image.createGraphics()
  icon.paintIcon(null, g2, (w - icon.iconWidth) / 2, (h - icon.iconHeight) / 2)
  g2.dispose()
  return image
}

private class StarIcon : Icon {
  private val star = makeStar()
  private fun makeStar(): Path2D {
    val or = 8.0
    val ir = 4.0
    val vc = 5
    var agl = 0.0
    val add = PI / vc
    val p: Path2D = Path2D.Double()
    p.moveTo(or, 0.0)
    for (i in 0 until vc * 2 - 1) {
      agl += add
      val r = if (i % 2 == 0) ir else or
      p.lineTo(r * cos(agl), r * sin(agl))
    }
    p.closePath()
    val at = AffineTransform.getRotateInstance(-PI / 2.0, or, 0.0)
    return Path2D.Double(p, at)
  }

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = Color.PINK
    g2.fill(star)
    g2.dispose()
  }

  override fun getIconWidth() = star.bounds.width

  override fun getIconHeight() = star.bounds.height
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
      if (SystemTray.isSupported()) {
        defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
        kotlin.runCatching {
          SystemTray.getSystemTray().add(makeTrayIcon(this))
        }
      }
    }
  }
}
