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

// @see https://java.net/projects/swingset3/sources/svn/content/trunk/SwingSet3/src/com/sun/swingset3/SwingSet3.java
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
      // firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel)
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
