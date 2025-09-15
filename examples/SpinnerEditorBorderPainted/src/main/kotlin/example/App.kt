package example

import java.awt.*
import javax.swing.*

private val info = JTextArea()
private val spinner = JSpinner()

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8))
  box.add(spinner)
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  mb.add(makeCheckBox())
  return JPanel(BorderLayout(5, 5)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(info))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeCheckBox(): Component {
  val key = "Spinner.editorBorderPainted"
  val check = object : JCheckBox(key) {
    override fun updateUI() {
      super.updateUI()
      val def = UIManager.getLookAndFeelDefaults()
      val b = def.getBoolean(key)
      val o = def[key]
      val lnf = UIManager.getLookAndFeel().javaClass.getName()
      val name = lnf.substring(lnf.lastIndexOf('.') + 1)
      info.append("%s: %s=%s%n".format(name, key, o?.let { b }))
      setSelected(b)
      UIManager.put(key, b)
      SwingUtilities.updateComponentTreeUI(spinner)
    }
  }
  check.addActionListener { e ->
    val src = e?.source as? JCheckBox
    UIManager.put(key, src?.isSelected == true)
    SwingUtilities.updateComponentTreeUI(spinner)
  }
  check.setOpaque(false)
  return check
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
