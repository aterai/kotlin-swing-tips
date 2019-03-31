package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalSliderUI

class MainPanel : JPanel(BorderLayout()) {
  init {
    val list = listOf(
      makeSilder("Default SnapToTicks"),
      makeSilder("Custom SnapToTicks")
    )

    val check = JCheckBox("JSlider.setMinorTickSpacing(5)")
    check.addActionListener { e ->
      val mts = if ((e.getSource() as JCheckBox).isSelected()) 5 else 0
      list.forEach { slider -> slider.setMinorTickSpacing(mts) }
    }

    val box = Box.createVerticalBox()
    box.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    for (slider in list) {
      box.add(slider)
      box.add(Box.createVerticalStrut(10))
    }
    box.add(check)
    box.add(Box.createVerticalGlue())

    add(box)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeSilder(title: String): JSlider {
    val slider = JSlider(0, 100, 50)
    // JSlider slider = new JSlider(-50, 50, 0);
    slider.setBorder(BorderFactory.createTitledBorder(title))
    slider.setMajorTickSpacing(10)
    slider.setSnapToTicks(true)
    slider.setPaintTicks(true)
    slider.setPaintLabels(true)
    if (title.startsWith("Default")) {
      return slider
    }
    slider.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "RIGHT_ARROW")
    slider.getActionMap().put("RIGHT_ARROW", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val s = e.getSource() as JSlider
        s.setValue(s.getValue() + s.getMajorTickSpacing())
      }
    })
    slider.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "LEFT_ARROW")
    slider.getActionMap().put("LEFT_ARROW", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val s = e.getSource() as JSlider
        s.setValue(s.getValue() - s.getMajorTickSpacing())
      }
    })
    slider.addMouseWheelListener { e ->
      val s = e.getComponent() as JSlider
      val hasMinorTickSpacing = s.getMinorTickSpacing() > 0
      val tickSpacing = if (hasMinorTickSpacing) s.getMinorTickSpacing() else s.getMajorTickSpacing()
      val v = s.getValue() - e.getWheelRotation() * tickSpacing
      val m = s.getModel()
      s.setValue(Math.min(m.getMaximum(), Math.max(v, m.getMinimum())))
    }
    if (slider.getUI() is WindowsSliderUI) {
      slider.setUI(WindowsSnapToTicksDragSliderUI(slider))
    } else {
      slider.setUI(MetalSnapToTicksDragSliderUI())
    }
    return slider
  }
}

internal class WindowsSnapToTicksDragSliderUI(slider: JSlider) : WindowsSliderUI(slider) {
  protected override fun createTrackListener(slider: JSlider): TrackListener {
    return object : TrackListener() {
      override fun mouseDragged(e: MouseEvent) {
        if (!slider.getSnapToTicks() || slider.getMajorTickSpacing() == 0) {
          super.mouseDragged(e)
          return
        }
        // case HORIZONTAL:
        val halfThumbWidth = thumbRect.width / 2
        val trackLength = trackRect.width
        val trackLeft = trackRect.x - halfThumbWidth
        val trackRight = trackRect.x + trackRect.width - 1 + halfThumbWidth
        val xpos = e.getX()
        val snappedPos = if (xpos <= trackLeft) {
          trackLeft
        } else if (xpos >= trackRight) {
          trackRight
        } else {
          offset = 0
          val possibleTickPositions = slider.getMaximum() - slider.getMinimum()
          val hasMinorTick = slider.getMinorTickSpacing() > 0
          val tickSpacing = if (hasMinorTick) slider.getMinorTickSpacing() else slider.getMajorTickSpacing()
          val actualPixelsForOneTick = trackLength * tickSpacing / possibleTickPositions.toFloat()
          val px = xpos - trackLeft
          (Math.round(px / actualPixelsForOneTick) * actualPixelsForOneTick + .5).toInt() + trackLeft
        }
        e.translatePoint(snappedPos - xpos, 0)
        super.mouseDragged(e)
      }
    }
  }
}

internal class MetalSnapToTicksDragSliderUI : MetalSliderUI() {
  protected override fun createTrackListener(slider: JSlider): TrackListener {
    return object : TrackListener() {
      override fun mouseDragged(e: MouseEvent) {
        if (!slider.getSnapToTicks() || slider.getMajorTickSpacing() == 0) {
          super.mouseDragged(e)
          return
        }
        // case HORIZONTAL:
        val halfThumbWidth = thumbRect.width / 2
        val trackLength = trackRect.width
        val trackLeft = trackRect.x - halfThumbWidth
        val trackRight = trackRect.x + trackRect.width - 1 + halfThumbWidth
        val xpos = e.getX()
        val snappedPos = if (xpos <= trackLeft) {
          trackLeft
        } else if (xpos >= trackRight) {
          trackRight
        } else {
          offset = 0
          val possibleTickPositions = slider.getMaximum() - slider.getMinimum()
          val hasMinorTick = slider.getMinorTickSpacing() > 0
          val tickSpacing = if (hasMinorTick) slider.getMinorTickSpacing() else slider.getMajorTickSpacing()
          val actualPixelsForOneTick = trackLength * tickSpacing / possibleTickPositions.toFloat()
          val px = xpos - trackLeft
          (Math.round(px / actualPixelsForOneTick) * actualPixelsForOneTick + .5).toInt() + trackLeft
        }
        e.translatePoint(snappedPos - xpos, 0)
        super.mouseDragged(e)
      }
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
