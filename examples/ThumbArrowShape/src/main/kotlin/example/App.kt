package example

import java.awt.*
import javax.swing.*

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
