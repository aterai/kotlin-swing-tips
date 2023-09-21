package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/test.png")
  val icon = url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) } ?: MissingIcon()
  val txt = "Mini-size 86Key Japanese Keyboard\n  Model No: DE-SK-86BK\n  SERIAL NO: 0000"
  return JPanel().also {
    it.add(ImageCaptionLabel(txt, icon))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ImageCaptionLabel(caption: String, icon: Icon) : JLabel() {
  private val textArea = object : JTextArea() {
    @Transient private var listener: MouseListener? = null

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = background
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      super.paintComponent(g)
    }

    override fun updateUI() {
      removeMouseListener(listener)
      super.updateUI()
      font = font.deriveFont(11f)
      isOpaque = false
      isEditable = false
      background = Color(0x0, true)
      foreground = Color.WHITE
      border = BorderFactory.createEmptyBorder(2, 4, 4, 4)
      listener = object : MouseAdapter() {
        override fun mouseEntered(e: MouseEvent) {
          dispatchMouseEvent(e)
        }

        override fun mouseExited(e: MouseEvent) {
          dispatchMouseEvent(e)
        }
      }
      addMouseListener(listener)
    }

    // override fun contains(x: Int, y: Int) = false
  }
  private val handler = LabelHandler(textArea)

  init {
    setIcon(icon)
    textArea.text = caption
    add(textArea)
    addMouseListener(handler)
    addHierarchyListener(handler)
  }

  private fun dispatchMouseEvent(e: MouseEvent) {
    val src = e.component
    this.dispatchEvent(SwingUtilities.convertMouseEvent(src, e, this))
  }

  override fun updateUI() {
    super.updateUI()
    border = BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(Color(0xDE_DE_DE)),
      BorderFactory.createLineBorder(Color.WHITE, 4),
    )
    layout = object : OverlayLayout(this) {
      override fun layoutContainer(parent: Container) {
        val componentCount = parent.componentCount
        if (componentCount == 0) {
          return
        }
        val width = parent.width
        val height = parent.height
        val x = 0
        val tah = handler.textAreaHeight
        val c = parent.getComponent(0) // = textArea
        c.setBounds(x, height - tah, width, c.preferredSize.height)
      }
    }
  }
}

private class LabelHandler(private val textArea: Component) : MouseAdapter(), HierarchyListener {
  private val animator = Timer(5) { updateTextAreaLocation() }
  var textAreaHeight = 0
    private set
  private var count = 0
  private var direction = 0

  private fun updateTextAreaLocation() {
    val height = textArea.preferredSize.getHeight()
    val a = AnimationUtils.easeInOut(count / height)
    count += direction
    textAreaHeight = (.5 + a * height).toInt()
    textArea.background = Color(0f, 0f, 0f, (.6 * a).toFloat())
    if (direction > 0) { // show
      if (textAreaHeight >= textArea.preferredSize.height) {
        textAreaHeight = textArea.preferredSize.height
        animator.stop()
      }
    } else { // hide
      if (textAreaHeight <= 0) {
        textAreaHeight = 0
        animator.stop()
      }
    }
    val p = SwingUtilities.getUnwrappedParent(textArea)
    p.revalidate()
    p.repaint()
  }

  override fun mouseEntered(e: MouseEvent) {
    if (animator.isRunning || textAreaHeight == textArea.preferredSize.height) {
      return
    }
    this.direction = 1
    animator.start()
  }

  override fun mouseExited(e: MouseEvent) {
    if (animator.isRunning) {
      return
    }
    val c = e.component
    if (c.contains(e.point) && textAreaHeight == textArea.preferredSize.height) {
      return
    }
    this.direction = -1
    animator.start()
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    if (e.changeFlags.toInt() and HierarchyEvent.DISPLAYABILITY_CHANGED != 0 &&
      !e.component.isDisplayable
    ) {
      animator.stop()
    }
  }
}

private object AnimationUtils {
  private const val N = 3

  fun easeInOut(t: Double): Double {
    val isFirstHalf = t < .5
    return if (isFirstHalf) {
      .5 * intPow(t * 2.0, N)
    } else {
      .5 * (intPow(t * 2.0 - 2.0, N) + 2.0)
    }
  }

  fun intPow(
    base0: Double,
    exp0: Int,
  ): Double {
    require(exp0 >= 0) { "exp0 must be a positive integer or zero" }
    var base = base0
    var exp = exp0
    var result = 1.0
    while (exp > 0) {
      if (exp and 1 != 0) {
        result *= base
      }
      base *= base
      exp = exp ushr 1
    }
    return result
  }
}

private class MissingIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.color = Color.WHITE
    g2.fillRect(x, y, w, h)
    g2.color = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 240

  override fun getIconHeight() = 160
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
