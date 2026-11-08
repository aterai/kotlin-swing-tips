package example

import java.awt.*
import javax.swing.*
import kotlin.math.max
import kotlin.math.min

fun createUI(): Component {
  val menuBar = JMenuBar()
  menuBar.add(LookAndFeelUtils.createLookAndFeelMenu())
  menuBar.add(createSpinnerMenu())
  menuBar.add(createSliderMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(JScrollPane(JTextArea()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createMenuBox(title: String, c: Component): Component {
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  box.add(JLabel(title))
  box.add(c)
  return box
}

private fun createComponentMenuItem(title: String, c: Component): Component {
  val item = object : JMenuItem() {
    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
      isEnabled = false
      layout = FlowLayout(FlowLayout.RIGHT, 0, 0)
    }

    override fun getPreferredSize(): Dimension {
      // BasicMenuItemUI ignores child components when computing the size,
      // so merge in the layout size (label, component and border insets)
      // to avoid clipping the bottom border area or the leading label.
      val d = super.getPreferredSize()
      val p = layout.preferredLayoutSize(this)
      d.width = max(d.width, p.width)
      d.height = max(d.height, p.height)
      return d
    }
  }
  item.add(JLabel(title))
  item.add(c)
  return item
}

private fun createSpinnerMenu(): JMenu {
  val model1 = SpinnerNumberModel(100, 10, 300, 10)
  val model2 = SpinnerNumberModel(150, 10, 300, 10)
  val menu = JMenu("JSpinner")
  menu.add(createMenuBox("L: ", createSpinner(model1)))
  menu.add(createMenuBox("R: ", createSpinner(model2)))
  menu.add(JSeparator())
  menu.add(createComponentMenuItem("Left: ", createSpinner(model1)))
  menu.add(createComponentMenuItem("Right: ", createSpinner(model2)))
  menu.addSeparator()
  menu.add("JMenuItem1")
  menu.add("JMenuItem2")
  menu.add(Box.createHorizontalStrut(160))
  return menu
}

private fun createSpinner(model: SpinnerModel): JSpinner {
  val spinner = JSpinner(model)
  (spinner.editor as? JSpinner.DefaultEditor)?.textField?.columns = 8
  spinner.addMouseWheelListener { e ->
    val s = e.component as? JSpinner
    if (s?.isEnabled == true) {
      spin(s.model, e.wheelRotation)
    }
    e.consume()
  }
  return spinner
}

private fun spin(model: SpinnerModel, rotation: Int) {
  // getNextValue()/getPreviousValue() respect the step size and bounds,
  // whereas SpinnerNumberModel#setValue(...) does not check the bounds
  val value = when {
    rotation < 0 -> model.nextValue
    rotation > 0 -> model.previousValue
    else -> null
  }
  if (value != null) {
    model.value = value
  }
}

private fun createSliderMenu(): JMenu {
  val model1 = DefaultBoundedRangeModel(90, 1, 0, 100)
  val model2 = DefaultBoundedRangeModel(50, 1, 0, 100)
  val menu = JMenu("JSlider")
  menu.add(createMenuBox("L: ", createSlider(model1)))
  menu.add(createMenuBox("R: ", createSlider(model2)))
  menu.add(JSeparator())
  menu.add(createComponentMenuItem("Left: ", createSlider(model1)))
  menu.add(createComponentMenuItem("Right: ", createSlider(model2)))
  menu.addSeparator()
  menu.add("JMenuItem3")
  menu.add("JMenuItem4")
  menu.add(Box.createHorizontalStrut(160))
  return menu
}

private fun createSlider(model: BoundedRangeModel): JSlider {
  val slider = object : JSlider(model) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = min(d.width, 100)
      return d
    }
  }
  slider.border = BorderFactory.createEmptyBorder(1, 1, 4, 1)
  slider.isOpaque = false
  slider.addMouseWheelListener { e ->
    val s = e.component as? JSlider
    if (s?.isEnabled == true) {
      s.model.value -= e.wheelRotation
    }
    e.consume()
  }
  return slider
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
