package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val slider1 = makeSlider()
  setCurrentLabelListener(slider1)

  val list2 = listOf("A", "B", "C", "D", "E")
  val slider2 = JSlider(0, list2.size - 1, 0)
  setCurrentLabelListener(slider2)
  slider2.snapToTicks = true
  slider2.paintTicks = true
  slider2.paintLabels = true
  slider2.majorTickSpacing = 1
  val labelTable = slider2.labelTable
  if (labelTable is Map<*, *>) {
    labelTable.forEach { key, value ->
      if (key is Int && value is JLabel) {
        updateLabel(list2, slider2, key, value)
      }
    }
  }
  slider2.labelTable = labelTable

  val box = Box.createVerticalBox()
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Default", makeSlider()))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("setMajorTickSpacing(10)", slider1))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("setMajorTickSpacing(0)", slider2))
  box.add(Box.createVerticalGlue())

  EventQueue.invokeLater {
    slider1.model.value = 40
    slider1.repaint()
  }

  val p = JPanel(BorderLayout(5, 5))
  p.add(box)
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeSlider(): JSlider {
  val slider = JSlider(0, 100)
  slider.majorTickSpacing = 10
  slider.paintLabels = true
  slider.snapToTicks = true
  slider.paintTicks = true
  return slider
}

private fun updateLabel(list: List<String>, slider: JSlider, i: Int, l: JLabel) {
  l.text = list[i]
  if (slider.value == i) {
    l.foreground = Color.RED
  }
}

private fun setCurrentLabelListener(slider: JSlider) {
  val prev = AtomicInteger(-1)
  slider.model.addChangeListener { e ->
    val i = (e.source as? BoundedRangeModel)?.value ?: prev.get()
    if ((slider.majorTickSpacing == 0 || i % slider.majorTickSpacing == 0) && i != prev.get()) {
      val labelTable = slider.labelTable
      if (labelTable is Map<*, *>) {
        resetForeground(labelTable[i], Color.RED)
        resetForeground(labelTable[prev.get()], Color.BLACK)
      }
      slider.repaint()
      prev.set(i)
    }
  }
}

private fun resetForeground(o: Any?, c: Color) {
  (o as? Component)?.foreground = c
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
