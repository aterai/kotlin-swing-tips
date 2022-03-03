package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val model = DefaultBoundedRangeModel(50, 0, 0, 100)
  val slider0 = JSlider(SwingConstants.VERTICAL)
  val slider1 = JSlider(SwingConstants.VERTICAL)
  val slider2 = JSlider(SwingConstants.HORIZONTAL)
  val slider3 = JSlider(SwingConstants.HORIZONTAL)
  val list = listOf(slider0, slider1, slider2, slider3)

  val check = JCheckBox("ComponentOrientation.RIGHT_TO_LEFT")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    val orientation = if (b) {
      ComponentOrientation.RIGHT_TO_LEFT
    } else {
      ComponentOrientation.LEFT_TO_RIGHT
    }
    list.forEach { it.componentOrientation = orientation }
  }

  list.forEach {
    it.model = model
    it.majorTickSpacing = 20
    it.minorTickSpacing = 10
    it.paintTicks = true
    it.paintLabels = true
  }
  slider1.inverted = true
  slider3.inverted = true

  val box1 = Box.createHorizontalBox()
  box1.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
  box1.add(slider0)
  box1.add(Box.createHorizontalStrut(20))
  box1.add(slider1)
  box1.add(Box.createHorizontalGlue())

  val box2 = Box.createVerticalBox()
  box2.border = BorderFactory.createEmptyBorder(20, 0, 20, 20)
  box2.add(makeTitledPanel("Default", slider2))
  box2.add(Box.createVerticalStrut(20))
  box2.add(makeTitledPanel("setInverted(true)", slider3))
  box2.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(box1, BorderLayout.WEST)
    it.add(box2)
    it.add(check, BorderLayout.SOUTH)
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
