package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.FilteredImageSource
import java.awt.image.ImageFilter
import java.awt.image.ImageProducer
import java.awt.image.RGBImageFilter
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  // http://www.freeiconsdownload.com/Free_Downloads.asp?id=60
  val defaultIcon = ImageIcon(cl.getResource("example/31g.png"))
  val ip = defaultIcon.image.source
  val p = JPanel(GridLayout(2, 2, 4, 4))

  val list1 = listOf(
    makeStarImageIcon(ip, SelectedImageFilter(1f, .5f, .5f)),
    makeStarImageIcon(ip, SelectedImageFilter(.5f, 1f, .5f)),
    makeStarImageIcon(ip, SelectedImageFilter(1f, .5f, 1f)),
    makeStarImageIcon(ip, SelectedImageFilter(.5f, .5f, 1f)),
    makeStarImageIcon(ip, SelectedImageFilter(1f, 1f, .5f))
  )
  p.add(makeStarRatingPanel("gap=0", LevelBar(defaultIcon, list1, 0)))

  val list2 = listOf(
    makeStarImageIcon(ip, SelectedImageFilter(.2f, .5f, .5f)),
    makeStarImageIcon(ip, SelectedImageFilter(0f, 1f, .2f)),
    makeStarImageIcon(ip, SelectedImageFilter(1f, 1f, .2f)),
    makeStarImageIcon(ip, SelectedImageFilter(.8f, .4f, .2f)),
    makeStarImageIcon(ip, SelectedImageFilter(1f, .1f, .1f))
  )
  val p2 = object : LevelBar(defaultIcon, list2, 1) {
    override fun repaintIcon(index: Int) {
      for (i in labelList.indices) {
        labelList[i].icon = if (i <= index) iconList[index] else defaultIcon
      }
      repaint()
    }
  }
  p.add(makeStarRatingPanel("gap=1+1", p2))

  val list3 = listOf(
    makeStarImageIcon(ip, SelectedImageFilter(.6f, .6f, 0f)),
    makeStarImageIcon(ip, SelectedImageFilter(.7f, .7f, 0f)),
    makeStarImageIcon(ip, SelectedImageFilter(.8f, .8f, 0f)),
    makeStarImageIcon(ip, SelectedImageFilter(.9f, .9f, 0f)),
    makeStarImageIcon(ip, SelectedImageFilter(1f, 1f, 0f))
  )
  p.add(makeStarRatingPanel("gap=2+2", LevelBar(defaultIcon, list3, 2)))

  val star = makeStarImageIcon(ip, SelectedImageFilter(1f, 1f, 0f))
  val list4 = listOf(star, star, star, star, star)
  p.add(makeStarRatingPanel("gap=1+1", LevelBar(defaultIcon, list4, 1)))

  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeStarRatingPanel(title: String, label: LevelBar) = JPanel(FlowLayout(FlowLayout.LEFT)).also {
  val button = JButton("clear")
  button.addActionListener { label.clear() }
  it.border = BorderFactory.createTitledBorder(title)
  it.add(button)
  it.add(label)
}

private fun makeStarImageIcon(ip: ImageProducer, filter: ImageFilter): ImageIcon {
  val img = Toolkit.getDefaultToolkit().createImage(FilteredImageSource(ip, filter))
  return ImageIcon(img)
}

private open class LevelBar(
  private val defaultIcon: ImageIcon,
  protected val iconList: List<ImageIcon>,
  private val gap: Int
) : JPanel(GridLayout(1, 5, gap * 2, gap * 2)) {
  protected val labelList = listOf(
    JLabel(), JLabel(), JLabel(), JLabel(), JLabel()
  )
  private var clicked = -1
  private var handler: MouseAdapter? = null

  init {
    EventQueue.invokeLater {
      for (l in labelList) {
        l.icon = defaultIcon
        add(l)
      }
    }
  }

  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    super.updateUI()
    handler = object : MouseAdapter() {
      override fun mouseMoved(e: MouseEvent) {
        repaintIcon(getSelectedIconIndex(e.point))
      }

      override fun mouseEntered(e: MouseEvent) {
        repaintIcon(getSelectedIconIndex(e.point))
      }

      override fun mouseClicked(e: MouseEvent) {
        clicked = getSelectedIconIndex(e.point)
      }

      override fun mouseExited(e: MouseEvent) {
        repaintIcon(clicked)
      }
    }
    addMouseListener(handler)
    addMouseMotionListener(handler)
  }

  fun clear() {
    clicked = -1
    repaintIcon(clicked)
  }

  private fun getSelectedIconIndex(p: Point): Int {
    for (i in labelList.indices) {
      val r = labelList[i].bounds
      r.grow(gap, gap)
      if (r.contains(p)) {
        return i
      }
    }
    return -1
  }

  open fun repaintIcon(index: Int) {
    for (i in labelList.indices) {
      labelList[i].icon = if (i <= index) iconList[i] else defaultIcon
    }
    repaint()
  }
}

class SelectedImageFilter(rf: Float, gf: Float, bf: Float) : RGBImageFilter() {
  private val rf = rf.coerceIn(0f, 1f)
  private val gf = gf.coerceIn(0f, 1f)
  private val bf = bf.coerceIn(0f, 1f)

  init {
    canFilterIndexColorModel = false
  }

  override fun filterRGB(x: Int, y: Int, argb: Int): Int {
    val r = ((argb shr 16 and 0xFF) * rf).toInt()
    val g = ((argb shr 8 and 0xFF) * gf).toInt()
    val b = ((argb and 0xFF) * bf).toInt()
    // return argb and -0x1000000 or (r shl 16) or (g shl 8) or b
    return argb and 0xFF_00_00_00.toInt() or (r shl 16) or (g shl 8) or b
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
