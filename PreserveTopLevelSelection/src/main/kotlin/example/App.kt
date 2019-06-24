package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel() {
  init {
    val key = "Menu.preserveTopLevelSelection"

    val b = UIManager.getBoolean(key)
    println("$key: $b")
    val preserveTopLevelSelectionCheck = object : JCheckBox(key, b) {
      override fun updateUI() {
        super.updateUI()
        setSelected(UIManager.getLookAndFeelDefaults().getBoolean(key))
        UIManager.put(key, isSelected())
      }
    }
    preserveTopLevelSelectionCheck.addActionListener { e ->
      UIManager.put(key, (e.getSource() as JCheckBox).isSelected())
    }

    EventQueue.invokeLater { getRootPane().setJMenuBar(makeMenuBar()) }
    add(preserveTopLevelSelectionCheck)
    setPreferredSize(Dimension(320, 240))
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
      menu.add(JMenu("JMenu").also { sub ->
        sub.add("JMenuItem4")
        sub.add("JMenuItem5")
      })
      menu.add("JMenuItem3")
    }

    it.add(LookAndFeelUtil.createLookAndFeelMenu())
  }
}

// @see https://java.net/projects/swingset3/sources/svn/content/trunk/SwingSet3/src/com/sun/swingset3/SwingSet3.java
internal object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.getName()
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.getName(), lafInfo.getClassName(), lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafRadioGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.setActionCommand(lafClassName)
    lafItem.setHideActionText(true)
    lafItem.addActionListener {
      val m = lafRadioGroup.getSelection()
      runCatching {
        setLookAndFeel(m.getActionCommand())
      }.onFailure {
        it.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
      }
    }
    lafRadioGroup.add(lafItem)
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
      // firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel);
    }
  }

  private fun updateLookAndFeel() {
    for (window in Frame.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
} /* Singleton */

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
