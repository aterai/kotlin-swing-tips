package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val slider1 = makeSlider()
  val labelTable1 = slider1.labelTable
  if (labelTable1 is Map<*, *>) {
    labelTable1.forEach { (key, value) ->
      if (key is Int && value is JLabel) {
        value.text = getLabel(slider1, key)
      }
    }
  }
  slider1.labelTable = slider1.labelTable

  val slider2 = makeSlider()
  val labelTable2 = slider2.labelTable
  if (labelTable2 is Map<*, *>) {
    labelTable2.forEach { (key, value) ->
      if (key is Int && value is JLabel) {
        value.text = " "
      }
    }
  }
  slider2.labelTable = slider2.labelTable
  val layer = JLayer(slider2, SliderLabelLayerUI())

  return JPanel(BorderLayout(5, 5)).also {
    it.add(makeTitledPanel("Default", slider1), BorderLayout.NORTH)
    it.add(makeTitledPanel("JLayer", layer), BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(25, 50, 25, 50)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSlider(): JSlider {
  val slider = JSlider(0, 4)
  slider.majorTickSpacing = 1
  slider.paintLabels = true
  slider.paintTicks = true
  slider.snapToTicks = true
  return slider
}

private fun getLabel(slider: JSlider, key: Any): String {
  var txt = ""
  if (key == slider.minimum) {
    txt = "Short"
  } else if (key == slider.maximum) {
    txt = "Long"
  }
  return txt
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class SliderLabelLayerUI : LayerUI<JSlider>() {
  private val min = JLabel("Short")
  private val max = JLabel("Long")
  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val s = (c as? JLayer<*>)?.view as? JSlider ?: return
    val d = c.getSize()
    val d2 = min.preferredSize
    val metrics = s.getFontMetrics(s.font)
    val yy = s.ui.getBaseline(s, d.width, d.height) - metrics.ascent
    val xx = 2
    val w2 = d2.width
    val h2 = d2.height
    val g2 = g.create()
    SwingUtilities.paintComponent(g2, min, s, xx, yy, w2, h2)
    val d3 = max.preferredSize
    val w3 = d3.width
    val h3 = d3.height
    SwingUtilities.paintComponent(g2, max, s, d.width - w3 - xx, yy, w3, h3)
    g2.dispose()
  }
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
