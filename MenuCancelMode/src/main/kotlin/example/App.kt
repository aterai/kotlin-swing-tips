package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val key = "Menu.cancelMode"
  val cancelMode = UIManager.getString(key)
  println("$key: $cancelMode")
  val defaultMode = "hideMenuTree" == cancelMode
  val hideMenuTreeRadio = makeRadioButton("hideMenuTree", defaultMode)
  val hideLastSubmenuRadio = makeRadioButton("hideLastSubmenu", !defaultMode)
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createTitledBorder(key)
  val handler = ItemListener { e ->
    val r = e.source
    if (r is JRadioButton && e.stateChange == ItemEvent.SELECTED) {
      UIManager.put(key, r.text)
    }
  }
  val bg = ButtonGroup()
  listOf(hideLastSubmenuRadio, hideMenuTreeRadio).forEach {
    it.addItemListener(handler)
    bg.add(it)
    box.add(it)
  }
  return JPanel().also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = makeMenuBar() }
    it.add(box)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeRadioButton(
  text: String,
  selected: Boolean
) = object : JRadioButton(text, selected) {
  override fun updateUI() {
    super.updateUI()
    val mode = UIManager.getLookAndFeelDefaults().getString("Menu.cancelMode")
    isSelected = text == mode
  }
}

private fun makeMenuBar(): JMenuBar {
  val bar = JMenuBar()
  val menu = bar.add(JMenu("Test"))
  menu.add("JMenuItem1")
  menu.add("JMenuItem2")
  val sub = JMenu("JMenu")
  sub.add("JMenuItem4")
  sub.add("JMenuItem5")
  menu.add(sub)
  menu.add("JMenuItem3")
  bar.add(LookAndFeelUtil.createLookAndFeelMenu())
  return bar
}

// @see https://java.net/projects/swingset3/sources/svn/content/trunk/SwingSet3/src/com/sun/swingset3/SwingSet3.java
private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    it.mnemonic = KeyEvent.VK_L
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
    for (window in Frame.getWindows()) {
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
