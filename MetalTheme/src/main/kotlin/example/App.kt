package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.metal.MetalLookAndFeel
import javax.swing.plaf.metal.OceanTheme
import javax.swing.plaf.synth.SynthInternalFrameUI

fun makeUI(): Component {
  val desktop = JDesktopPane()
  addFrame(desktop, 0)
  addFrame(desktop, 1)

  val menuBar = JMenuBar()
  menuBar.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater {
      it.rootPane.jMenuBar = menuBar
    }
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addFrame(desktop: JDesktopPane, idx: Int) {
  val frame = object : JInternalFrame("JInternalFrame", true, true, true, true) {
    override fun updateUI() {
      super.updateUI()
      val currentUI = ui
      if (currentUI is SynthInternalFrameUI) {
        val d = UIDefaults()
        d["InternalFrame:InternalFrameTitlePane[Enabled].textForeground"] = Color.GREEN
        currentUI.northPane.putClientProperty("Nimbus.Overrides", d)
      }
    }
  }
  frame.add(makePanel())
  frame.setSize(240, 100)
  frame.setLocation(10 + 60 * idx, 5 + 105 * idx)
  desktop.add(frame)
  EventQueue.invokeLater { frame.isVisible = true }
  // desktop.desktopManager.activateFrame(frame)
}

private fun makePanel() = JPanel().also {
  it.add(JLabel("label"))
  it.add(JButton("button"))
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
    // BasicLookAndFeel, WidowsLookAndFeel
    UIManager.put("InternalFrame.activeTitleForeground", Color.RED)
    UIManager.put("InternalFrame.inactiveTitleForeground", Color.WHITE)
    // MetalLookAndFeel
    val theme = object : OceanTheme() {
      override fun getWindowTitleForeground() = ColorUIResource(Color.RED.brighter())

      override fun getWindowTitleInactiveForeground() = ColorUIResource(Color.ORANGE.darker())
    }
    MetalLookAndFeel.setCurrentTheme(theme)
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
