package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val splitPane = JSplitPane()
  splitPane.topComponent = JScrollPane(JTable(8, 3))
  splitPane.bottomComponent = JScrollPane(JTree())
  splitPane.isOneTouchExpandable = true
  splitPane.dividerSize = 32

  val key = "SplitPane.centerOneTouchButtons"
  val check = object : JCheckBox(key, UIManager.getBoolean(key)) {
    override fun updateUI() {
      super.updateUI()
      EventQueue.invokeLater {
        val b = UIManager.getLookAndFeelDefaults().getBoolean(key)
        isSelected = b
        updateCenterOneTouchButtons(splitPane, b)
      }
    }
  }
  check.isOpaque = false
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    updateCenterOneTouchButtons(splitPane, b)
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  mb.add(Box.createHorizontalStrut(2))
  mb.add(check)

  return JPanel(BorderLayout(5, 5)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(splitPane)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

fun updateCenterOneTouchButtons(splitPane: JSplitPane?, b: Boolean) {
  UIManager.put("SplitPane.centerOneTouchButtons", b)
  SwingUtilities.updateComponentTreeUI(splitPane)
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
