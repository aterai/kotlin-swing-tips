package example

import java.awt.*
import javax.swing.*

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
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
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
