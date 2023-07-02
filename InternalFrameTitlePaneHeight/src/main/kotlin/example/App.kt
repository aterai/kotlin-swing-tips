package example

import example.LookAndFeelUtils.createLookAndFeelMenu
import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val desktop = JDesktopPane()
  addFrame(desktop, 0)
  addFrame(desktop, 1)
  // InternalFrame.titleFont
  // InternalFrame.titleButtonHeight
  // InternalFrame.titleButtonWidth
  val key = "InternalFrame.titlePaneHeight"
  val height = UIManager.getLookAndFeelDefaults().getInt(key)
  val model = SpinnerNumberModel(height, 0, 50, 1)
  model.addChangeListener {
    val v = model.number.toInt()
    UIManager.put(key, v)
    SwingUtilities.updateComponentTreeUI(desktop)
  }
  val spinner = object : JSpinner(model) {
    override fun updateUI() {
      super.updateUI()
      val h = UIManager.getLookAndFeelDefaults().getInt(key)
      UIManager.put(key, h)
      model.value = h
      SwingUtilities.updateComponentTreeUI(desktop)
    }
  }

  val menuBar = JMenuBar()
  menuBar.add(LookAndFeelUtils.createLookAndFeelMenu())
  menuBar.add(spinner)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addFrame(desktop: JDesktopPane, idx: Int) {
  val frame = JInternalFrame("JInternalFrame", true, true, true, true)
  frame.add(makePanel())
  frame.setSize(240, 100)
  frame.setLocation(10 + 60 * idx, 5 + 105 * idx)
  desktop.add(frame)
  EventQueue.invokeLater { frame.isVisible = true }
}

private fun makePanel(): Component? {
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
    UnsupportedLookAndFeelException::class
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
