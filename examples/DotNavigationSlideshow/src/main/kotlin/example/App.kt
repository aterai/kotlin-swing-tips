package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.math.min

private const val TOTAL_IMAGES = 5
private val images = makeDemoImages()
private val animationPanel = AnimationPanel(images[0])
private val dotPanel = JPanel(FlowLayout(FlowLayout.CENTER, 16, 16))
private var currentIndex = 0

fun createUI(): Component {
  val layeredPane = JLayeredPane()
  layeredPane.setLayout(OverlayLayout(layeredPane))
  layeredPane.add(animationPanel, JLayeredPane.DEFAULT_LAYER)

  dotPanel.setOpaque(false)
  setupDots()

  val dotWrapper = JPanel(BorderLayout())
  dotWrapper.setOpaque(false)
  dotWrapper.add(dotPanel, BorderLayout.SOUTH)

  val overlay = JPanel(BorderLayout())
  overlay.setOpaque(false)
  overlay.add(dotWrapper)
  overlay.add(makeArrowButton(true), BorderLayout.WEST)
  overlay.add(makeArrowButton(false), BorderLayout.EAST)

  layeredPane.add(overlay, JLayeredPane.PALETTE_LAYER)

  return JPanel(BorderLayout()).also {
    val handler = NavigateHandler()
    it.addKeyListener(handler)
    it.addMouseWheelListener(handler)
    it.setFocusable(true)
    it.add(layeredPane)
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeDemoImages(): List<ImageIcon> {
  val list = mutableListOf<ImageIcon>()
  for (i in 0..<TOTAL_IMAGES) {
    list.add(createDemoImageIcon(i))
  }
  return list
}

private fun makeArrowButton(moveRight: Boolean): JButton {
  val button = HoverArrowButton(moveRight)
  button.addActionListener {
    val totalImages = images.size
    if (moveRight) {
      navigateTo((currentIndex - 1 + totalImages) % totalImages, false)
    } else {
      navigateTo((currentIndex + 1) % totalImages, true)
    }
  }
  return button
}

private fun setupDots() {
  val totalImages = images.size
  val group = ButtonGroup()
  for (i in 0..<totalImages) {
    val dot = makeDotButton(i)
    group.add(dot)
    dotPanel.add(dot)
  }
}

private fun makeDotButton(index: Int): JToggleButton {
  val dot = JToggleButton(DotIcon(), index == 0)
  dot.setBorderPainted(false)
  dot.setContentAreaFilled(false)
  dot.setFocusPainted(false)
  dot.setBorder(BorderFactory.createEmptyBorder())
  dot.setCursor(Cursor(Cursor.HAND_CURSOR))
  dot.addActionListener {
    if (index != currentIndex) {
      navigateTo(index, index > currentIndex)
    }
  }
  return dot
}

private fun navigateTo(index: Int, moveRight: Boolean) {
  val c = dotPanel.getComponent(index)
  if (c is JToggleButton && !animationPanel.isAnimating()) {
    c.setSelected(true)
    val nextImage = images[index]
    currentIndex = index
    animationPanel.startAnimation(nextImage, moveRight)
    animationPanel.requestFocusInWindow()
  }
}

class NavigateHandler :
  KeyAdapter(),
  MouseWheelListener {
  override fun keyPressed(e: KeyEvent) {
    val totalImages = images.size
    val keyCode = e.getKeyCode()
    if (keyCode == KeyEvent.VK_LEFT) {
      navigateTo((currentIndex - 1 + totalImages) % totalImages, false)
    } else if (keyCode == KeyEvent.VK_RIGHT) {
      navigateTo((currentIndex + 1) % totalImages, true)
    }
  }

  override fun mouseWheelMoved(e: MouseWheelEvent) {
    val totalImages = images.size
    val rotation = e.getWheelRotation()
    if (rotation < 0) {
      navigateTo((currentIndex - 1 + totalImages) % totalImages, false)
    } else {
      navigateTo((currentIndex + 1) % totalImages, true)
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
  private val timer: Timer
  private var currentImage: Image?
  private var nextImage: Image? = null
  private var animationProgress = 0.0 // 0.0 to 1.0
  private var moveRight = true

  init {
    this.currentImage = initialImage.getImage()
    val delay = 1000 / FRAME_RATE
    timer = Timer(
      delay,
      object : ActionListener {
        private var startTime: Long = 0

        override fun actionPerformed(e: ActionEvent?) {
          val b0 = animationProgress == 0.0
          if (b0) {
            startTime = System.currentTimeMillis()
            animationProgress = .001 // Start
          } else {
            val elapsed = System.currentTimeMillis() - startTime
            animationProgress = elapsed.toDouble() / DURATION
            val b1 = animationProgress >= 1.0
            if (b1) {
              animationProgress = 1.0
              completeAnimation()
            }
          }
          repaint()
        }
      },
    )
    timer.setInitialDelay(0)
  }

  fun startAnimation(nextImageIcon: ImageIcon, isMoveRight: Boolean) {
    this.nextImage = nextImageIcon.getImage()
    this.moveRight = isMoveRight
    this.animationProgress = 0.0
    timer.start()
  }

  private fun completeAnimation() {
    timer.stop()
    currentImage = nextImage // Swap images
    animationProgress = 0.0
  }

  fun isAnimating() = timer.isRunning

  private fun easeOut(t: Double) = t * (2.0 - t)

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (currentImage == null) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    val width = getWidth()
    val height = getHeight()
    if (isAnimating() && nextImage != null) {
      val easedProgress = easeOut(animationProgress)
      val offsetX = (width * easedProgress).toInt()
      if (moveRight) {
        g2.drawImage(currentImage, -offsetX, 0, width, height, this)
        g2.drawImage(nextImage, width - offsetX, 0, width, height, this)
      } else {
        g2.drawImage(currentImage, offsetX, 0, width, height, this)
        g2.drawImage(nextImage, -width + offsetX, 0, width, height, this)
      }
    } else {
      g2.drawImage(currentImage, 0, 0, width, height, this)
    }
    g2.dispose()
  }

  companion object {
    private const val DURATION = 250 // ms
    private const val FRAME_RATE = 60
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
    g2.color = if (b) SEL_COLOR else DEF_COLOR
    val gap = if (b) 0 else 2
    g2.fillOval(x + gap, y + gap, iconWidth - gap * 2, iconHeight - gap * 2)
    g2.dispose()
  }

  override fun getIconWidth() = 18

  override fun getIconHeight() = 18

  companion object {
    private val SEL_COLOR = Color.WHITE
    private val DEF_COLOR = Color(0x64_FF_FF_FF, true)
  }
}

private class HoverArrowButton(
  private val isLeft: Boolean,
) : JButton() {
  private var isHovered = false
  private var mouseAdapter: MouseAdapter? = null

  override fun updateUI() {
    removeMouseListener(mouseAdapter)
    super.updateUI()
    setContentAreaFilled(false)
    setFocusPainted(false)
    setOpaque(false)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setCursor(Cursor(Cursor.HAND_CURSOR))
    mouseAdapter = object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent?) {
        isHovered = true
        repaint()
      }

      override fun mouseExited(e: MouseEvent?) {
        isHovered = false
        repaint()
      }
    }
    addMouseListener(mouseAdapter)
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.width = 50
    return d
  }

  override fun paintComponent(g: Graphics) {
    if (isHovered) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      val rect = SwingUtilities.calculateInnerArea(this, null)
      val arrowHeight = min(25, rect.height / 2)
      val arrowWidth = arrowHeight / 2
      val centerX = rect.centerX.toInt()
      val centerY = rect.centerY.toInt()
      val startX = centerX - arrowWidth / 2
      val xpt = if (isLeft) { // <
        intArrayOf(startX + arrowWidth, startX, startX + arrowWidth)
      } else { // >
        intArrayOf(startX, startX + arrowWidth, startX)
      }
      val ypt = intArrayOf(
        centerY - arrowHeight / 2,
        centerY,
        centerY + arrowHeight / 2,
      )
      g2.color = Color.WHITE
      g2.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
      g2.drawPolyline(xpt, ypt, 3)
      g2.dispose()
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
