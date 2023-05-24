package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val check = JCheckBox("Hide the TaskBar button when JFrame is minimized")

private fun makePreferredSizeImage(icon: Icon, w: Int, h: Int): Image {
  val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = image.createGraphics()
  icon.paintIcon(null, g2, (w - icon.iconWidth) / 2, (h - icon.iconHeight) / 2)
  g2.dispose()
  return image
}

fun makeUI(): Component {
  EventQueue.invokeLater {
    (check.topLevelAncestor as? Frame)?.also { f ->
      f.addWindowStateListener { e ->
        if (check.isSelected && e.newState == Frame.ICONIFIED) {
          e.window.dispose()
        }
      }
    }
  }

  val item1 = MenuItem("OPEN")
  item1.addActionListener {
    (check.topLevelAncestor as? Frame)?.also { f ->
      f.extendedState = Frame.NORMAL
      f.isVisible = true
    }
  }

  val item2 = MenuItem("EXIT")
  item2.addActionListener {
    val tray = SystemTray.getSystemTray()
    for (icon in tray.trayIcons) {
      tray.remove(icon)
    }
    for (frame in Frame.getFrames()) {
      frame.dispose()
    }
  }

  val popup = PopupMenu()
  popup.add(item1)
  popup.add(item2)
  val d = SystemTray.getSystemTray().trayIconSize
  val image = makePreferredSizeImage(StarIcon(), d.width, d.height)
  runCatching {
    SystemTray.getSystemTray().add(TrayIcon(image, "TRAY", popup))
  }

  return JPanel().also {
    it.add(check)
    it.preferredSize = Dimension(320, 240)
  }
}

private class StarIcon(r1: Int = 8, r2: Int = 4, vc: Int = 5) : Icon {
  private val star: Shape

  init {
    val or = r1.coerceAtLeast(r2).toDouble()
    val ir = r1.coerceAtMost(r2).toDouble()
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
    star = Path2D.Double(p, at)
  }

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = Color.ORANGE
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
      iconImages = listOf(
        makePreferredSizeImage(StarIcon(), 16, 16),
        makePreferredSizeImage(StarIcon(16, 8, 5), 40, 40)
      )
      defaultCloseOperation = if (SystemTray.isSupported()) {
        WindowConstants.DISPOSE_ON_CLOSE
      } else {
        WindowConstants.EXIT_ON_CLOSE
      }
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
