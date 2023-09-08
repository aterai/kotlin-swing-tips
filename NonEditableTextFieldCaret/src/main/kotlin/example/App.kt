package example

import java.awt.*
import java.awt.event.FocusEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.text.DefaultCaret

fun makeUI(): Component {
  val field1 = makeTextField("setEnabled(false)")
  field1.isEnabled = false
  val field2 = makeTextField("setEditable(false)")
  field2.isEditable = false
  val field3 = makeTextField("DefaultCaret#setVisible(true)")
  field3.isEditable = false
  field3.caret = object : DefaultCaret() {
    override fun focusGained(e: FocusEvent) {
      super.focusGained(e)
      if (component.isEnabled) {
        isVisible = true
      }
    }
  }
  val field4 = makeTextField("DefaultCaret#setBlinkRate(...)")
  field4.isEditable = false
  field4.caret = object : DefaultCaret() {
    override fun focusGained(e: FocusEvent) {
      super.focusGained(e)
      if (component.isEnabled) {
        blinkRate = UIManager.getInt("TextField.caretBlinkRate")
        isVisible = true
      }
    }
  }
  val c = GridBagConstraints()
  c.insets = Insets(8, 4, 8, 4)
  c.anchor = GridBagConstraints.WEST
  val p = JPanel(GridBagLayout())
  c.gridy = 0
  p.add(JLabel("Default: "), c)
  c.gridy = 1
  p.add(JLabel("setEnabled: "), c)
  c.gridy = 2
  p.add(JLabel("setEditable: "), c)
  c.gridy = 3
  p.add(JLabel("setVisible: "), c)
  c.gridy = 4
  p.add(JLabel("setBlinkRate: "), c)
  c.gridx = 1
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.gridy = 0
  p.add(makeTextField("Default JTextField"), c)
  c.gridy = 1
  p.add(field1, c)
  c.gridy = 2
  p.add(field2, c)
  c.gridy = 3
  p.add(field3, c)
  c.gridy = 4
  p.add(field4, c)
  return JPanel(BorderLayout(5, 5)).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTextField(txt: String): JTextField {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  val now = formatter.format(LocalDateTime.now(ZoneId.systemDefault()))
  val field = JTextField("$now $txt")
  field.isOpaque = false
  field.border = BorderFactory.createEmptyBorder()
  field.background = Color(0x0, true) // Nimbus?
  return field
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

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
