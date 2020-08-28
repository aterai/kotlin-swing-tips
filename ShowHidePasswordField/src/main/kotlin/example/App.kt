package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val pf1 = makePasswordField()
  val b1 = JCheckBox("show passwords")
  b1.addActionListener { e ->
    val b = (e.source as? AbstractButton)?.isSelected == true
    pf1.echoChar = if (b) '\u0000' else getUIEchoChar()
  }
  val p1 = JPanel(BorderLayout())
  p1.add(pf1)
  p1.add(b1, BorderLayout.SOUTH)

  val pf2 = makePasswordField()
  val b2 = JToggleButton()
  b2.addActionListener { e ->
    val b = (e.source as? AbstractButton)?.isSelected == true
    pf2.echoChar = if (b) '\u0000' else getUIEchoChar()
  }
  initEyeButton(b2)
  val p2 = makeOverlayLayoutPanel()
  p2.add(b2)
  p2.add(pf2)

  val pf3 = makePasswordField()
  val tf3 = JTextField(24)
  tf3.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
  tf3.enableInputMethods(false)
  tf3.document = pf3.document
  val cardLayout = CardLayout()
  val p3 = object : JPanel(cardLayout) {
    override fun updateUI() {
      super.updateUI()
      alignmentX = Component.RIGHT_ALIGNMENT
    }
  }
  p3.add(pf3, PasswordField.HIDE.toString())
  p3.add(tf3, PasswordField.SHOW.toString())
  val b3 = JToggleButton()
  b3.addActionListener { e ->
    val b = (e.source as? AbstractButton)?.isSelected == true
    val s = if (b) PasswordField.SHOW else PasswordField.HIDE
    cardLayout.show(p3, s.toString())
  }
  initEyeButton(b3)
  val pp3 = makeOverlayLayoutPanel()
  pp3.add(b3)
  pp3.add(p3)

  val pf4 = makePasswordField()
  val b4 = JButton()
  val ml4 = object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      pf4.echoChar = '\u0000'
    }

    override fun mouseReleased(e: MouseEvent) {
      pf4.echoChar = getUIEchoChar()
    }
  }
  b4.addMouseListener(ml4)
  initEyeButton(b4)
  val p4 = makeOverlayLayoutPanel()
  p4.add(b4)
  p4.add(pf4)

  return JPanel(GridLayout(4, 1, 0, 2)).also {
    it.add(makeTitledPanel("BorderLayout + JCheckBox", p1))
    it.add(makeTitledPanel("OverlayLayout + JToggleButton", p2))
    it.add(makeTitledPanel("CardLayout + JTextField(can copy) + ...", pp3))
    it.add(makeTitledPanel("press and hold down the mouse button", p4))
    it.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getUIEchoChar() = UIManager.get("PasswordField.echoChar") as? Char ?: '*'

private fun initEyeButton(b: AbstractButton) {
  b.isFocusable = false
  b.isOpaque = false
  b.isContentAreaFilled = false
  b.border = BorderFactory.createEmptyBorder(0, 0, 0, 4)
  b.alignmentX = Component.RIGHT_ALIGNMENT
  b.alignmentY = Component.CENTER_ALIGNMENT
  b.icon = ColorIcon(Color.GREEN)
  b.rolloverIcon = ColorIcon(Color.BLUE)
  b.selectedIcon = ColorIcon(Color.RED)
  b.rolloverSelectedIcon = ColorIcon(Color.ORANGE)
  b.toolTipText = "show/hide passwords"
}

private fun makeOverlayLayoutPanel() = object : JPanel() {
  override fun isOptimizedDrawingEnabled() = false
}.also {
  it.layout = OverlayLayout(it)
}

private fun makePasswordField() = JPasswordField(24).also {
  it.text = "1234567890"
  it.alignmentX = Component.RIGHT_ALIGNMENT
}

private fun makeTitledPanel(title: String, cmp: Component): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  p.add(cmp, c)
  return p
}

private enum class PasswordField { SHOW, HIDE }

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
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
