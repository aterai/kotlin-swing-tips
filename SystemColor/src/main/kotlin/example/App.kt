package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  val box = Box.createVerticalBox()
  box.add(makeSystemColorPanel(SystemColor.desktop, "desktop"))
  box.add(makeSystemColorPanel(SystemColor.activeCaption, "activeCaption"))
  box.add(makeSystemColorPanel(SystemColor.inactiveCaption, "inactiveCaption"))
  box.add(makeSystemColorPanel(SystemColor.activeCaptionText, "activeCaptionText"))
  box.add(makeSystemColorPanel(SystemColor.inactiveCaptionText, "inactiveCaptionText"))
  box.add(makeSystemColorPanel(SystemColor.activeCaptionBorder, "activeCaptionBorder"))
  box.add(makeSystemColorPanel(SystemColor.inactiveCaptionBorder, "inactiveCaptionBorder"))
  box.add(makeSystemColorPanel(SystemColor.window, "window"))
  box.add(makeSystemColorPanel(SystemColor.windowText, "windowText"))
  box.add(makeSystemColorPanel(SystemColor.menu, "menu"))
  box.add(makeSystemColorPanel(SystemColor.menuText, "menuText"))
  box.add(makeSystemColorPanel(SystemColor.text, "text"))
  box.add(makeSystemColorPanel(SystemColor.textHighlight, "textHighlight"))
  box.add(makeSystemColorPanel(SystemColor.textText, "textText"))
  box.add(makeSystemColorPanel(SystemColor.textHighlightText, "textHighlightText"))
  box.add(makeSystemColorPanel(SystemColor.control, "control"))
  box.add(makeSystemColorPanel(SystemColor.controlLtHighlight, "controlLtHighlight"))
  box.add(makeSystemColorPanel(SystemColor.controlHighlight, "controlHighlight"))
  box.add(makeSystemColorPanel(SystemColor.controlShadow, "controlShadow"))
  box.add(makeSystemColorPanel(SystemColor.controlDkShadow, "controlDkShadow"))
  box.add(makeSystemColorPanel(SystemColor.controlText, "controlText"))
  box.add(makeSystemColorPanel(SystemColor.control, "control"))
  box.add(makeSystemColorPanel(SystemColor.scrollbar, "scrollbar"))
  box.add(makeSystemColorPanel(SystemColor.info, "info"))
  box.add(makeSystemColorPanel(SystemColor.infoText, "infoText"))

  it.add(JScrollPane(box))
  it.preferredSize = Dimension(320, 240)
}

private fun makeSystemColorPanel(color: Color, text: String) = JPanel(BorderLayout()).also {
  val hex = Integer.toHexString(color.rgb).uppercase(Locale.ENGLISH)
  val field = JTextField("$text: 0x$hex")
  field.isEditable = false
  it.add(JLabel(ColorIcon(color)), BorderLayout.EAST)
  it.add(field)
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
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
