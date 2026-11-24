package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import javax.swing.*

fun createUI() = JPanel(GridLayout(0, 1, 0, 2)).also {
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
  val p1 = createCheckBoxPasswordPanel()
  it.add(createTitledPanel("BorderLayout + JCheckBox", p1))
  val p2 = createToggleButtonPasswordPanel()
  it.add(createTitledPanel("OverlayLayout + JToggleButton", p2))
  val p3 = createCardLayoutPasswordPanel()
  it.add(createTitledPanel("CardLayout + JTextField(can copy) + ...", p3))
  val p4 = createHoldToShowPasswordPanel()
  it.add(createTitledPanel("press and hold down the mouse button", p4))
  it.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
  it.preferredSize = Dimension(320, 240)
}

private fun createCheckBoxPasswordPanel(): JPanel {
  val password = createPasswordField()
  val button = JCheckBox("show passwords")
  button.addActionListener { e ->
    val b = (e.source as? AbstractButton)?.isSelected == true
    password.echoChar = if (b) 0.toChar() else getUIEchoChar()
  }
  val p = JPanel(BorderLayout())
  p.add(password)
  p.add(button, BorderLayout.SOUTH)
  return p
}

private fun createToggleButtonPasswordPanel(): JPanel {
  val password = createPasswordField()
  val button = EyeButtonUtils.createEyeToggleButton()
  button.addActionListener { e ->
    val b = (e.source as? AbstractButton)?.isSelected == true
    password.echoChar = if (b) 0.toChar() else getUIEchoChar()
  }
  val p = createOverlayPanel()
  p.add(button)
  p.add(password)
  return p
}

private fun createCardLayoutPasswordPanel(): JPanel {
  val password = createPasswordField()
  val field = JTextField(24)
  field.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
  field.enableInputMethods(false)
  field.document = password.document

  val visibilityLayout = CardLayout()
  val p = object : JPanel(visibilityLayout) {
    override fun updateUI() {
      super.updateUI()
      alignmentX = RIGHT_ALIGNMENT
    }
  }
  p.add(password, PasswordVisibility.HIDDEN.toString())
  p.add(field, PasswordVisibility.VISIBLE.toString())

  val button = EyeButtonUtils.createEyeToggleButton()
  button.addActionListener { e ->
    val b = (e.source as? AbstractButton)?.isSelected == true
    val s = if (b) PasswordVisibility.VISIBLE else PasswordVisibility.HIDDEN
    visibilityLayout.show(p, s.toString())
  }

  val panel = createOverlayPanel()
  panel.add(button)
  panel.add(p)
  return panel
}

private fun createHoldToShowPasswordPanel(): Container {
  val passwordField = createPasswordField()
  val button = EyeButtonUtils.createEyeButton()
  val mouseAdapter = object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      passwordField.echoChar = 0.toChar()
    }

    override fun mouseReleased(e: MouseEvent) {
      passwordField.echoChar = getUIEchoChar()
    }
  }
  button.addMouseListener(mouseAdapter)
  val panel = createOverlayPanel()
  panel.add(button)
  panel.add(passwordField)
  return panel
}

private fun getUIEchoChar() = UIManager.get("PasswordField.echoChar") as? Char ?: '*'

private fun createOverlayPanel() = object : JPanel() {
  override fun isOptimizedDrawingEnabled() = false
}.also {
  it.layout = OverlayLayout(it)
}

private fun createPasswordField() = object : JPasswordField(24) {
  override fun updateUI() {
    super.updateUI()
    // Once JPasswordField#setEchoChar(...) is called, the new LookAndFeel no longer
    // installs its own echo character, so reapply it while the password is hidden
    val echoChar = UIManager.get("PasswordField.echoChar")
    if (echoCharIsSet() && echoChar is Char) {
      setEchoChar(echoChar)
    }
  }
}.also {
  it.text = "1234567890"
  it.alignmentX = Component.RIGHT_ALIGNMENT
}

private fun createTitledPanel(
  title: String,
  cmp: Component,
): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  p.add(cmp, c)
  return p
}

private object EyeButtonUtils {
  fun createEyeToggleButton(): AbstractButton {
    val b = object : JToggleButton() {
      override fun updateUI() {
        super.updateUI()
        // WindowsToggleButtonUI replaces even a non-UIResource border with an XP border,
        // so reapply the empty border every time the LookAndFeel is changed
        border = BorderFactory.createEmptyBorder(0, 0, 0, 4)
      }
    }
    configureEyeButton(b)
    return b
  }

  fun createEyeButton(): AbstractButton {
    val b = object : JButton() {
      override fun updateUI() {
        super.updateUI()
        // WindowsButtonUI also replaces the border with an XP border
        border = BorderFactory.createEmptyBorder(0, 0, 0, 4)
      }
    }
    configureEyeButton(b)
    return b
  }

  private fun configureEyeButton(b: AbstractButton) {
    b.isFocusable = false
    // WindowsButtonUI enlarges an even preferred size to an odd one when the focus is painted
    b.isFocusPainted = false
    b.isOpaque = false
    b.isContentAreaFilled = false
    b.alignmentX = Component.RIGHT_ALIGNMENT
    b.alignmentY = Component.CENTER_ALIGNMENT
    b.icon = EyeIcon(Color.BLUE)
    b.rolloverIcon = EyeIcon(Color.DARK_GRAY)
    b.selectedIcon = EyeIcon(Color.BLUE)
    b.rolloverSelectedIcon = EyeIcon(Color.BLUE)
    b.toolTipText = "show/hide passwords"
  }
}

private enum class PasswordVisibility { VISIBLE, HIDDEN }

private class EyeIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.translate(x, y)
    g2.paint = color
    val iw = iconWidth
    val ih = iconHeight
    val s = iconWidth / 12.0
    g2.stroke = BasicStroke(s.toFloat())
    val w = iw - s * 2.0
    val h = ih - s * 2.0
    val r = w * 3.0 / 4.0 - s * 2.0
    val x0 = w / 2.0 - r + s
    val eye = Area(Ellipse2D.Double(x0, s * 4.0 - r, r * 2.0, r * 2.0))
    eye.intersect(Area(Ellipse2D.Double(x0, h - r - s * 2.0, r * 2.0, r * 2.0)))
    g2.draw(eye)
    val rr = iw / 6.0
    g2.draw(Ellipse2D.Double(iw / 2.0 - rr, ih / 2.0 - rr, rr * 2.0, rr * 2.0))
    if (c is AbstractButton) {
      val m = c.model
      if (m.isSelected || m.isPressed) {
        val l = Line2D.Double(iw / 6.0, ih * 5.0 / 6.0, iw * 5.0 / 6.0, ih / 6.0)
        val at = AffineTransform.getTranslateInstance(-s, 0.0)
        g2.paint = Color.WHITE
        g2.draw(at.createTransformedShape(l))
        g2.paint = color
        g2.draw(l)
      }
    }
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
