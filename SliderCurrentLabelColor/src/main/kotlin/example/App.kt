package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Dictionary
import java.util.Hashtable
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

fun makeUI(): Component {
  val slider1 = makeSlider()
  setCurrentLabelListener(slider1)

  val labelTable: Dictionary<Int, Component> = Hashtable()
  listOf("A", "B", "C", "D", "E").map { JLabel(it) }
    .forEachIndexed { i, c -> labelTable.put(i, c) }

  val slider2 = JSlider(0, 4, 0)
  setCurrentLabelListener(slider2)
  slider2.labelTable = labelTable
  slider2.snapToTicks = true
  slider2.paintTicks = true
  slider2.paintLabels = true

  labelTable[0].foreground = Color.RED
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

private fun setCurrentLabelListener(slider: JSlider) {
  slider.model.addChangeListener(object : ChangeListener {
    private var prev = -1

    private fun resetForeground(o: Any?, c: Color) {
      (o as? Component)?.foreground = c
    }

    override fun stateChanged(e: ChangeEvent) {
      val m = e.source as? BoundedRangeModel ?: return
      val i = m.value
      if ((slider.majorTickSpacing == 0 || i % slider.majorTickSpacing == 0) && i != prev) {
        val dictionary = slider.labelTable
        resetForeground(dictionary[i], Color.RED)
        // if (prev >= 0) {
        resetForeground(dictionary[prev], Color.BLACK)
        slider.repaint()
        prev = i
      }
    }
  })
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
