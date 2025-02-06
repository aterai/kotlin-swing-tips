package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val s1 = JScrollPane(JTable(8, 3))
  val s2 = JScrollPane(JTree())
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, s1, s2)
  split.resizeWeight = .5
  split.isOneTouchExpandable = true
  split.dividerSize = 32

  val key = "SplitPane.centerOneTouchButtons"
  val check = object : JCheckBox(key, UIManager.getBoolean(key)) {
    override fun updateUI() {
      super.updateUI()
      EventQueue.invokeLater {
        val b = UIManager.getLookAndFeelDefaults().getBoolean(key)
        isSelected = b
        updateCenterOneTouchButtons(split, b)
      }
    }
  }
  check.isOpaque = false
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    updateCenterOneTouchButtons(split, b)
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  mb.add(Box.createHorizontalStrut(2))
  mb.add(check)

  return JPanel(BorderLayout(5, 5)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(split)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

fun updateCenterOneTouchButtons(
  splitPane: JSplitPane?,
  b: Boolean,
) {
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
