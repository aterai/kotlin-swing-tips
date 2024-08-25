package example

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.Window
import javax.swing.AbstractButton
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JFrame
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JRadioButtonMenuItem
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JSplitPane
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException
import javax.swing.WindowConstants
import javax.swing.event.MenuEvent
import javax.swing.event.MenuListener

fun makeUI(): Component {
  val menuBar = object : JMenuBar() {
    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
  }
  initMenuBar(menuBar)
  val listener = object : MenuListener {
    override fun menuSelected(e: MenuEvent) {
      val menu = e.source
      if (menu is JMenu && menu.isTopLevelMenu) {
        EventQueue.invokeLater {
          val loc = menu.locationOnScreen
          loc.x += menu.width
          menu.popupMenu.location = loc
        }
      }
    }

    override fun menuDeselected(e: MenuEvent) {
      // Do nothing
    }

    override fun menuCanceled(e: MenuEvent) {
      // Do nothing
    }
  }
  menuBar
    .subElements
    .filterIsInstance<JMenu>()
    .forEach {
      it.addMenuListener(listener)
      val d = it.maximumSize
      d.width = Short.MAX_VALUE.toInt()
      it.maximumSize = d
    }

  val p = JPanel(BorderLayout())
  p.add(menuBar, BorderLayout.NORTH)

  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).also {
    it.leftComponent = p
    it.rightComponent = JScrollPane(JTree())
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(split)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initMenuBar(menuBar: JMenuBar) {
  val menu1 = JMenu("question")
  menu1.icon = UIManager.getIcon("OptionPane.questionIcon")
  menuBar.add(menu1)
  menu1.add(JMenuItem("warning", UIManager.getIcon("OptionPane.warningIcon")))
  menu1.add(JMenuItem("error", UIManager.getIcon("OptionPane.errorIcon")))

  val menu2 = JMenu("warning")
  menu2.icon = UIManager.getIcon("OptionPane.warningIcon")
  menuBar.add(menu2)

  val menu3 = JMenu("error")
  menu3.icon = UIManager.getIcon("OptionPane.errorIcon")
  menuBar.add(menu3)

  val menu4 = JMenu("information")
  menu4.icon = UIManager.getIcon("OptionPane.informationIcon")
  menuBar.add(menu4)
  menu4.add(makeSubMenu())

  menuBar.add(JSeparator(SwingConstants.HORIZONTAL))
  menuBar.add(makeSubMenu())
}

private fun makeSubMenu(): JMenu {
  val sub1 = JMenu("JMenu1")
  sub1.add("MenuItem1")
  sub1.add("MenuItem2")
  val sub2 = JMenu("JMenu2")
  sub2.add("MenuItem3")
  sub2.add("MenuItem4")
  sub1.add(sub2)
  return sub1
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
