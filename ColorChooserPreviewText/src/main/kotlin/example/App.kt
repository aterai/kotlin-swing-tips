package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val key = "ColorChooser.showPreviewPanelText"
  val log = JTextArea()
  val check = object : JCheckBox(key, getBoolean(UIManager.get(key))) {
    override fun updateUI() {
      super.updateUI()
      val b = getBoolean(UIManager.getLookAndFeelDefaults().get(key))
      isSelected = b
      val laf = UIManager.getLookAndFeel()
      log.append("%s%n  %s: %s%n".format(laf, key, b))
      UIManager.put(key, b)
    }
  }

  val button = JButton("JColorChooser.showDialog(...)")
  button.addActionListener {
    UIManager.put(key, check.isSelected)
    val color = JColorChooser.showDialog(log.getRootPane(), "JColorChooser", null)
    log.append("color: %s%n".format(color))
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { log.getRootPane().setJMenuBar(mb) }

  val p = JPanel(GridLayout(2, 1, 10, 10))
  p.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  p.add(button)
  p.add(check)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

fun getBoolean(o: Any?) = when {
  o is Boolean -> o
  o == null -> true
  else -> false
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
