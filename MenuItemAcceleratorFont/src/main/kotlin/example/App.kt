package example

import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.*

private val popup = JPopupMenu()

fun makeUI(): Component {
  val check = object : JCheckBox("change accelerator") {
    override fun updateUI() {
      super.updateUI()
      changeAccelerator(isSelected)
    }
  }
  check.addActionListener {
    changeAccelerator(check.isSelected)
    SwingUtilities.updateComponentTreeUI(check.rootPane)
  }

  val menu = LookAndFeelUtils.createLookAndFeelMenu()
  menu.setMnemonic('L')
  val sub = JMenu("JMenu(M)")
  sub.setMnemonic('M')
  val ks1 = KeyStroke.getKeyStroke(KeyEvent.VK_1, 0)
  val ks2 = KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK)
  val ks3 = KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.SHIFT_DOWN_MASK)
  val ks4 = KeyStroke.getKeyStroke(KeyEvent.VK_C, 0)
  val ks5 = KeyStroke.getKeyStroke(KeyEvent.VK_R, 0)
  sub.add("MenuItem1").setAccelerator(ks1)
  sub.add("MenuItem2").setAccelerator(ks2)
  sub.add("MenuItem3").setAccelerator(ks3)
  sub.add(JCheckBoxMenuItem("JCheckBoxMenuItem")).setAccelerator(ks4)
  sub.add(JRadioButtonMenuItem("JRadioButtonMenuItem")).setAccelerator(ks5)
  menu.add(sub)
  val mb = JMenuBar()
  mb.add(menu)
  mb.add(sub)

  val tree = JTree()
  tree.setComponentPopupMenu(popup)
  popup.add("MenuItem4").setAccelerator(ks1)
  popup.add("MenuItem5").setAccelerator(ks2)
  popup.add("MenuItem6").setAccelerator(ks3)
  popup.add(JCheckBoxMenuItem("JCheckBoxMenuItem")).setAccelerator(ks4)
  popup.add(JRadioButtonMenuItem("JRadioButtonMenuItem")).setAccelerator(ks5)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.setJMenuBar(mb) }
    it.add(JScrollPane(tree))
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun changeAccelerator(selected: Boolean) {
  var color1: Color
  var color2: Color?
  var font: Font?
  for (prefix in listOf("MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem")) {
    val key1 = "$prefix.acceleratorForeground"
    val key2 = "$prefix.acceleratorSelectionForeground"
    val key3 = "$prefix.acceleratorFont"
    if (selected) {
      color1 = Color(0xEC_64_64)
      color2 = Color.WHITE
      font = UIManager.getFont(key3).deriveFont(10f)
    } else {
      val def = UIManager.getLookAndFeelDefaults()
      color1 = def.getColor(key1)
      color2 = def.getColor(key2)
      font = def.getFont(key3)
    }
    UIManager.put(key1, color1)
    UIManager.put(key2, color2)
    UIManager.put(key3, font)
  }
  SwingUtilities.updateComponentTreeUI(popup)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
