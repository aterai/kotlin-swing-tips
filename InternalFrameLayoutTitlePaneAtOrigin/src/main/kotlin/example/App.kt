package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

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
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())
  mb.add(check)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addFrame(desktop: JDesktopPane, idx: Int, resizable: Boolean) {
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

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(
    lafName: String,
    lafClassName: String,
    lafGroup: ButtonGroup
  ): JMenuItem {
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
