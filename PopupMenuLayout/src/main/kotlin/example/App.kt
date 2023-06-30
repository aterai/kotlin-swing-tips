package example

import java.awt.*
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import javax.swing.*
import kotlin.math.roundToInt

fun makeUI() = JPanel(BorderLayout()).also {
  val textArea = JTextArea("★ ☆ ⤾ ⤿ ⥀ ⥁ ⇐ ⇒ ⇦ ⇨ ↺ ↻ ↶ ↷")
  textArea.componentPopupMenu = makePopup()
  it.add(JScrollPane(textArea))
  it.preferredSize = Dimension(320, 240)
}

private fun makePopup(): JPopupMenu {
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.weighty = 0.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.anchor = GridBagConstraints.CENTER
  c.gridy = 0

  val popup = JPopupMenu()
  popup.layout = GridBagLayout()
  popup.add(makeButton("⇦"), c)
  popup.add(makeButton("⇨"), c)
  popup.add(makeButton("↻"), c)
  popup.add(makeButton("✩"), c)

  c.insets = Insets(2, 0, 2, 0)
  c.gridwidth = 4
  c.gridx = 0
  c.gridy = GridBagConstraints.RELATIVE
  popup.add(JSeparator(), c)

  c.insets = Insets(0, 0, 0, 0)
  popup.add(JMenuItem("000000000"), c)
  popup.add(JPopupMenu.Separator(), c)
  popup.add(JMenuItem("1111"), c)
  popup.add(JMenuItem("222222222222"), c)
  popup.add(JMenuItem("333333333"), c)

  return popup
}

private fun makeButton(symbol: String): AbstractButton {
  val icon = SymbolIcon(symbol)
  val b = object : JMenuItem() {
    private val dim = Dimension(icon.iconWidth, icon.iconHeight)
    override fun getPreferredSize() = dim

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val cd = size
      val pd = preferredSize
      val ix = (.5 * (cd.width - pd.width)).roundToInt()
      val iy = (.5 * (cd.height - pd.height)).roundToInt()
      icon.paintIcon(this, g, ix, iy)
    }
  }
  b.isOpaque = true
  if ("\u21E8" == symbol) {
    b.isEnabled = false
    b.toolTipText = "forward"
  }
  return b
}

private class SymbolIcon(private val str: String) : Icon {
  private val font = Font(Font.MONOSPACED, Font.BOLD, ICON_SIZE)

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.translate(x, y)
    g2.paint = if (c.isEnabled) Color.BLACK else Color.GRAY
    val frc = g2.fontRenderContext
    val symbol = TextLayout(str, font, frc).getOutline(null)
    val b = symbol.bounds2D
    val cx = iconWidth / 2.0 - b.centerX
    val cy = iconHeight / 2.0 - b.centerY
    val toCenterAtf = AffineTransform.getTranslateInstance(cx, cy)
    g2.fill(toCenterAtf.createTransformedShape(symbol))
    g2.dispose()
  }

  override fun getIconWidth() = ICON_SIZE

  override fun getIconHeight() = ICON_SIZE

  companion object {
    private const val ICON_SIZE = 32
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
