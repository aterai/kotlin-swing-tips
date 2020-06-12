package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.roundToInt

fun makeUI(): Component {
  val d = UIDefaults()
  d["Slider:SliderTrack[Enabled].backgroundPainter"] = object : Painter<JSlider> {
    override fun paint(g: Graphics2D, c: JSlider, w: Int, h: Int) {
      val arc = 10
      val trackHeight = 8
      val trackWidth = w - 2
      val fillTop = 4
      val fillLeft = 1
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g.stroke = BasicStroke(1.5f)
      g.color = Color.GRAY
      g.fillRoundRect(fillLeft, fillTop, trackWidth, trackHeight, arc, arc)
      val fillBottom = fillTop + trackHeight
      val fillRight =
        getXPositionForValue(c, Rectangle(fillLeft, fillTop, trackWidth, fillBottom - fillTop))
      g.color = Color.ORANGE
      g.fillRect(fillLeft + 1, fillTop + 1, fillRight - fillLeft, fillBottom - fillTop)
      g.color = Color.WHITE
      g.drawRoundRect(fillLeft, fillTop, trackWidth, trackHeight, arc, arc)
    }

    // @see javax/swing/plaf/basic/BasicSliderUI#xPositionForValue(int value)
    private fun getXPositionForValue(slider: JSlider, trackRect: Rectangle): Int {
      val value = slider.value
      val min = slider.minimum
      val max = slider.maximum
      val trackLength = trackRect.width
      val valueRange = max.toFloat() - min.toFloat()
      val pixelsPerValue = trackLength.toFloat() / valueRange
      val trackLeft = trackRect.x
      val trackRight = trackRect.x + trackRect.width - 1
      return (trackLeft + (pixelsPerValue * (value.toFloat() - min)).roundToInt()).coerceIn(trackLeft, trackRight)
    }
  }

  val slider = JSlider()
  slider.putClientProperty("Nimbus.Overrides", d)

  val box = Box.createVerticalBox()
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Default", JSlider()))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Nimbus JSlider.isFilled", slider))
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
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
