package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(GridLayout(4, 1, 0, 2)) {
  init {
    val pf1 = makePasswordField()
    val b1 = JCheckBox("show passwords")
    b1.addActionListener { e ->
      val c = e.getSource() as? AbstractButton ?: return@addActionListener
      pf1.setEchoChar(if (c.isSelected()) '\u0000' else getUIEchoChar())
    }
    val p1 = JPanel(BorderLayout())
    p1.add(pf1)
    p1.add(b1, BorderLayout.SOUTH)
    add(makeTitledPanel("BorderLayout + JCheckBox", p1))
    val pf2 = makePasswordField()
    val b2 = JToggleButton()
    b2.addActionListener { e ->
      val c = e.getSource() as? AbstractButton ?: return@addActionListener
      pf2.setEchoChar(if (c.isSelected()) '\u0000' else getUIEchoChar())
    }
    initEyeButton(b2)
    val p2 = makeOverlayLayoutPanel()
    p2.add(b2)
    p2.add(pf2)
    add(makeTitledPanel("OverlayLayout + JToggleButton", p2))
    val pf3 = makePasswordField()
    val tf3 = JTextField(24)
    tf3.setFont(Font(Font.MONOSPACED, Font.PLAIN, 12))
    tf3.enableInputMethods(false)
    tf3.setDocument(pf3.getDocument())
    val cardLayout = CardLayout()
    val p3 = object : JPanel(cardLayout) {
      override fun updateUI() {
        super.updateUI()
        setAlignmentX(Component.RIGHT_ALIGNMENT)
      }
    }
    p3.add(pf3, PasswordField.HIDE.toString())
    p3.add(tf3, PasswordField.SHOW.toString())
    val b3 = JToggleButton()
    b3.addActionListener { e ->
      val c = e.getSource() as? AbstractButton ?: return@addActionListener
      val s = if (c.isSelected()) PasswordField.SHOW else PasswordField.HIDE
      cardLayout.show(p3, s.toString())
    }
    initEyeButton(b3)
    val pp3 = makeOverlayLayoutPanel()
    pp3.add(b3)
    pp3.add(p3)
    add(makeTitledPanel("CardLayout + JTextField(can copy) + ...", pp3))
    val pf4 = makePasswordField()
    val b4 = JButton()
    b4.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        pf4.setEchoChar('\u0000')
      }

      override fun mouseReleased(e: MouseEvent) {
        pf4.setEchoChar(getUIEchoChar())
      }
    })
    initEyeButton(b4)
    val p4 = makeOverlayLayoutPanel()
    p4.add(b4)
    p4.add(pf4)
    add(makeTitledPanel("press and hold down the mouse button", p4))
    setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun getUIEchoChar() = UIManager.get("PasswordField.echoChar") as? Char ?: '*'

  private fun initEyeButton(b: AbstractButton) {
    b.setFocusable(false)
    b.setOpaque(false)
    b.setContentAreaFilled(false)
    b.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4))
    b.setAlignmentX(Component.RIGHT_ALIGNMENT)
    b.setAlignmentY(Component.CENTER_ALIGNMENT)
    b.setIcon(ColorIcon(Color.GREEN))
    b.setRolloverIcon(ColorIcon(Color.BLUE))
    b.setSelectedIcon(ColorIcon(Color.RED))
    b.setRolloverSelectedIcon(ColorIcon(Color.ORANGE))
    b.setToolTipText("show/hide passwords")
  }

  private fun makeOverlayLayoutPanel() = object : JPanel() {
    override fun isOptimizedDrawingEnabled() = false
  }.also {
    it.setLayout(OverlayLayout(it))
  }

  private fun makePasswordField() = JPasswordField(24).also {
    it.setText("1234567890")
    it.setAlignmentX(Component.RIGHT_ALIGNMENT)
  }

  private fun makeTitledPanel(title: String, cmp: Component): Component {
    val p = JPanel(GridBagLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    val c = GridBagConstraints()
    c.weightx = 1.0
    c.fill = GridBagConstraints.HORIZONTAL
    c.insets = Insets(5, 5, 5, 5)
    p.add(cmp, c)
    return p
  }
}

enum class PasswordField { SHOW, HIDE }

class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.setPaint(color)
    g2.fillRect(1, 1, getIconWidth() - 2, getIconHeight() - 2)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
