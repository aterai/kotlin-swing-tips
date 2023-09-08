package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val sub0 = JMenu("JMenu(Default)").also {
    it.add("JMenuItem:0")
    it.add("JMenuItem:1")
  }
  val sub1 = makeMenu("JMenu(0..2000)", 2000).also {
    it.add("JMenuItem:2")
    it.add("JMenuItem:3")
  }

  val popup = JPopupMenu()
  popup.add(sub0)
  popup.add(sub1)

  val model = SpinnerNumberModel(2000, 0, 2000, 100)
  val spinner = JSpinner(model)
  model.addChangeListener { sub1.setDelay(model.number.toInt()) }

  val mb = JMenuBar()
  mb.add(makeTopLevelMenu())
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel().also {
    EventQueue.invokeLater { it.rootPane.setJMenuBar(mb) }
    it.setComponentPopupMenu(popup)
    it.add(spinner)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTopLevelMenu(): JMenu {
  val menu = JMenu("JMenu#setDelay(...)").also {
    it.add("JMenuItem1")
    it.add("JMenuItem2")
  }
  val sub = JMenu("JMenu(Default)").also {
    it.add("JMenuItem4")
    it.add("JMenuItem5")
  }
  menu.add(sub)
  val sub0 = makeMenu("JMenu(0)", 0).also {
    it.add("JMenuItem6")
    it.add("JMenuItem7")
  }
  menu.add(sub0)
  val sub1 = makeMenu("JMenu(2000)", 2000).also {
    it.add("JMenuItem8")
    it.add("JMenuItem9")
  }
  menu.add(sub1)
  val sub2 = makeMenu("JMenu(500)", 500).also {
    it.add("JMenuItem10")
    it.add("JMenuItem11")
  }
  menu.add(sub2)
  menu.add("JMenuItem3")
  return menu
}

private fun makeMenu(title: String, delay: Int): JMenu {
  val menu = JMenu(title)
  menu.setDelay(delay)
  return menu
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
