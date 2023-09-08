package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.Timer

fun makeUI(): Component {
  val image = makeImageIcon("example/test.png", Dimension(240, 180))
  val toolBar = makeTranslucientToolBar()
  val label = LabelWithToolBox(image, toolBar)
  label.add(toolBar)
  label.border = BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(Color(0xDE_DE_DE)),
    BorderFactory.createLineBorder(Color.WHITE, 4),
  )
  return JPanel().also {
    it.add(label)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTranslucientToolBar(): JToolBar {
  val toolBar = object : JToolBar() {
    private var listener: MouseListener? = null

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
      listener = ParentDispatchMouseListener()
      addMouseListener(listener)
      isFloatable = false
      isOpaque = false
      background = Color(0x0, true)
      foreground = Color.WHITE
      border = BorderFactory.createEmptyBorder(2, 4, 4, 4)
    }
  }
  toolBar.add(Box.createGlue())
  // https://www.deviantart.com/chrfb/art/ecqlipse-2-PNG-59941546
  toolBar.add(makeToolButton("example/ATTACHMENT_16x16-32.png"))
  toolBar.add(Box.createHorizontalStrut(2))
  toolBar.add(makeToolButton("example/RECYCLE BIN - EMPTY_16x16-32.png"))
  return toolBar
}

private class LabelWithToolBox(image: Icon?, toolBox: JToolBar) : JLabel(image) {
  private val delay = 8
  private val animator = Timer(delay, null)
  private var handler: ToolBoxHandler? = null
  private var isHidden = false
  private var counter = 0
  private var yy = 0

  init {
    animator.addActionListener {
      val height = toolBox.preferredSize.height
      val h = height.toDouble()
      if (isHidden) {
        val a = AnimationUtils.easeInOut(++counter / h)
        yy = (.5 + a * h).toInt()
        toolBox.background = Color(0f, 0f, 0f, (.6 * a).toFloat())
        if (yy >= height) {
          yy = height
          animator.stop()
        }
      } else {
        val a = AnimationUtils.easeInOut(--counter / h)
        yy = (.5 + a * h).toInt()
        toolBox.background = Color(0f, 0f, 0f, (.6 * a).toFloat())
        if (yy <= 0) {
          yy = 0
          animator.stop()
        }
      }
      toolBox.revalidate()
    }
  }

  override fun updateUI() {
    removeMouseListener(handler)
    addHierarchyListener(handler)
    super.updateUI()
    layout = object : OverlayLayout(this) {
      override fun layoutContainer(parent: Container) {
        if (parent.componentCount == 0) {
          return
        }
        val width = parent.width
        val height = parent.height
        val x = 0
        val c = parent.getComponent(0)
        c.setBounds(x, height - yy, width, c.preferredSize.height)
      }
    }
    handler = ToolBoxHandler()
    addMouseListener(handler)
    addHierarchyListener(handler)
  }

  private inner class ToolBoxHandler : MouseAdapter(), HierarchyListener {
    override fun mouseEntered(e: MouseEvent) {
      if (!animator.isRunning) {
        isHidden = true
        animator.start()
      }
    }

    override fun mouseExited(e: MouseEvent) {
      if (!contains(e.point)) {
        isHidden = false
        animator.start()
      }
    }

    override fun hierarchyChanged(e: HierarchyEvent) {
      val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
      if (b && !e.component.isDisplayable) {
        animator.stop()
      }
    }
  }
}

private fun makeToolButton(name: String): JButton {
  val icon = makeImageIcon(name, Dimension(16, 16))
  val b = JButton()
  b.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
  b.icon = makeRolloverIcon(icon)
  b.rolloverIcon = icon
  b.isContentAreaFilled = false
  b.isFocusPainted = false
  b.isFocusable = false
  b.toolTipText = name
  return b
}

private fun makeRolloverIcon(srcIcon: ImageIcon): ImageIcon {
  val w = srcIcon.iconWidth
  val h = srcIcon.iconHeight
  val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = img.createGraphics()
  srcIcon.paintIcon(null, g2, 0, 0)
  val scaleFactors = floatArrayOf(.5f, .5f, .5f, 1f)
  val offsets = floatArrayOf(0f, 0f, 0f, 0f)
  val op = RescaleOp(scaleFactors, offsets, g2.renderingHints)
  g2.dispose()
  return ImageIcon(op.filter(img, null))
}

private fun makeImageIcon(path: String, d: Dimension): ImageIcon {
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val img = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage(d)
  return ImageIcon(img)
}

private fun makeMissingImage(d: Dimension): BufferedImage {
  val missingIcon = UIManager.getIcon("html.missingImage")
  val iw = missingIcon.iconWidth
  val ih = missingIcon.iconHeight
  val bi = BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, (d.width - iw) / 2, (d.height - ih) / 2)
  g2.dispose()
  return bi
}

private class ParentDispatchMouseListener : MouseAdapter() {
  override fun mouseEntered(e: MouseEvent) {
    dispatchMouseEvent(e)
  }

  override fun mouseExited(e: MouseEvent) {
    dispatchMouseEvent(e)
  }

  private fun dispatchMouseEvent(e: MouseEvent) {
    val src = e.component
    SwingUtilities.getUnwrappedParent(src)?.also {
      it.dispatchEvent(SwingUtilities.convertMouseEvent(src, e, it))
    }
  }
}

private object AnimationUtils {
  private const val N = 3

  fun easeInOut(t: Double): Double {
    val ret: Double
    val isFirstHalf = t < .5
    ret = if (isFirstHalf) {
      .5 * intPow(t * 2.0)
    } else {
      .5 * (intPow(t * 2.0 - 2.0) + 2.0)
    }
    return ret
  }

  private fun intPow(da: Double): Double {
    var b = N
    require(b >= 0) {
      "B must be a positive integer or zero"
    }
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
