package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.geom.Ellipse2D
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val scroll = JScrollPane(makeList())
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeList(): JList<String> {
  val model = DefaultListModel<String>().also {
    it.addElement("asd fas dfa sdf sad fas")
    it.addElement("qwe rqw erq wer qwe rwe qr")
    it.addElement("zxc vzx cbz xcv zxc bzx cbz xcb zxc vzx cbz xbz xcv vzx")
    it.addElement("try urt irt iri")
    it.addElement("jhk ghj kfh jkg hjk hjk")
    it.addElement("bnm, bnm vmv bm, vbm fmv bmn")
    it.addElement("1234123541514354677697808967867895678474567356723456245624")
    it.addElement("qw er qw er rq we tt rt ry tr u")
    it.addElement("tiu tyi tyi tyo iuo")
    it.addElement("hjk lgk ghk ghk")
    it.addElement("zxc vzx cvb vnv bmv bm bm")
  }
  val list = JList(model)
  list.cellRenderer = AnimeListCellRenderer(list)
  return list
}

private class AnimeListCellRenderer<E>(
  val list: JList<E>
) : JPanel(BorderLayout()), ListCellRenderer<E>, HierarchyListener {
  private val iconLabel = AnimeIcon()
  private val marqueeLabel = MarqueeLabel()
  private val animator: Timer
  private var running = false
  private var animateIndex = -1
  private val isAnimatingCell get() = running && animateIndex == list.selectedIndex

  init {
    animator = Timer(80) {
      val i = list.selectedIndex
      if (i >= 0) {
        running = true
        list.repaint(list.getCellBounds(i, i))
      } else {
        running = false
      }
    }
    isOpaque = true
    add(iconLabel, BorderLayout.WEST)
    add(marqueeLabel)
    list.addHierarchyListener(this)
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    if (e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L) {
      if (e.component.isDisplayable) {
        animator.start()
      } else {
        animator.stop()
      }
    }
  }

  override fun getListCellRendererComponent(
    l: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    background = if (isSelected) SELECTED_COLOR else l.background
    marqueeLabel.text = value?.toString() ?: ""
    animateIndex = index
    return this
  }

  private inner class MarqueeLabel : JLabel() {
    private var xx = 0f
    init {
      isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      val r = list.visibleRect
      val cw = r.width - iconLabel.preferredSize.width
      val frc = g2.fontRenderContext
      val gv = font.createGlyphVector(frc, text)
      if (isAnimatingCell && gv.visualBounds.width > cw) {
        val lm = font.getLineMetrics(text, frc)
        val yy = lm.ascent / 2f + gv.visualBounds.y.toFloat()
        g2.drawGlyphVector(gv, cw - xx, height / 2f - yy)
        xx = if (cw + gv.visualBounds.width - xx > 0) xx + 8f else 0f
      } else {
        super.paintComponent(g)
      }
      g2.dispose()
    }
  }

  private inner class AnimeIcon : JComponent() {
    init {
      border = BorderFactory.createEmptyBorder(0, 0, 0, 2)
      isOpaque = false
    }

    private val flipbookFrames = ArrayList<Shape>(
      listOf(
        Ellipse2D.Double(SX + 3 * R, SY + 0 * R, 2 * R, 2 * R),
        Ellipse2D.Double(SX + 5 * R, SY + 1 * R, 2 * R, 2 * R),
        Ellipse2D.Double(SX + 6 * R, SY + 3 * R, 2 * R, 2 * R),
        Ellipse2D.Double(SX + 5 * R, SY + 5 * R, 2 * R, 2 * R),
        Ellipse2D.Double(SX + 3 * R, SY + 6 * R, 2 * R, 2 * R),
        Ellipse2D.Double(SX + 1 * R, SY + 5 * R, 2 * R, 2 * R),
        Ellipse2D.Double(SX + 0 * R, SY + 3 * R, 2 * R, 2 * R),
        Ellipse2D.Double(SX + 1 * R, SY + 1 * R, 2 * R, 2 * R)
      )
    )

    override fun getPreferredSize() = Dimension(ICON_WIDTH + 2, ICON_HEIGHT)

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      if (isAnimatingCell) {
        var alpha = .1f
        for (s in flipbookFrames) {
          g2.paint = makeColor(alpha)
          g2.fill(s)
          alpha += .1f
        }
        java.util.Collections.rotate(flipbookFrames, 1)
      } else {
        g2.paint = Color(0x99_99_99)
        for (s in flipbookFrames) {
          g2.fill(s)
        }
      }
      g2.dispose()
    }
  }

  private fun makeColor(alpha: Float) = Color(.5f, .5f, .5f, alpha)

  companion object {
    private val SELECTED_COLOR = Color(0xE6_E6_FF)
    private const val R = 2.0
    private const val SX = 1.0
    private const val SY = 1.0
    private const val ICON_WIDTH = (R * 8 + SX * 2).toInt()
    private const val ICON_HEIGHT = (R * 8 + SY * 2).toInt()
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
