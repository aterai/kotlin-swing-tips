package example

import java.awt.*
import java.util.Locale
import javax.swing.*

fun makeUI() = JPanel(BorderLayout()).also {
  val box = Box.createVerticalBox()
  box.add(mkSysColorCell(SystemColor.desktop, "desktop"))
  box.add(mkSysColorCell(SystemColor.activeCaption, "activeCaption"))
  box.add(mkSysColorCell(SystemColor.inactiveCaption, "inactiveCaption"))
  box.add(mkSysColorCell(SystemColor.activeCaptionText, "activeCaptionText"))
  box.add(mkSysColorCell(SystemColor.inactiveCaptionText, "inactiveCaptionText"))
  box.add(mkSysColorCell(SystemColor.activeCaptionBorder, "activeCaptionBorder"))
  box.add(mkSysColorCell(SystemColor.inactiveCaptionBorder, "inactiveCaptionBorder"))
  box.add(mkSysColorCell(SystemColor.window, "window"))
  box.add(mkSysColorCell(SystemColor.windowText, "windowText"))
  box.add(mkSysColorCell(SystemColor.menu, "menu"))
  box.add(mkSysColorCell(SystemColor.menuText, "menuText"))
  box.add(mkSysColorCell(SystemColor.text, "text"))
  box.add(mkSysColorCell(SystemColor.textHighlight, "textHighlight"))
  box.add(mkSysColorCell(SystemColor.textText, "textText"))
  box.add(mkSysColorCell(SystemColor.textHighlightText, "textHighlightText"))
  box.add(mkSysColorCell(SystemColor.control, "control"))
  box.add(mkSysColorCell(SystemColor.controlLtHighlight, "controlLtHighlight"))
  box.add(mkSysColorCell(SystemColor.controlHighlight, "controlHighlight"))
  box.add(mkSysColorCell(SystemColor.controlShadow, "controlShadow"))
  box.add(mkSysColorCell(SystemColor.controlDkShadow, "controlDkShadow"))
  box.add(mkSysColorCell(SystemColor.controlText, "controlText"))
  box.add(mkSysColorCell(SystemColor.control, "control"))
  box.add(mkSysColorCell(SystemColor.scrollbar, "scrollbar"))
  box.add(mkSysColorCell(SystemColor.info, "info"))
  box.add(mkSysColorCell(SystemColor.infoText, "infoText"))
  it.add(JScrollPane(box))
  it.preferredSize = Dimension(320, 240)
}

private fun mkSysColorCell(color: Color, txt: String) : JPanel {
  val hex = Integer.toHexString(color.rgb).uppercase(Locale.ENGLISH)
  val field = JTextField("$txt: 0x$hex")
  field.isEditable = false
  val p = JPanel(BorderLayout())
  p.add(JLabel(ColorIcon(color)), BorderLayout.EAST)
  p.add(field)
  return p
}

private class ColorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
