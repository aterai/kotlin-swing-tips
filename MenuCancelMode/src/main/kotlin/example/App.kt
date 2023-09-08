package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.KeyEvent
import javax.swing.*

fun makeUI(): Component {
  val key = "Menu.cancelMode"
  val cancelMode = UIManager.getString(key)
  // println("$key: $cancelMode")
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
  val laf = LookAndFeelUtils.createLookAndFeelMenu()
  laf.mnemonic = KeyEvent.VK_L
  bar.add(laf)
  return bar
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
