package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicSliderUI

fun makeUI(): Component {
  val slider1 = object : JSlider(0, 100, 0) {
    override fun updateUI() {
      super.updateUI()
      setUI(TriSliderUI(this))
      majorTickSpacing = 10
      minorTickSpacing = 5
      paintTicks = true
      paintLabels = true
    }
  }

  val slider2 = object : JSlider(0, 100, 0) {
    override fun updateUI() {
      val sliderUI = object : BasicSliderUI(this) {
        override fun paintHorizontalLabel(
          g: Graphics,
          value: Int,
          label: Component,
        ) {
          // Windows/Motif L&F: JSlider should use foreground color for ticks. - Java Bug System
          // https://bugs.openjdk.org/browse/JDK-5099681
          label.foreground = Color.GREEN
          super.paintHorizontalLabel(g, value, label)
        }
      }
      setUI(sliderUI)
      // setBackground(Color.BLACK)
      foreground = Color.BLUE
      majorTickSpacing = 10
      minorTickSpacing = 5
      paintTicks = true
      paintLabels = true
    }
  }

  val box = Box.createVerticalBox().also {
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("TriangleSliderUI", slider1))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("HorizontalLabelColor", slider2))
    it.add(Box.createVerticalGlue())
  }

  return JPanel(BorderLayout(5, 5)).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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

private class TriSliderUI(
  slider: JSlider,
) : BasicSliderUI(slider) {
  override fun paintThumb(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.fillRect(thumbRect.x, thumbRect.y, thumbRect.width - 4, thumbRect.height - 4)
    g2.dispose()
  }

  override fun paintTrack(g: Graphics) {
    val cy: Int
    val cw: Int
    val trackBounds = trackRect
    if (slider.orientation == SwingConstants.HORIZONTAL) {
      val g2 = g.create() as? Graphics2D ?: return
      cy = -2 + trackBounds.height / 2
      cw = trackBounds.width
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.translate(trackBounds.x, trackBounds.y + cy)

      g2.paint = Color.GRAY
      g2.fillRect(0, -cy, cw, cy * 2)
      val trackLeft = 0
      val trackRight = trackRect.width - 1
      var middleOfThumb = thumbRect.x + thumbRect.width / 2
      middleOfThumb -= trackRect.x // To compensate for the g.translate()

      val fillLeft: Int
      val fillRight: Int
      if (drawInverted()) {
        fillLeft = middleOfThumb
        fillRight = if (slider.isEnabled) trackRight - 2 else trackRight - 1
      } else {
        fillLeft = if (slider.isEnabled) trackLeft + 1 else trackLeft
        fillRight = middleOfThumb
      }
      val c1 = Color(0x00_64_64)
      val c2 = Color(0x00_FF_64)
      g2.paint = GradientPaint(0f, 0f, c1, cw.toFloat(), 0f, c2, true)
      g2.fillRect(0, -cy, fillRight - fillLeft, cy * 2)
      g2.paint = slider.background
      val polygon = Polygon()
      polygon.addPoint(0, cy)
      polygon.addPoint(0, -cy)
      polygon.addPoint(cw, -cy)
      g2.fillPolygon(polygon)
      polygon.reset()
      g2.paint = Color.WHITE
      g2.drawLine(0, cy, cw - 1, cy)

      g2.dispose()
    } else {
      super.paintTrack(g)
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
