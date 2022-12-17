package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.JInternalFrame.JDesktopIcon

private val ICON_SIZE = Dimension(150, 40)

private val info = "JDesktopIcon: ${ICON_SIZE.width}x${ICON_SIZE.height}"
private val check = JCheckBox(info)
private var num = 0

fun makeUI(): Component {
  check.isOpaque = false
  val desktop = JDesktopPane()
  desktop.desktopManager = object : DefaultDesktopManager() {
    override fun getBoundsForIconOf(f: JInternalFrame): Rectangle {
      val r = super.getBoundsForIconOf(f)
      // println(r.size)
      return r
    }
  }

  val button = JButton("add")
  button.addActionListener {
    desktop.add(createFrame("#$num", num * 10, num * 10))
    num++
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  mb.add(button)
  mb.add(Box.createHorizontalGlue())
  mb.add(check)
  addIconifiedFrame(desktop, createFrame("Frame", 30, 10))
  addIconifiedFrame(desktop, createFrame("Frame", 50, 30))

  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.add(mb, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createFrame(t: String, x: Int, y: Int): JInternalFrame {
  val f = JInternalFrame(t, true, true, true, true)
  f.desktopIcon = object : JDesktopIcon(f) {
    override fun getPreferredSize() = when {
      !check.isSelected -> super.getPreferredSize()
      ui.javaClass.name.contains("MotifDesktopIconUI") -> Dimension(64, 64 + 32)
      else -> ICON_SIZE
    }
  }
  f.setSize(200, 100)
  f.setLocation(x, y)
  EventQueue.invokeLater { f.isVisible = true }
  return f
}

private fun addIconifiedFrame(desktop: JDesktopPane, f: JInternalFrame) {
  desktop.add(f)
  runCatching { f.isIcon = true }
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
      UIManager.put("DesktopIcon.width", ICON_SIZE.width)
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
