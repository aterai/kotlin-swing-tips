package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val tree = JTree()
  tree.componentPopupMenu = makePopupMenu()
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePopupMenu(): JPopupMenu {
  val popup = JPopupMenu()
  popup.add("↓ add(new JSeparator()")
  popup.add(JSeparator())
  popup.add("↓ JSeparator(): height = 8")
  popup.add(object : JSeparator() {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = 8
      return d
    }
  })
  popup.add("↓ addSeparator()")
  popup.addSeparator()
  popup.add("↓ JPopupMenu.Separator(): height = 4")
  popup.add(object : JPopupMenu.Separator() {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = 4
      return d
    }
  })
  popup.add("↓ JPopupMenu.Separator(): font size 16f")
  popup.add(object : JPopupMenu.Separator() {
    override fun getFont() = super.getFont().deriveFont(16f)
  })
  popup.add("↓ PopupMenuSeparator.contentMargins")
  popup.add(object : JPopupMenu.Separator() {
    override fun updateUI() {
      super.updateUI()
      val d = UIDefaults()
      d["PopupMenuSeparator.contentMargins"] = Insets(3, 0, 3, 0)
      putClientProperty("Nimbus.Overrides", d)
      putClientProperty("Nimbus.Overrides.InheritDefaults", true)
    }
  })
  popup.add(JCheckBoxMenuItem("JCheckBoxMenuItem"))
  return popup
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
