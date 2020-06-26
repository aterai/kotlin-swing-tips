package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val slider = JSlider(0, 100, 50)
  val spinner = JSpinner(SpinnerNumberModel(500, 0, 1000, 10))
  slider.majorTickSpacing = 10
  slider.minorTickSpacing = 1
  slider.paintTicks = true
  slider.paintLabels = true
  slider.addChangeListener { e ->
    val source = e.source as JSlider
    spinner.value = source.value * 10
  }
  slider.addMouseWheelListener { e ->
    val source = e.component as JSlider
    slider.value = source.value - e.wheelRotation
  }
  spinner.addChangeListener { e ->
    val source = e.source as JSpinner
    val newValue = source.value as Int
    slider.value = newValue / 10
  }
  spinner.addMouseWheelListener { e ->
    val source = e.component as JSpinner
    val model = source.model as SpinnerNumberModel
    val oldValue = source.value as Int
    val intValue = oldValue - (e.preciseWheelRotation * model.stepSize.toInt()).toInt()
    if (intValue in model.minimum..model.maximum) {
      source.value = intValue
    }
  }

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("MouseWheel+JSpinner", spinner))
    it.add(makeTitledPanel("MouseWheel+JSlider", slider))
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, cmp: Component): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  p.add(cmp, c)
  return p
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
