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
    (e.source as? JSlider)?.also {
      spinner.value = it.value * 10
    }
  }
  slider.addMouseWheelListener { e ->
    (e.source as? JSlider)?.also {
      slider.value = it.value - e.wheelRotation
    }
  }
  spinner.addChangeListener { e ->
    (e.source as? JSpinner)?.also {
      val iv = it.value as? Int ?: 0
      slider.value = iv / 10
    }
  }
  spinner.addMouseWheelListener { e ->
    (e.component as? JSpinner)?.also {
      (it.model as? SpinnerNumberModel)?.also { m ->
        val iv = it.value as? Int ?: 0
        val v = iv - (e.preciseWheelRotation * m.stepSize.toInt()).toInt()
        val min = m.minimum as? Int ?: 0
        val max = m.maximum as? Int ?: 0
        if (v in min..max) {
          it.value = v
        }
      }
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
