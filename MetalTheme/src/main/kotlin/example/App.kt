package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.metal.MetalLookAndFeel
import javax.swing.plaf.metal.OceanTheme
import javax.swing.plaf.synth.SynthInternalFrameUI

fun makeUI(): Component {
  val desktop = JDesktopPane()
  addFrame(desktop, 0)
  addFrame(desktop, 1)

  val menuBar = JMenuBar()
  menuBar.add(LookAndFeelUtil.createLookAndFeelMenu())

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

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.actionCommand = lafClassName
    lafItem.hideActionText = true
    lafItem.addActionListener { e ->
      val m = lafGroup.selection
      runCatching {
        setLookAndFeel(m.actionCommand)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
      }
    }
    lafGroup.add(lafItem)
    return lafItem
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class
  )
  private fun setLookAndFeel(lookAndFeel: String) {
    val oldLookAndFeel = LookAndFeelUtil.lookAndFeel
    if (oldLookAndFeel != lookAndFeel) {
      UIManager.setLookAndFeel(lookAndFeel)
      LookAndFeelUtil.lookAndFeel = lookAndFeel
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
