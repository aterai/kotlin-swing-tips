package example

import java.awt.*
import java.awt.geom.Path2D
import java.util.concurrent.TimeUnit
import javax.swing.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

private val root = JPanel(BorderLayout(5, 5))
private val stepperPanel =
  StepperPanel("Order", "Your info", "Payment", "Confirmation")
private val prevButton = JButton("Previous Step")
private val nextButton = JButton("Next Step")
private val statusLabel = JLabel("", SwingConstants.CENTER)
private val orientationCombo = JComboBox(Orientation.entries.toTypedArray())

fun createUI(): Container {
  orientationCombo.setSelectedItem(Orientation.HORIZONTAL)
  statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 16f))

  val controlPanel = JPanel()
  controlPanel.add(orientationCombo)
  controlPanel.add(prevButton)
  controlPanel.add(nextButton)

  prevButton.addActionListener {
    stepperPanel.previousStep()
    updateControls()
  }

  nextButton.addActionListener {
    stepperPanel.nextStep()
    updateControls()
  }

  orientationCombo.addActionListener {
    val index = orientationCombo.getSelectedIndex()
    updateOrientation(orientationCombo.getItemAt(index))
  }

  val contents = JPanel(BorderLayout())
  contents.add(statusLabel)
  contents.add(controlPanel, BorderLayout.SOUTH)

  root.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2))
  root.add(contents)
  updateOrientation(Orientation.HORIZONTAL)
  updateControls()
  return JPanel(GridBagLayout()).also {
    it.add(root)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateOrientation(orientation: Orientation) {
  stepperPanel.setOrientation(orientation)
  val side = if (orientation == Orientation.HORIZONTAL) {
    BorderLayout.NORTH
  } else {
    BorderLayout.WEST
  }
  root.add(stepperPanel, side)
  root.revalidate()
  root.repaint()
}

private fun updateControls() {
  val current = stepperPanel.currentStepIndex
  val total = stepperPanel.stepCount

  prevButton.setEnabled(current > 0)
  nextButton.setEnabled(current < total - 1)

  val label = stepperPanel.getStepLabel(current)
  statusLabel.setText(
    String.format("Current Step: %d / %d (%s)", current + 1, total, label),
  )
}

private enum class StepStatus(
  val foreground: Color,
  val description: String,
) {
  DONE(Color.DARK_GRAY, "completed"),
  CURRENT(StepIcon.COLOR_PRIMARY, "current step"),
  UPCOMING(Color.GRAY, "not completed"),
}

private enum class Orientation {
  HORIZONTAL,
  VERTICAL,
}

private class StepperPanel(
  vararg labels: String,
) : JPanel() {
  private val stepLabels = mutableListOf<JLabel>()
  private val stepIcons = mutableListOf<StepIcon>()
  private val stepTexts = labels.clone()
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()
  private var orientation = Orientation.HORIZONTAL
  private var animatedProgress = 0f
  private var startProgress = 0f
  private var targetProgress = 0f
  private var animStartTime: Long = 0
  private val animTimer = Timer(ANIM_DELAY_MS) { updateAnimation() }

  var currentStepIndex: Int = 0
    private set
  val stepCount: Int
    get() = stepTexts.size

  init {
    buildComponents()
  }

  override fun isOpaque() = false

  fun setOrientation(orientation: Orientation) {
    if (this.orientation != orientation) {
      this.orientation = orientation
      buildComponents()
    }
  }

  private fun buildComponents() {
    removeAll()
    stepLabels.clear()
    stepIcons.clear()

    val horizontal = orientation == Orientation.HORIZONTAL
    val count = stepTexts.size
    setLayout(if (horizontal) GridLayout(1, count) else GridLayout(count, 1))

    val padding = if (horizontal) {
      BorderFactory.createEmptyBorder(5, 5, 5, 5)
    } else {
      BorderFactory.createEmptyBorder(6, 10, 6, 10)
    }
    val alignment = if (horizontal) {
      SwingConstants.CENTER
    } else {
      SwingConstants.LEFT
    }
    val textPosition = if (horizontal) {
      SwingConstants.CENTER
    } else {
      SwingConstants.RIGHT
    }
    val verticalPosition = if (horizontal) {
      SwingConstants.BOTTOM
    } else {
      SwingConstants.CENTER
    }

    for (i in 0..<count) {
      val icon = StepIcon(i + 1, STEP_ICON_SIZE)
      val label = JLabel(stepTexts[i], icon, alignment)
      label.setBorder(padding)
      label.setHorizontalTextPosition(textPosition)
      label.setVerticalTextPosition(verticalPosition)
      stepLabels.add(label)
      stepIcons.add(icon)
      add(label)
    }
    getAccessibleContext().setAccessibleName("Stepper")
    updateStepStates()
    revalidate()
    repaint()
  }

  fun getStepLabel(index: Int) = stepTexts[index]

  fun nextStep() {
    if (currentStepIndex < stepTexts.size - 1) {
      animateToStep(currentStepIndex + 1)
    }
  }

  fun previousStep() {
    if (currentStepIndex > 0) {
      animateToStep(currentStepIndex - 1)
    }
  }

  private fun animateToStep(targetIndex: Int) {
    this.currentStepIndex = targetIndex
    this.startProgress = animatedProgress
    this.targetProgress = targetIndex.toFloat()
    this.animStartTime = System.nanoTime()
    updateStepStates()
    if (!animTimer.isRunning) {
      animTimer.start()
    }
  }

  private fun updateAnimation() {
    val elapsed = System.nanoTime() - animStartTime
    if (elapsed < ANIM_DURATION_NS) {
      val timeFraction = elapsed / ANIM_DURATION_NS.toFloat()
      val easedFraction = 1f - (1.0 - timeFraction).pow(3.0).toFloat()
      animatedProgress =
        startProgress + (targetProgress - startProgress) * easedFraction
    } else {
      animatedProgress = targetProgress
      animTimer.stop()
    }
    repaint()
  }

  private fun updateStepStates() {
    val count = stepLabels.size
    for (i in 0..<count) {
      val status = if (i < currentStepIndex) {
        StepStatus.DONE
      } else if (i == currentStepIndex) {
        StepStatus.CURRENT
      } else {
        StepStatus.UPCOMING
      }
      val label = stepLabels[i]
      stepIcons[i].setStatus(status)
      label.setForeground(status.foreground)
      label.getAccessibleContext().setAccessibleDescription(
        "Step %d of %d, %s".format(i + 1, count, status.description),
      )
    }
    if (count > 0) {
      getAccessibleContext().setAccessibleDescription(
        "Step %d of %d: %s".format(
          currentStepIndex + 1,
          count,
          stepTexts[currentStepIndex],
        ),
      )
    }
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    for (i in 0..<stepLabels.size - 1) {
      val p1 = getIconCenterPoint(stepLabels[i])
      val p2 = getIconCenterPoint(stepLabels[i + 1])

      val fraction = max(0f, min(1f, animatedProgress - i))
      val mx = (p1.x + (p2.x - p1.x) * fraction).roundToInt()
      val my = (p1.y + (p2.y - p1.y) * fraction).roundToInt()

      g2.color = StepIcon.COLOR_PRIMARY
      g2.stroke = SOLID_CONNECTOR
      g2.drawLine(p1.x, p1.y, mx, my)

      g2.color = StepIcon.COLOR_INACTIVE
      g2.stroke = DASHED_CONNECTOR
      g2.drawLine(mx, my, p2.x, p2.y)
    }
    g2.dispose()
  }

  private fun getIconCenterPoint(label: JLabel): Point {
    val i = label.getInsets()
    viewRect.setBounds(
      i.left,
      i.top,
      label.getWidth() - i.left - i.right,
      label.getHeight() - i.top - i.bottom,
    )
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)
    SwingUtilities.layoutCompoundLabel(
      label,
      label.getFontMetrics(label.getFont()),
      label.text,
      label.icon,
      label.verticalAlignment,
      label.horizontalAlignment,
      label.verticalTextPosition,
      label.horizontalTextPosition,
      viewRect,
      iconRect,
      textRect,
      label.iconTextGap,
    )
    return Point(
      label.getX() + iconRect.x + iconRect.width / 2,
      label.getY() + iconRect.y + iconRect.height / 2,
    )
  }

  companion object {
    private const val STEP_ICON_SIZE = 24
    private const val ANIM_DELAY_MS = 16
    private val ANIM_DURATION_NS = TimeUnit.MILLISECONDS.toNanos(250)
    private val SOLID_CONNECTOR = BasicStroke(3f)
    private val DASHED_CONNECTOR = BasicStroke(
      2f,
      BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER,
      10f,
      floatArrayOf(6f, 4f),
      0f,
    )
  }
}

private class StepIcon(
  private val stepNumber: Int,
  private val size: Int,
) : Icon {
  private var status = StepStatus.UPCOMING

  fun setStatus(status: StepStatus) {
    this.status = status
  }

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
    )

    val padding = 2
    val diameter = size - padding * 2
    val circleX = x + padding
    val circleY = y + padding

    when (status) {
      StepStatus.DONE -> {
        g2.color = COLOR_PRIMARY
        g2.fillOval(circleX, circleY, diameter, diameter)
        g2.color = Color.WHITE
        g2.stroke = BasicStroke(
          diameter * .125f,
          BasicStroke.CAP_ROUND,
          BasicStroke.JOIN_ROUND,
        )
        g2.translate(circleX + diameter / 2.0, circleY + diameter / 2.0)
        g2.draw(createCheckMark(diameter))
      }

      StepStatus.CURRENT -> {
        g2.color = COLOR_CURRENT_BG
        g2.fillOval(circleX, circleY, diameter, diameter)
        g2.color = COLOR_PRIMARY
        g2.stroke = BasicStroke(2.5f)
        g2.drawOval(circleX, circleY, diameter, diameter)
        drawCenteredText(g2, circleX, circleY, diameter, COLOR_PRIMARY, true)
      }

      StepStatus.UPCOMING -> {
        g2.color = COLOR_CURRENT_BG
        g2.fillOval(circleX, circleY, diameter, diameter)
        g2.color = COLOR_INACTIVE
        g2.stroke = BasicStroke(1.5f)
        g2.drawOval(circleX, circleY, diameter, diameter)
        drawCenteredText(g2, circleX, circleY, diameter, COLOR_INACTIVE, false)
      }
    }
    g2.dispose()
  }

  private fun drawCenteredText(
    g2: Graphics2D,
    x: Int,
    y: Int,
    diameter: Int,
    color: Color,
    bold: Boolean,
  ) {
    g2.color = color
    val fontSize = size * .48f
    val font = g2.font.deriveFont(if (bold) Font.BOLD else Font.PLAIN, fontSize)
    g2.font = font

    val text = stepNumber.toString()
    val frc = g2.fontRenderContext
    val gv = font.createGlyphVector(frc, text)
    val visualBounds = gv.visualBounds

    val textWidth = visualBounds.width
    val textHeight = visualBounds.height

    val textX = (x + (diameter - textWidth) / 2.0 - visualBounds.x).toFloat()
    val textY = (y + (diameter - textHeight) / 2.0 - visualBounds.y).toFloat()
    g2.drawString(text, textX, textY)
  }

  override fun getIconWidth() = size

  override fun getIconHeight() = size

  companion object {
    val COLOR_PRIMARY = Color(0x21_96_F3)
    val COLOR_INACTIVE = Color(0xD2_D7_DC)
    val COLOR_CURRENT_BG = Color(0xFF_FF_FF)

    private fun createCheckMark(diameter: Int): Shape {
      val path = Path2D.Double()
      path.moveTo(-.25 * diameter, 0.0)
      path.lineTo(-.05 * diameter, .2 * diameter)
      path.lineTo(.25 * diameter, -.2 * diameter)
      return path
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
