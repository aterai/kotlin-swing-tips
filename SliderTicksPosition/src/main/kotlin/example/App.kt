package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicSliderUI

fun makeUI(): Component {
  val slider1 = makeSlider(SwingConstants.VERTICAL)
  slider1.componentOrientation = ComponentOrientation.RIGHT_TO_LEFT

  val slider2 = makeSlider(SwingConstants.HORIZONTAL)

  val slider3 = makeSlider(SwingConstants.HORIZONTAL)
  slider3.paintLabels = true
  slider3.ui = BasicSliderUI(slider3)

  val slider4 = makeSlider(SwingConstants.HORIZONTAL)
  slider4.paintLabels = true
  slider4.ui = UpArrowThumbSliderUI(slider4)

  val p = JPanel(BorderLayout())
  p.add(slider3, BorderLayout.NORTH)
  p.add(slider4, BorderLayout.SOUTH)

  return JPanel(BorderLayout()).also {
    it.add(makeSlider(SwingConstants.HORIZONTAL), BorderLayout.NORTH)
    it.add(makeSlider(SwingConstants.VERTICAL), BorderLayout.WEST)
    it.add(slider1, BorderLayout.EAST)
    it.add(JLayer(slider2, VerticalFlipLayerUI()), BorderLayout.SOUTH)
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSlider(orientation: Int): JSlider {
  val slider = JSlider(orientation)
  slider.majorTickSpacing = 20
  slider.minorTickSpacing = 10
  slider.paintTicks = true
  return slider
}

private class VerticalFlipLayerUI : LayerUI<JComponent>() {
  override fun paint(g: Graphics, c: JComponent) {
    if (c is JLayer<*>) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.transform = getAffineTransform(c.getSize())
      super.paint(g2, c)
      g2.dispose()
    } else {
      super.paint(g, c)
    }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    if (c is JLayer<*>) {
      c.layerEventMask = 0
    }
    super.uninstallUI(c)
  }

  override fun eventDispatched(e: AWTEvent, l: JLayer<out JComponent>) {
    if (e is MouseEvent) {
      val pt = e.point.let {
        runCatching {
          getAffineTransform(l.size).inverseTransform(it, null)
        }.getOrNull()
      } ?: Point()
      e.translatePoint(0, pt.y.toInt() - e.y)
      e.component.repaint()
    }
    super.eventDispatched(e, l)
  }

  private fun getAffineTransform(d: Dimension): AffineTransform {
    val at = AffineTransform.getTranslateInstance(0.0, d.height.toDouble())
    at.scale(1.0, -1.0)
    return at
  }
}

private class UpArrowThumbSliderUI(slider: JSlider) : BasicSliderUI(slider) {
  override fun calculateTrackRect() {
    if (slider.orientation == SwingConstants.HORIZONTAL) {
      var centerSpacing = thumbRect.height
      if (slider.paintTicks) {
        centerSpacing -= tickLength
      }
      if (slider.paintLabels) {
        centerSpacing -= heightOfTallestLabel
      }
      trackRect.x = contentRect.x + trackBuffer
      trackRect.y = contentRect.y + (contentRect.height + centerSpacing + 1) / 2
      trackRect.width = contentRect.width - trackBuffer * 2
      trackRect.height = thumbRect.height
    } else {
      super.calculateTrackRect()
    }
  }

  override fun calculateTickRect() {
    if (slider.orientation == SwingConstants.HORIZONTAL) {
      tickRect.x = trackRect.x
      // tickRect.y = trackRect.y + trackRect.height;
      tickRect.y = trackRect.y
      tickRect.width = trackRect.width
      tickRect.height = if (slider.paintTicks) tickLength else 0
    } else {
      super.calculateTickRect()
    }
  }

  override fun calculateLabelRect() {
    if (slider.paintLabels) {
      if (slider.orientation == SwingConstants.HORIZONTAL) {
        labelRect.width = tickRect.width + trackBuffer * 2
        labelRect.height = heightOfTallestLabel
        labelRect.x = tickRect.x - trackBuffer
        labelRect.y = tickRect.y - labelRect.height
      } else {
        super.calculateLabelRect()
      }
    } else {
      if (slider.orientation == SwingConstants.HORIZONTAL) {
        labelRect.x = tickRect.x
        labelRect.y = tickRect.y // + tickRect.height;
        labelRect.width = tickRect.width
        labelRect.height = 0
      } else {
        super.calculateLabelRect()
      }
    }
  }

  override fun paintThumb(g: Graphics) {
    if (slider.orientation == SwingConstants.HORIZONTAL) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.translate(0, contentRect.y + contentRect.height + thumbRect.height)
      g2.scale(1.0, -1.0)
      super.paintThumb(g2)
      g2.dispose()
    } else {
      super.paintThumb(g)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
