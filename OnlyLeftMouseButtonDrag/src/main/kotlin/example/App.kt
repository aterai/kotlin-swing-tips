package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val model = DefaultBoundedRangeModel(50, 0, 0, 100)
  val slider1 = JSlider(SwingConstants.VERTICAL)
  val slider2 = JSlider(SwingConstants.HORIZONTAL)
  listOf(slider1, slider2).forEach {
    it.model = model
    it.majorTickSpacing = 20
    it.minorTickSpacing = 10
    it.paintTicks = true
    it.paintLabels = true
  }
  val key = "Slider.onlyLeftMouseButtonDrag"
  val check = object : JCheckBox(key) {
    override fun updateUI() {
      super.updateUI()
      isSelected = UIManager.getLookAndFeelDefaults().getBoolean(key)
    }
  }
  check.addActionListener { e ->
    val f = (e.source as? JCheckBox)?.isSelected == true
    UIManager.put(key, f)
  }

  val box1 = Box.createHorizontalBox()
  box1.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
  box1.add(slider1)
  box1.add(Box.createHorizontalGlue())

  val box2 = Box.createVerticalBox()
  box2.border = BorderFactory.createEmptyBorder(20, 0, 20, 20)
  box2.add(slider2)
  box2.add(Box.createVerticalGlue())

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { box1.rootPane.jMenuBar = mb }

  return JPanel(BorderLayout()).also {
    it.add(box1, BorderLayout.WEST)
    it.add(box2)
    it.add(check, BorderLayout.SOUTH)
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
    UnsupportedLookAndFeelException::class
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
