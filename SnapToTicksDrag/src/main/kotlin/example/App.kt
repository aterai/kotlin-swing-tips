package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalSliderUI
import kotlin.math.roundToInt

class MainPanel : JPanel(BorderLayout()) {
  init {
    val slider = makeSlider("Custom SnapToTicks")
    initSlider(slider)

    val list = listOf(makeSlider("Default SnapToTicks"), slider)

    val check = JCheckBox("JSlider.setMinorTickSpacing(5)")
    check.addActionListener { e ->
      val mts = if ((e.getSource() as? JCheckBox)?.isSelected() == true) 5 else 0
      list.forEach { it.setMinorTickSpacing(mts) }
    }

    val box = Box.createVerticalBox()
    box.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    for (s in list) {
      box.add(s)
      box.add(Box.createVerticalStrut(10))
    }
    box.add(check)
    box.add(Box.createVerticalGlue())

    add(box)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeSlider(title: String) = JSlider(0, 100, 50).also {
    it.setBorder(BorderFactory.createTitledBorder(title))
    it.setMajorTickSpacing(10)
    it.setSnapToTicks(true)
    it.setPaintTicks(true)
    it.setPaintLabels(true)
  }

  private fun initSlider(slider: JSlider) {
    slider.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "RIGHT_ARROW")
    slider.getActionMap().put("RIGHT_ARROW", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val s = e.getSource() as? JSlider ?: return
        s.setValue(s.getValue() + s.getMajorTickSpacing())
      }
    })
    slider.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "LEFT_ARROW")
    slider.getActionMap().put("LEFT_ARROW", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val s = e.getSource() as? JSlider ?: return
        s.setValue(s.getValue() - s.getMajorTickSpacing())
      }
    })
    slider.addMouseWheelListener { e ->
      val s = e.getComponent() as? JSlider ?: return@addMouseWheelListener
      val hasMinorTickSpacing = s.getMinorTickSpacing() > 0
      val tickSpacing = if (hasMinorTickSpacing) s.getMinorTickSpacing() else s.getMajorTickSpacing()
      val v = s.getValue() - e.getWheelRotation() * tickSpacing
      val m = s.getModel()
      s.setValue(minOf(m.getMaximum(), maxOf(v, m.getMinimum())))
    }
    if (slider.getUI() is WindowsSliderUI) {
      slider.setUI(WindowsSnapToTicksDragSliderUI(slider))
    } else {
      slider.setUI(MetalSnapToTicksDragSliderUI())
    }
  }
}

internal class WindowsSnapToTicksDragSliderUI(slider: JSlider) : WindowsSliderUI(slider) {
  override fun createTrackListener(slider: JSlider): TrackListener {
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
        val snappedPos = when {
          xpos <= trackLeft -> trackLeft
          xpos >= trackRight -> trackRight
          else -> {
            offset = 0
            val possibleTickPositions = slider.getMaximum() - slider.getMinimum()
            val hasMinorTick = slider.getMinorTickSpacing() > 0
            val tickSpacing = if (hasMinorTick) slider.getMinorTickSpacing() else slider.getMajorTickSpacing()
            val actualPixelsForOneTick = trackLength * tickSpacing / possibleTickPositions.toFloat()
            val px = xpos - trackLeft
            // (Math.round(px / actualPixelsForOneTick) * actualPixelsForOneTick + .5).toInt() + trackLeft
            ((px / actualPixelsForOneTick).toInt() * actualPixelsForOneTick).roundToInt() + trackLeft
          }
        }
        e.translatePoint(snappedPos - xpos, 0)
        super.mouseDragged(e)
      }
    }
  }
}

internal class MetalSnapToTicksDragSliderUI : MetalSliderUI() {
  override fun createTrackListener(slider: JSlider): TrackListener {
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
        val snappedPos = when {
          xpos <= trackLeft -> trackLeft
          xpos >= trackRight -> trackRight
          else -> {
            offset = 0
            val possibleTickPositions = slider.getMaximum() - slider.getMinimum()
            val hasMinorTick = slider.getMinorTickSpacing() > 0
            val tickSpacing = if (hasMinorTick) slider.getMinorTickSpacing() else slider.getMajorTickSpacing()
            val actualPixelsForOneTick = trackLength * tickSpacing / possibleTickPositions.toFloat()
            val px = xpos - trackLeft
            // (Math.round(px / actualPixelsForOneTick) * actualPixelsForOneTick + .5).toInt() + trackLeft
            ((px / actualPixelsForOneTick).toInt() * actualPixelsForOneTick).roundToInt() + trackLeft
          }
        }
        e.translatePoint(snappedPos - xpos, 0)
        super.mouseDragged(e)
      }
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
