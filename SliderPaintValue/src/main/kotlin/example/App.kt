package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
  UIManager.put("Slider.onlyLeftMouseButtonDrag", true)

  val slider1 = makeSlider(SwingConstants.HORIZONTAL)
  val d = UIManager.getLookAndFeelDefaults()
  d["Slider.paintValue"] = true
  slider1.putClientProperty("Nimbus.Overrides", d)

  val slider2 = makeSlider(SwingConstants.VERTICAL)
  slider2.putClientProperty("Nimbus.Overrides", d)

  val ma = SliderPopupListener()
  val slider3 = makeSlider(SwingConstants.HORIZONTAL)
  slider3.addMouseMotionListener(ma)
  slider3.addMouseListener(ma)

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("Default", makeSlider(SwingConstants.HORIZONTAL)))
  box.add(makeTitledPanel("Slider.paintValue", slider1))
  box.add(makeTitledPanel("Show ToolTip", slider3))
  box.add(Box.createVerticalGlue())
  EventQueue.invokeLater {
    box.revalidate()
    box.repaint()
  }

  val p = Box.createHorizontalBox()
  p.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
  p.add(box)
  p.add(slider2)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSlider(orientation: Int) = JSlider(orientation).also {
  it.paintTrack = true
  it.paintLabels = false
  it.paintTicks = true
  it.majorTickSpacing = 10
  it.minorTickSpacing = 5
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class SliderPopupListener : MouseAdapter() {
  private val toolTip = JWindow()
  private val label = object : JLabel(" ", CENTER) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 32
      return d
    }
  }

  init {
    label.border = BorderFactory.createLineBorder(Color.DARK_GRAY)
    toolTip.add(label)
    toolTip.pack()
  }

  fun updateToolTip(e: MouseEvent) {
    val slider = e.component as? JSlider ?: return
    label.text = "%03d".format(slider.value)
    val pt = e.point
    pt.y = SwingUtilities.calculateInnerArea(slider, null).centerY.toInt()
    SwingUtilities.convertPointToScreen(pt, e.component)
    val h2 = slider.preferredSize.height / 2
    val d = label.preferredSize
    pt.translate(-d.width / 2, -d.height - h2)
    toolTip.location = pt
  }

  override fun mouseDragged(e: MouseEvent) {
    updateToolTip(e)
  }

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      toolTip.isVisible = true
      updateToolTip(e)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    toolTip.isVisible = false
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
