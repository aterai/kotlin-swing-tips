package example

import java.awt.*
import javax.swing.*

fun makeUI() = JPanel(BorderLayout()).also {
  EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
  it.add(JScrollPane(JTextArea()))
  it.preferredSize = Dimension(320, 240)
}

private fun createMenuBar(): JMenuBar {
  val menu = JMenu("File")
  menu.toolTipText = "File JMenu ToolTipText"
  menu.add("JMenuItem").toolTipText = "JMenuItem ToolTipText"
  val sub1 = JMenu("JMenu(Default)")
  sub1.toolTipText = "JMenu Default ToolTipText"
  sub1.add("JMenuItem1").toolTipText = "JMenuItem1 ToolTipText"
  sub1.add("JMenuItem2").toolTipText = "JMenuItem2 ToolTipText"
  menu.add(sub1)
  val sub2 = object : JMenu("JMenu#getToolTipText()") {
    override fun getToolTipText(): String? {
      val b = popupMenu.isVisible
      return if (b) null else super.getToolTipText()
    }
  }
  sub2.toolTipText = "JMenu ToolTipText"
  sub2.add("JMenuItem1").toolTipText = "JMenuItem1 ToolTipText"
  sub2.add("JMenuItem2").toolTipText = "JMenuItem2 ToolTipText"
  menu.add(sub2)
  val item2 = JCheckBoxMenuItem("JCheckBoxMenuItem", true)
  item2.toolTipText = "JCheckBoxMenuItem ToolTipText"
  menu.add(item2)
  val item3 = JRadioButtonMenuItem("JRadioButtonMenuItem", true)
  item3.toolTipText = "JRadioButtonMenuItem ToolTipText"
  menu.add(item3)
  val mb = JMenuBar()
  mb.toolTipText = "JMenuBar ToolTipText"
  mb.add(menu)
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return mb
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
    b.toolTipText = cmd
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
