package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val slider = JSlider(-900, 900, 0)
  slider.majorTickSpacing = 10
  // slider.paintTicks = true
  slider.snapToTicks = true
  slider.paintLabels = true
  updateSliderLabelTable(slider)
  val label = object : JLabel("100%", CENTER) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = d.width.coerceAtLeast(32)
      return d
    }
  }
  slider.addChangeListener {
    val iv = slider.value
    val pct: Int
    if (iv >= 0) {
      pct = 100 + iv
      slider.majorTickSpacing = 1
    } else {
      pct = 100 + iv / 10
      slider.majorTickSpacing = 10
    }
    label.text = "$pct%"
    label.repaint()
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(makeButton(-5, slider))
  box.add(slider)
  box.add(makeButton(+5, slider))
  box.add(label)
  box.add(Box.createHorizontalGlue())

  return JPanel(GridLayout()).also {
    it.add(box)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeButton(stepSize: Int, slider: JSlider): JButton {
  val title = if (stepSize > 0) "+" else "-"
  val button = object : JButton(title) {
    override fun getPreferredSize(): Dimension {
      return Dimension(24, 24)
    }
  }
  button.margin = Insets(4, 4, 4, 4)
  val handler = AutoRepeatHandler(stepSize, slider)
  button.addActionListener(handler)
  button.addMouseListener(handler)
  return button
}

private fun updateSliderLabelTable(slider: JSlider) {
  val labelTable = slider.labelTable
  if (labelTable is Map<*, *>) {
    labelTable.forEach { key, value ->
      if (key is Int && value is JLabel) {
        value.text = getLabel(key, slider)
      }
    }
  }
  slider.labelTable = labelTable // Update LabelTable
}

private fun getLabel(key: Int, slider: JSlider) = when (key) {
  0 -> "100%"
  slider.minimum -> "10%"
  slider.maximum -> "1000%"
  else -> " "
}

private class AutoRepeatHandler(
  private val stepSize: Int,
  private val slider: JSlider
) : MouseAdapter(), ActionListener {
  private val autoRepeatTimer = Timer(60, this)
  private var arrowButton: JButton? = null

  init {
    autoRepeatTimer.initialDelay = 300
  }

  override fun actionPerformed(e: ActionEvent) {
    val o = e.source
    if (o is Timer) {
      val isPressed = arrowButton != null && !arrowButton!!.model.isPressed
      if (isPressed && autoRepeatTimer.isRunning) {
        autoRepeatTimer.stop()
        arrowButton = null
      }
    } else if (o is JButton) {
      arrowButton = o
    }
    val iv = slider.value
    val step = if (iv == 0) {
      if (stepSize > 0) stepSize * 2 else stepSize * 10
    } else if (iv > 0) {
      stepSize * 2
    } else {
      stepSize * 10
    }
    slider.value = iv + step
  }

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e) && e.component.isEnabled) {
      autoRepeatTimer.start()
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    autoRepeatTimer.stop()
    arrowButton = null
  }

  override fun mouseExited(e: MouseEvent) {
    if (autoRepeatTimer.isRunning) {
      autoRepeatTimer.stop()
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
