package example

import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.math.min

private const val TOTAL_IMAGES = 5
private val images = createDemoImages()
private val dots = mutableListOf<JToggleButton>()
private val mainPanel = JPanel(BorderLayout())
private val animationPanel = AnimationPanel(images[0])
private val dotPanel = JPanel(FlowLayout(FlowLayout.CENTER, 16, 16))
private var currentIndex = 0

fun createUI(): Component {
  dotPanel.setOpaque(false)
  setupDots()

  val dotWrapper = JPanel(BorderLayout())
  dotWrapper.setOpaque(false)
  dotWrapper.add(dotPanel, BorderLayout.SOUTH)

  val overlay = JPanel(BorderLayout())
  overlay.setOpaque(false)
  overlay.add(dotWrapper)
  overlay.add(createArrowButton(true), BorderLayout.WEST)
  overlay.add(createArrowButton(false), BorderLayout.EAST)

  val layeredPane = JLayeredPane()
  layeredPane.setLayout(OverlayLayout(layeredPane))
  // Kotlin resolves add(Component, Integer) to add(Component, int index),
  // so the layer must be set explicitly
  layeredPane.setLayer(animationPanel, JLayeredPane.DEFAULT_LAYER)
  layeredPane.add(animationPanel)
  layeredPane.setLayer(overlay, JLayeredPane.PALETTE_LAYER)
  layeredPane.add(overlay)

  return mainPanel.also {
    val handler = NavigateHandler()
    it.addKeyListener(handler)
    it.addMouseWheelListener(handler)
    it.setFocusable(true)
    it.add(layeredPane)
    it.preferredSize = Dimension(320, 240)
  }
}

fun createDemoImages(): List<ImageIcon> {
  val list = mutableListOf<ImageIcon>()
  for (i in 0..<TOTAL_IMAGES) {
    list.add(createDemoImageIcon(i))
  }
  return list
}

private fun createArrowButton(left: Boolean): JButton {
  val button = HoverArrowButton(left)
  button.addActionListener {
    if (left) {
      navigatePrevious()
    } else {
      navigateNext()
    }
  }
  return button
}

private fun setupDots() {
  val totalImages = images.size
  val group = ButtonGroup()
  for (i in 0..<totalImages) {
    val dot = createDotButton(i)
    group.add(dot)
    dots.add(dot)
    dotPanel.add(dot)
  }
}

private fun createDotButton(index: Int): JToggleButton {
  val dot = JToggleButton(DotIcon(), index == 0)
  dot.setBorderPainted(false)
  dot.setContentAreaFilled(false)
  dot.setFocusPainted(false)
  dot.setBorder(BorderFactory.createEmptyBorder())
  dot.setCursor(Cursor(Cursor.HAND_CURSOR))
  dot.addActionListener { navigateTo(index, index > currentIndex) }
  return dot
}

private fun navigatePrevious() {
  val totalImages = images.size
  navigateTo((currentIndex - 1 + totalImages) % totalImages, false)
}

private fun navigateNext() {
  navigateTo((currentIndex + 1) % images.size, true)
}

private fun navigateTo(index: Int, forward: Boolean) {
  mainPanel.requestFocusInWindow()
  if (index != currentIndex && !animationPanel.isAnimating) {
    currentIndex = index
    animationPanel.startAnimation(images[index], forward)
  }
  // A clicked dot is already selected by the ButtonGroup, so the selection
  // must be restored when the navigation is ignored during the animation
  dots[currentIndex].setSelected(true)
}

class NavigateHandler :
  KeyAdapter(),
  MouseWheelListener {
  override fun keyPressed(e: KeyEvent) {
    val keyCode = e.getKeyCode()
    if (keyCode == KeyEvent.VK_LEFT) {
      navigatePrevious()
    } else if (keyCode == KeyEvent.VK_RIGHT) {
      navigateNext()
    }
  }

  override fun mouseWheelMoved(e: MouseWheelEvent) {
    // A high-resolution wheel may report a rotation of 0
    val rotation = e.getWheelRotation()
    if (rotation < 0) {
      navigatePrevious()
    } else if (rotation > 0) {
      navigateNext()
    }
  }
}

private fun createDemoImageIcon(index: Int): ImageIcon {
  val width = 800
  val height = 600
  val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
  val g2 = img.createGraphics()
  g2.setRenderingHint(
    RenderingHints.KEY_ANTIALIASING,
    RenderingHints.VALUE_ANTIALIAS_ON,
  )

  g2.color = Color.getHSBColor(index.toFloat() / TOTAL_IMAGES, .5f, .8f)
  g2.fillRect(0, 0, width, height)

  g2.color = Color.WHITE
  g2.font = g2.font.deriveFont(80f)
  val text = "Slide " + (index + 1)
  val fm = g2.fontMetrics
  val x = (width - fm.stringWidth(text)) / 2
  val y = (height - fm.height) / 2 + fm.ascent
  g2.drawString(text, x, y)
  g2.dispose()
  return ImageIcon(img)
}

private class AnimationPanel(
  initialImage: ImageIcon,
) : JPanel() {
  private val timer = Timer(1000 / FRAME_RATE) { updateAnimation() }
  private var currentImage: Image = initialImage.getImage()
  private var nextImage: Image = currentImage
  private var animationProgress = 0.0 // 0.0 to 1.0
  private var slideForward = true
  private var startTime = 0L
  val isAnimating: Boolean
    get() = timer.isRunning

  init {
    timer.setInitialDelay(0)
  }

  fun startAnimation(nextImageIcon: ImageIcon, forward: Boolean) {
    this.nextImage = nextImageIcon.getImage()
    this.slideForward = forward
    this.animationProgress = 0.0
    this.startTime = System.currentTimeMillis()
    timer.start()
  }

  private fun updateAnimation() {
    val elapsed = System.currentTimeMillis() - startTime
    animationProgress = elapsed.toDouble() / DURATION
    if (elapsed >= DURATION) {
      completeAnimation()
    }
    repaint()
  }

  private fun completeAnimation() {
    timer.stop()
    currentImage = nextImage // Swap images
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    val width = getWidth()
    val height = getHeight()
    if (isAnimating) {
      val easedProgress = easeOut(animationProgress)
      val offsetX = (width * easedProgress).toInt()
      if (slideForward) {
        // Slide to left (new image comes from right)
        g2.drawImage(currentImage, -offsetX, 0, width, height, this)
        g2.drawImage(nextImage, width - offsetX, 0, width, height, this)
      } else {
        // Slide to right (new image comes from left)
        g2.drawImage(currentImage, offsetX, 0, width, height, this)
        g2.drawImage(nextImage, -width + offsetX, 0, width, height, this)
      }
    } else {
      // Static state
      g2.drawImage(currentImage, 0, 0, width, height, this)
    }
    g2.dispose()
  }

  companion object {
    private const val DURATION = 250 // ms
    private const val FRAME_RATE = 60

    private fun easeOut(t: Double) = t * (2.0 - t)
  }
}

private class DotIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val b = (c as? AbstractButton)?.model?.isSelected == true
    g2.color = if (b) SELECTED_COLOR else DEFAULT_COLOR
    val gap = if (b) 0 else 2
    g2.fillOval(x + gap, y + gap, iconWidth - gap * 2, iconHeight - gap * 2)
    g2.dispose()
  }

  override fun getIconWidth() = 18

  override fun getIconHeight() = 18

  companion object {
    private val SELECTED_COLOR = Color.WHITE
    private val DEFAULT_COLOR = Color(0x64_FF_FF_FF, true)
  }
}

private class HoverArrowButton(
  private val left: Boolean,
) : JButton() {
  override fun updateUI() {
    super.updateUI()
    setContentAreaFilled(false)
    setFocusPainted(false)
    setOpaque(false)
    setRolloverEnabled(true)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setCursor(Cursor(Cursor.HAND_CURSOR))
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.width = 50
    return d
  }

  override fun paintComponent(g: Graphics) {
    if (getModel().isRollover) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.color = Color.WHITE
      g2.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
      val rect = SwingUtilities.calculateInnerArea(this, null)
      g2.draw(createArrowPath(rect, left))
      g2.dispose()
    }
  }

  private fun createArrowPath(rect: Rectangle, leftArrow: Boolean): Path2D {
    val arrowHeight = min(25.0, rect.height / 2.0)
    val arrowWidth = arrowHeight / 2.0
    val centerX = rect.centerX
    val centerY = rect.centerY
    val startX = centerX - (arrowWidth / 2.0)

    val path = Path2D.Double()
    if (leftArrow) { // <
      path.moveTo(startX + arrowWidth, centerY - arrowHeight / 2.0)
      path.lineTo(startX, centerY)
      path.lineTo(startX + arrowWidth, centerY + arrowHeight / 2.0)
    } else { // >
      path.moveTo(startX, centerY - arrowHeight / 2.0)
      path.lineTo(startX + arrowWidth, centerY)
      path.lineTo(startX, centerY + arrowHeight / 2.0)
    }
    return path
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
