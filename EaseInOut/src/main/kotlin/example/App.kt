package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel().also {
  val txt = "Mini-size 86Key Japanese Keyboard\n  Model No: DE-SK-86BK\n  SERIAL NO: 00000000"
  val cl = Thread.currentThread().contextClassLoader
  val icon = ImageIcon(cl.getResource("example/test.png"))
  it.add(ImageCaptionLabel(txt, icon))
  it.preferredSize = Dimension(320, 240)
}

private class ImageCaptionLabel(caption: String, icon: Icon) : JLabel() {
  private val textArea = object : JTextArea() {
    @Transient
    private var listener: MouseListener? = null

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as Graphics2D
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
  @Transient private val handler = LabelHandler(textArea)

  private fun dispatchMouseEvent(e: MouseEvent) {
    val src = e.component
    this.dispatchEvent(SwingUtilities.convertMouseEvent(src, e, this))
  }

  override fun updateUI() {
    super.updateUI()
    border = BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(Color(0xDE_DE_DE)),
      BorderFactory.createLineBorder(Color.WHITE, 4)
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

  init {
    setIcon(icon)
    textArea.text = caption
    add(textArea)
    addMouseListener(handler)
    addHierarchyListener(handler)
  }
}

internal class LabelHandler(private val textArea: Component) : MouseAdapter(), HierarchyListener {
  private val animator = Timer(5) { updateTextAreaLocation() }
  var textAreaHeight = 0
    private set
  private var count = 0
  private var direction = 0

  private fun updateTextAreaLocation() {
    val height = textArea.preferredSize.getHeight()
    val a = AnimationUtil.easeInOut(count / height)
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

private object AnimationUtil {
  private const val N = 3

  // http://www.anima-entertainment.de/math-easein-easeout-easeinout-and-bezier-curves
  // Math: EaseIn EaseOut, EaseInOut and Bezier Curves | Anima Entertainment GmbH
  // fun easeIn(t: Double) = t.pow(N.toDouble()) // range: 0.0 <= t <= 1.0

  // fun easeOut(t: Double) = (t - 1.0).pow(N.toDouble()) + 1.0

  fun easeInOut(t: Double): Double {
    val isFirstHalf = t < .5
    return if (isFirstHalf) {
      .5 * intPow(t * 2.0, N)
    } else {
      .5 * (intPow(t * 2.0 - 2.0, N) + 2.0)
    }
  }

  // http://d.hatena.ne.jp/pcl/20120617/p1
  // http://d.hatena.ne.jp/rexpit/20110328/1301305266
  // http://c2.com/cgi/wiki?IntegerPowerAlgorithm
  // http://www.osix.net/modules/article/?id=696
  fun intPow(da: Double, ib: Int): Double {
    var b = ib
    require(b >= 0) { "B must be a positive integer or zero" }
    var a = da
    var d = 1.0
    while (b > 0) {
      if (b and 1 != 0) {
        d *= a
      }
      a *= a
      b = b ushr 1
    }
    return d
  }
} /* Singleton */

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
