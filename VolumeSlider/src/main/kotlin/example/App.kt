package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalSliderUI

fun makeUI(): Component {
  val slider1 = JSlider(0, 100, 0)
  slider1.ui = TriSliderUI()
  slider1.majorTickSpacing = 10
  slider1.minorTickSpacing = 5
  slider1.paintTicks = true
  slider1.paintLabels = true

  val slider2 = JSlider(0, 100, 0)
  slider2.ui = object : MetalSliderUI() {
    override fun paintHorizontalLabel(g: Graphics, value: Int, label: Component) {
      // [JDK-5099681] Windows/Motif L&F: JSlider should use foreground color for ticks. - Java Bug System
      // https://bugs.openjdk.java.net/browse/JDK-5099681
      label.foreground = Color.GREEN
      super.paintHorizontalLabel(g, value, label)
    }
  }
  // slider2.setBackground(Color.BLACK);
  slider2.foreground = Color.BLUE
  slider2.majorTickSpacing = 10
  slider2.minorTickSpacing = 5
  slider2.paintTicks = true
  slider2.paintLabels = true

  val box = Box.createVerticalBox()
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("TriangleSliderUI", slider1))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("HorizontalLabelColor", slider2))
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout(5, 5)).also {
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

private class TriSliderUI : MetalSliderUI() {
  override fun paintThumb(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height)
    g2.dispose()
  }

  override fun paintTrack(g: Graphics) {
    val cy: Int
    val cw: Int
    // int pad;
    val trackBounds = trackRect
    if (slider.orientation == SwingConstants.HORIZONTAL) {
      val g2 = g.create() as? Graphics2D ?: return
      cy = -2 + trackBounds.height / 2
      cw = trackBounds.width
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
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
      g2.paint = GradientPaint(0f, 0f, Color(0, 100, 100), cw.toFloat(), 0f, Color(0, 255, 100), true)
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
