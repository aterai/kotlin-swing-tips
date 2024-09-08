package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  UIManager.put("CheckBox.foreground", Color.RED)
  UIManager.put("RadioButton.foreground", Color.RED)
  UIManager.put("CheckBox.background", Color.GREEN)
  UIManager.put("RadioButton.background", Color.GREEN)
  UIManager.put("CheckBox.interiorBackground", Color.BLUE)
  UIManager.put("RadioButton.interiorBackground", Color.BLUE)

  val check1 = JCheckBox("JCheckBox1", true)
  val check2 = JCheckBox("JCheckBox2")
  val check3 = JCheckBox("JCheckBox3")
  val box1 = Box.createHorizontalBox()
  box1.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  listOf(check1, check2, check3).forEach {
    it.isOpaque = false
    box1.add(it)
    box1.add(Box.createVerticalStrut(5))
  }

  val radio1 = JRadioButton("JRadioButton1", true)
  val radio2 = JRadioButton("JRadioButton2")
  val radio3 = JRadioButton("JRadioButton3")
  val group = ButtonGroup()
  val box2 = Box.createHorizontalBox()
  box2.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  listOf(radio1, radio2, radio3).forEach {
    it.isOpaque = false
    group.add(it)
    box2.add(it)
    box2.add(Box.createVerticalStrut(5))
  }

  val p = JPanel(GridLayout(2, 1, 5, 5))
  p.add(box1)
  p.add(box2)

  val info = JTextArea()
  info.append("CheckBox.foreground, Color.RED\n")
  info.append("CheckBox.background, Color.GREEN\n")
  info.append("CheckBox.interiorBackground, Color.BLUE\n")
  info.append("RadioButton.foreground, Color.RED\n")
  info.append("RadioButton.background, Color.GREEN\n")
  info.append("RadioButton.interiorBackground, Color.BLUE\n")

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(info))
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.preferredSize = Dimension(320, 240)
  }
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
  val classic = "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel"
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(classic)
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
