package example

import java.awt.*
import javax.swing.*
import kotlin.math.abs
import kotlin.math.min

private var score = 0

fun createUI(): Component {
  val odometer = Odometer(5)
  val addButton = JButton("Add Score")
  addButton.addActionListener {
    score += (100..600).random()
    odometer.updateValue(score)
  }

  val resetButton = JButton("Reset")
  resetButton.addActionListener {
    score = 0
    odometer.updateValue(0)
  }

  val buttonPanel = JPanel()
  buttonPanel.setOpaque(false)
  buttonPanel.add(addButton)
  buttonPanel.add(resetButton)

  return JPanel(BorderLayout()).also {
    it.add(odometer)
    it.add(buttonPanel, BorderLayout.SOUTH)
    it.setBackground(Color.BLACK)
    it.setBorder(BorderFactory.createEmptyBorder(20, 5, 5, 5))
    it.preferredSize = Dimension(320, 240)
  }
}

private class Odometer(
  digitCount: Int,
) : JPanel(FlowLayout(FlowLayout.CENTER, 4, 0)) {
  private val wheels = ArrayList<DigitWheel>()

  init {
    repeat(digitCount) {
      val wheel = DigitWheel()
      wheels.add(wheel)
      add(wheel)
    }
  }

  // Updates the displayed value by extracting each digit mathematically
  fun updateValue(value: Int) {
    val isReset = value == 0
    var remainingValue = value
    // Process from right to left (ones place first)
    for (i in wheels.indices.reversed()) {
      val digit = remainingValue % 10
      wheels[i].setTargetDigit(digit, isReset)
      remainingValue /= 10
    }
  }

  override fun isOpaque() = false
}

// A vertical rotating wheel component representing a single digit (0-9)
private class DigitWheel : JComponent() {
  private val animationTimer = Timer(16) { animateScroll() }

  private var currentY = 0.0
  private var targetY = 0.0

  override fun getPreferredSize() = Dimension(55, DIGIT_HEIGHT)

  private fun animateScroll() {
    val delta = targetY - currentY
    val threshold = abs(delta) < .01
    if (threshold) {
      currentY = targetY
      animationTimer.stop()
    } else {
      // Smoothly decelerate as it approaches the target
      val speed = min(.25, .5 + abs(delta) / 2000.0)
      currentY += delta * speed
    }
    repaint()
  }

  fun setTargetDigit(digit: Int, isReset: Boolean) {
    if (isReset) {
      // For reset, normalize coordinates to return to zero via the shortest path
      targetY = (digit * DIGIT_HEIGHT).toDouble()
      currentY %= WHEEL_HEIGHT.toDouble()
      if (currentY < 0) {
        currentY += WHEEL_HEIGHT.toDouble()
      }
    } else {
      val nextTargetY = (digit * DIGIT_HEIGHT).toDouble()
      var normalizedY = currentY % WHEEL_HEIGHT
      if (normalizedY < 0) {
        normalizedY += WHEEL_HEIGHT.toDouble()
      }

      var distance = nextTargetY - normalizedY
      if (distance < 0) {
        // Ensure the wheel always rotates forward (slot machine style)
        distance += WHEEL_HEIGHT.toDouble()
      }
      targetY = currentY + distance
    }

    if (!animationTimer.isRunning) {
      animationTimer.start()
    }
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setClip(0, 0, getWidth(), getHeight())

    // Draw background
    g2.color = Color.DARK_GRAY
    g2.fillRect(0, 0, getWidth(), getHeight())

    g2.font = getFont().deriveFont(Font.BOLD, 55f)
    val fm = g2.fontMetrics

    for (i in 0..9) {
      var posY = i * DIGIT_HEIGHT - currentY % WHEEL_HEIGHT
      // Coordinate correction for looping display
      if (posY < -DIGIT_HEIGHT) {
        posY += WHEEL_HEIGHT.toDouble()
      } else if (posY > WHEEL_HEIGHT - DIGIT_HEIGHT) {
        posY -= WHEEL_HEIGHT.toDouble()
      }

      // Highlight the number when it's near the center
      val distFromCenter = abs(posY)
      g2.color = if (distFromCenter < 10.0) Color.WHITE else Color.GRAY
      val drawX = (getWidth() - fm.stringWidth(i.toString())) / 2
      val drawY = (posY + (DIGIT_HEIGHT + fm.ascent) / 2.0 - 5.0).toInt()
      g2.drawString(i.toString(), drawX, drawY)
    }
    g2.dispose()
  }

  companion object {
    private const val DIGIT_HEIGHT = 80
    private const val WHEEL_HEIGHT = DIGIT_HEIGHT * 10
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
