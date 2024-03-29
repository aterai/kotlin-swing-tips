package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val key = "InternalFrame.layoutTitlePaneAtOrigin"
  val desktop = JDesktopPane()
  val flg = UIManager.getBoolean(key)
  val check = object : JCheckBox("InternalFrame TitlePane layout", flg) {
    override fun updateUI() {
      super.updateUI()
      val b = UIManager.getLookAndFeelDefaults().getBoolean(key)
      isSelected = b
      UIManager.put(key, b)
      SwingUtilities.updateComponentTreeUI(desktop)
    }
  }
  check.addActionListener { e ->
    UIManager.put(key, (e.source as? JCheckBox)?.isSelected == true)
    SwingUtilities.updateComponentTreeUI(desktop)
  }
  check.isOpaque = false

  addFrame(desktop, 0, true)
  addFrame(desktop, 1, false)

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  mb.add(check)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addFrame(
  desktop: JDesktopPane,
  idx: Int,
  resizable: Boolean,
) {
  val f = JInternalFrame("resizable: $resizable", resizable, true, true, true)
  f.add(makePanel())
  f.setSize(240, 100)
  f.setLocation(10 + 60 * idx, 10 + 120 * idx)
  EventQueue.invokeLater { f.isVisible = true }
  desktop.add(f)
}

private fun makePanel(): Component {
  val p = JPanel()
  p.add(JLabel("label"))
  p.add(JButton("button"))
  return p
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
