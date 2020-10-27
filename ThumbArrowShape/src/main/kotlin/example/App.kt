package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val slider0 = JSlider(SwingConstants.VERTICAL)
  val slider1 = JSlider(SwingConstants.VERTICAL)
  val slider2 = JSlider(SwingConstants.VERTICAL)
  val slider3 = JSlider(SwingConstants.HORIZONTAL)
  val slider4 = JSlider(SwingConstants.HORIZONTAL)
  val slider5 = JSlider(SwingConstants.HORIZONTAL)
  val model = DefaultBoundedRangeModel(50, 0, 0, 100)
  listOf(slider0, slider1, slider2, slider3, slider4, slider5).forEach {
    it.model = model
  }
  slider1.majorTickSpacing = 20
  slider1.paintTicks = true
  val key = "Slider.paintThumbArrowShape"
  slider2.putClientProperty(key, true)
  slider4.majorTickSpacing = 20
  slider4.paintTicks = true
  slider5.putClientProperty(key, true)

  val box1 = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(20, 5, 20, 5)
    it.add(slider0)
    it.add(Box.createHorizontalStrut(20))
    it.add(slider1)
    it.add(Box.createHorizontalStrut(20))
    it.add(slider2)
    it.add(Box.createHorizontalGlue())
  }

  val box2 = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(20, 0, 20, 5)
    it.add(makeTitledPanel("Default", slider3))
    it.add(Box.createVerticalStrut(20))
    it.add(makeTitledPanel("setPaintTicks", slider4))
    it.add(Box.createVerticalStrut(20))
    it.add(makeTitledPanel(key, slider5))
    it.add(Box.createVerticalGlue())
  }

  return JPanel(BorderLayout()).also {
    it.add(box1, BorderLayout.WEST)
    it.add(box2)
    val mb = JMenuBar()
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafRadioGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.actionCommand = lafClassName
    lafItem.mnemonic = lafName.codePointAt(0)
    lafItem.hideActionText = true
    lafItem.addActionListener {
      val m = lafRadioGroup.selection
      runCatching {
        setLookAndFeel(m.actionCommand)
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
