package example

import java.awt.*
import javax.swing.*
import javax.swing.event.ChangeListener

fun makeUI(): Component {
  val bar = JToolBar().also {
    it.add(JCheckBox("JCheckBox"))
    it.addSeparator()
    it.add(JRadioButton("JRadioButton"))
    it.addSeparator(Dimension(32, 32))
    it.add(JButton("JButton"))
    it.addSeparator(Dimension(10, 10))
    it.add(JToggleButton("JToggleButton"))
    it.add(Box.createVerticalGlue())
  }

  val mw = SpinnerNumberModel(10, -10, 50, 1)
  val mh = SpinnerNumberModel(32, -10, 50, 1)
  val cl = ChangeListener {
    val d = Dimension(mw.number.toInt(), mh.number.toInt())
    for (c in bar.components) {
      if (c is JToolBar.Separator) {
        c.separatorSize = d
      }
    }
    bar.revalidate()
  }
  mw.addChangeListener(cl)
  mh.addChangeListener(cl)

  val button = JButton("reset")
  button.addActionListener {
    val list = bar.components
    bar.removeAll()
    for (c in list) {
      if (c is JToolBar.Separator) {
        bar.addSeparator()
      } else {
        bar.add(c)
      }
    }
  }

  val p = JPanel().also {
    it.add(JLabel("width:"))
    it.add(JSpinner(mw))
    it.add(JLabel("height:"))
    it.add(JSpinner(mh))
    it.add(button)
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(BorderLayout(5, 5)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(bar, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
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
