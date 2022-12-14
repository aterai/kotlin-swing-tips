package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val key = "Menu.preserveTopLevelSelection"
  val b = UIManager.getBoolean(key)
  val keepTopLvlSel = object : JCheckBox(key, b) {
    override fun updateUI() {
      super.updateUI()
      isSelected = UIManager.getLookAndFeelDefaults().getBoolean(key)
      UIManager.put(key, isSelected)
    }
  }
  keepTopLvlSel.addActionListener { e ->
    UIManager.put(key, (e.source as? JCheckBox)?.isSelected == true)
  }

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = makeMenuBar() }
    it.add(keepTopLvlSel)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMenuBar() = JMenuBar().also {
  it.add(JMenu("File")).also { menu ->
    menu.add("Open")
    menu.add("Save")
    menu.add("Exit")
  }

  it.add(JMenu("Edit")).also { menu ->
    menu.add("Undo")
    menu.add("Redo")
    menu.addSeparator()
    menu.add("Cut")
    menu.add("Copy")
    menu.add("Paste")
    menu.add("Delete")
  }

  it.add(JMenu("Test")).also { menu ->
    menu.add("JMenuItem1")
    menu.add("JMenuItem2")
    val m1 = JMenu("JMenu").also { sub ->
      sub.add("JMenuItem4")
      sub.add("JMenuItem5")
    }
    menu.add(m1)
    menu.add("JMenuItem3")
  }

  it.add(LookAndFeelUtil.createLookAndFeelMenu())
}

private object LookAndFeelUtil {
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
