package example

import java.awt.*
import javax.swing.*

private const val PRE = "RadioButtonMenuItem."

fun makeUI(): Component {
  val log = JTextArea()
  info(log)
  val popup = makePopup()
  log.componentPopupMenu = popup
  return JPanel(BorderLayout()).also {
    it.add(log)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun info(log: JTextArea) {
  val key = PRE + "margin"
  log.append("%s: %s%n".format(key, UIManager.getInsets(key)))
  log.append(infoInt(PRE + "minimumTextOffset"))
  log.append(infoInt(PRE + "afterCheckIconGap"))
  log.append(infoInt(PRE + "checkIconOffset"))
  val icon = getCheckIcon()
  log.append("%scheckIcon: %s%n".format(PRE, icon))
  if (icon != null) {
    val w = icon.iconWidth
    val h = icon.iconHeight
    log.append("  checkIcon size -> (%dx%d)%n".format(w, h))
  }
}

private fun infoInt(key: String) = "%s: %d%n".format(key, UIManager.getInt(key))

private fun makePopup(): JPopupMenu {
  UIManager.put(PRE + "minimumTextOffset", 10)
  UIManager.put(PRE + "afterCheckIconGap", 0)
  UIManager.put(PRE + "checkIconOffset", 0)
  val checkIcon = getCheckIcon()
  val height = checkIcon?.iconHeight ?: 22
  UIManager.put(PRE + "checkIcon", EmptyIcon())
  val d = Dimension(100, height)
  val popup = JPopupMenu()
  val bg = ButtonGroup()
  listOf(
    makeMenuItem("0.5 pt", .5f, d),
    makeMenuItem("0.75 pt", .75f, d),
    makeMenuItem("1 pt", 1f, d),
    makeMenuItem("1.5 pt", 1.5f, d),
    makeMenuItem("2.25 pt", 2.25f, d),
    makeMenuItem("3 pt", 3f, d)
  ).forEach {
    popup.add(it)
    bg.add(it)
  }
  return popup
}

private fun getCheckIcon() = UIManager.getIcon(PRE + "checkIcon")

private fun makeMenuItem(txt: String, width: Float, d: Dimension): JMenuItem {
  val px = width * Toolkit.getDefaultToolkit().screenResolution / 72f
  return object : JRadioButtonMenuItem(txt, LineIcon(BasicStroke(px), d)) {
    override fun init(text: String, icon: Icon) {
      super.init(text, icon)
      horizontalTextPosition = LEADING
      horizontalAlignment = TRAILING
    }

    override fun paintComponent(g: Graphics) {
      if (isSelected) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = Color(0xAA_64_AA_FF.toInt(), true)
        g2.fillRect(0, 0, getWidth(), height)
        g2.dispose()
      }
      super.paintComponent(g)
    }
  }
}

private class LineIcon(private val stroke: Stroke, private val size: Dimension) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.color = Color.BLACK
    g2.stroke = stroke
    val yy = y + iconHeight / 2
    g2.drawLine(x + 5, yy, x + iconWidth - 5, yy)
    g2.dispose()
  }

  override fun getIconWidth() = size.width

  override fun getIconHeight() = size.height
}

private class EmptyIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    // empty
  }

  override fun getIconWidth() = 0

  override fun getIconHeight() = 0
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
