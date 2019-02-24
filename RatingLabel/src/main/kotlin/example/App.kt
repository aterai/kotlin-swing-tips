package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.image.FilteredImageSource
import java.awt.image.ImageFilter
import java.awt.image.ImageProducer
import java.awt.image.RGBImageFilter
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(GridLayout(2, 2, 4, 4)) {
  init {
    // PI Diagona Icons Pack 1.0 - Download Royalty Free Icons and Stock Images For Web & Graphics Design
    // http://www.freeiconsdownload.com/Free_Downloads.asp?id=60
    val defaultIcon = ImageIcon(javaClass.getResource("31g.png"))
    val ip = defaultIcon.getImage().getSource()

    val list1 = listOf(
      makeStarImageIcon(ip, SelectedImageFilter(1f, .5f, .5f)),
      makeStarImageIcon(ip, SelectedImageFilter(.5f, 1f, .5f)),
      makeStarImageIcon(ip, SelectedImageFilter(1f, .5f, 1f)),
      makeStarImageIcon(ip, SelectedImageFilter(.5f, .5f, 1f)),
      makeStarImageIcon(ip, SelectedImageFilter(1f, 1f, .5f))
    )
    add(makeStarRatingPanel("gap=0", LevelBar(defaultIcon, list1, 0)))

    val list2 = listOf(
      makeStarImageIcon(ip, SelectedImageFilter(.2f, .5f, .5f)),
      makeStarImageIcon(ip, SelectedImageFilter(0f, 1f, .2f)),
      makeStarImageIcon(ip, SelectedImageFilter(1f, 1f, .2f)),
      makeStarImageIcon(ip, SelectedImageFilter(.8f, .4f, .2f)),
      makeStarImageIcon(ip, SelectedImageFilter(1f, .1f, .1f))
    )
    add(makeStarRatingPanel("gap=1+1", object : LevelBar(defaultIcon, list2, 1) {
      override fun repaintIcon(index: Int) {
        for (i in labelList.indices) {
          labelList.get(i).setIcon(if (i <= index) iconList.get(index) else defaultIcon)
        }
        repaint()
      }
    }))

    val list3 = listOf(
      makeStarImageIcon(ip, SelectedImageFilter(.6f, .6f, 0f)),
      makeStarImageIcon(ip, SelectedImageFilter(.7f, .7f, 0f)),
      makeStarImageIcon(ip, SelectedImageFilter(.8f, .8f, 0f)),
      makeStarImageIcon(ip, SelectedImageFilter(.9f, .9f, 0f)),
      makeStarImageIcon(ip, SelectedImageFilter(1f, 1f, 0f))
    )
    add(makeStarRatingPanel("gap=2+2", LevelBar(defaultIcon, list3, 2)))

    val star = makeStarImageIcon(ip, SelectedImageFilter(1f, 1f, 0f))
    val list4 = listOf(star, star, star, star, star)
    add(makeStarRatingPanel("gap=1+1", LevelBar(defaultIcon, list4, 1)))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeStarRatingPanel(title: String, label: LevelBar): JPanel {
    val button = JButton("clear")
    button.addActionListener { label.clear() }

    val p = JPanel(FlowLayout(FlowLayout.LEFT))
    p.setBorder(BorderFactory.createTitledBorder(title))
    p.add(button)
    p.add(label)
    return p
  }

  private fun makeStarImageIcon(ip: ImageProducer, filter: ImageFilter): ImageIcon {
    val img = Toolkit.getDefaultToolkit().createImage(FilteredImageSource(ip, filter))
    return ImageIcon(img)
  }
}

open class LevelBar(
  protected val defaultIcon: ImageIcon,
  protected val iconList: List<ImageIcon>,
  private val gap: Int
) : JPanel(GridLayout(1, 5, gap * 2, gap * 2)), MouseListener, MouseMotionListener {
  protected val labelList = listOf(
    JLabel(), JLabel(), JLabel(), JLabel(), JLabel()
  )
  private var clicked = -1

  var level: Int
    get() = clicked
    set(l) {
      clicked = l
      repaintIcon(clicked)
    }

  init {
    for (l in labelList) {
      l.setIcon(defaultIcon)
      add(l)
    }
    addMouseListener(this)
    addMouseMotionListener(this)
  }

  fun clear() {
    clicked = -1
    repaintIcon(clicked)
  }

  private fun getSelectedIconIndex(p: Point): Int {
    for (i in labelList.indices) {
      val r = labelList.get(i).getBounds()
      r.grow(gap, gap)
      if (r.contains(p)) {
        return i
      }
    }
    return -1
  }

  open fun repaintIcon(index: Int) {
    for (i in labelList.indices) {
      labelList.get(i).setIcon(if (i <= index) iconList[i] else defaultIcon)
    }
    repaint()
  }

  override fun mouseMoved(e: MouseEvent) {
    repaintIcon(getSelectedIconIndex(e.getPoint()))
  }

  override fun mouseEntered(e: MouseEvent) {
    repaintIcon(getSelectedIconIndex(e.getPoint()))
  }

  override fun mouseClicked(e: MouseEvent) {
    clicked = getSelectedIconIndex(e.getPoint())
  }

  override fun mouseExited(e: MouseEvent) {
    repaintIcon(clicked)
  }

  override fun mouseDragged(e: MouseEvent) { /* not needed */
  }

  override fun mousePressed(e: MouseEvent) { /* not needed */
  }

  override fun mouseReleased(e: MouseEvent) { /* not needed */
  }
}

internal class SelectedImageFilter(rf: Float, gf: Float, bf: Float) : RGBImageFilter() {
  private val rf: Float
  private val gf: Float
  private val bf: Float

  init {
    this.rf = Math.min(1f, rf)
    this.gf = Math.min(1f, gf)
    this.bf = Math.min(1f, bf)
    canFilterIndexColorModel = false
  }

  override fun filterRGB(x: Int, y: Int, argb: Int): Int {
    val r = ((argb shr 16 and 0xFF) * rf).toInt()
    val g = ((argb shr 8 and 0xFF) * gf).toInt()
    val b = ((argb and 0xFF) * bf).toInt()
    // return (argb.toUInt() and 0xFF_00_00_00.toUInt()).toInt() or (r shl 16) or (g shl 8) or b
    return argb and -0x1000000 or (r shl 16) or (g shl 8) or b
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
