package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val slider = makeSlider()
  val table = slider.labelTable
  slider.labelTable = table // @see JSlider#setLabelTable(...)

  val box = Box.createVerticalBox()
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Default", makeSlider()))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("JSlider#updateLabelUIs()", slider))
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout(5, 5)).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSlider(): JSlider {
  val slider = JSlider(0, 10_000)
  slider.putClientProperty("Slider.paintThumbArrowShape", true)
  slider.majorTickSpacing = 2500
  slider.minorTickSpacing = 500
  slider.paintLabels = true
  slider.paintTicks = true
  slider.snapToTicks = true

  val labelTable = slider.labelTable
  (labelTable as? Map<*, *>)?.forEach { (key, value) ->
    if (key is Int && value is JLabel) {
      value.text = (key / 100).toString()
    }
  }
  return slider
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
