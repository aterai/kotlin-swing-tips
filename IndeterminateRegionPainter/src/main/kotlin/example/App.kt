package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.plaf.nimbus.AbstractRegionPainter

private var worker: SwingWorker<String, Unit?>? = null

fun makeUI(): Component {
  val model = DefaultBoundedRangeModel()
  val progressBar0 = JProgressBar(model)
  val progressBar1 = JProgressBar(model)
  val d = UIDefaults()
  d["ProgressBar[Enabled+Indeterminate].foregroundPainter"] = IndeterminateRegionPainter()
  progressBar1.putClientProperty("Nimbus.Overrides", d)

  val p = JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("Default", progressBar0))
    it.add(makeTitledPanel("ProgressBar[Indeterminate].foregroundPainter", progressBar1))
  }

  val button = JButton("Test start")
  button.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
    progressBar0.isIndeterminate = true
    progressBar1.isIndeterminate = true
    worker = BackgroundTask().also {
      it.addPropertyChangeListener(ProgressListener(progressBar0))
      it.addPropertyChangeListener(ProgressListener(progressBar1))
      it.execute()
    }
  }
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(button)
  box.add(Box.createHorizontalStrut(5))

  return JPanel(BorderLayout()).also {
    it.addHierarchyListener { e ->
      val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
      if (b && !e.component.isDisplayable) {
        worker?.cancel(true)
        worker = null
      }
    }
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  cmp: Component,
): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  p.add(cmp, c)
  return p
}

private class BackgroundTask : SwingWorker<String, Unit?>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    Thread.sleep(5000)
    var current = 0
    val lengthOfTask = 100
    while (current <= lengthOfTask && !isCancelled) {
      doSomething()
      progress = 100 * current / lengthOfTask
      current++
    }
    return "Done"
  }

  @Throws(InterruptedException::class)
  fun doSomething() {
    Thread.sleep(50)
  }
}

private class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
  init {
    progressBar.value = 0
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    if ("progress" == e.propertyName) {
      progressBar.isIndeterminate = false
      val progress = e.newValue as? Int ?: 0
      progressBar.value = progress
    }
  }
}

private class IndeterminateRegionPainter : AbstractRegionPainter() {
  private val color17 = decodeColor(NIMBUS_ORANGE, .0000000000f, .00000000f, .0000000000f, -156)
  private val color18 = decodeColor(NIMBUS_ORANGE, -.0157965120f, .02094239f, -.1529411700f, 0)
  private val color19 = decodeColor(NIMBUS_ORANGE, -.0043216050f, .02094239f, -.0745098000f, 0)
  private val color20 = decodeColor(NIMBUS_ORANGE, -.0080213990f, .02094239f, -.1019607800f, 0)
  private val color21 = decodeColor(NIMBUS_ORANGE, -.0117069040f, -.17905760f, -.0235294100f, 0)
  private val color22 = decodeColor(NIMBUS_ORANGE, -.0486912540f, .02094239f, -.3019608000f, 0)
  private val color23 = decodeColor(NIMBUS_ORANGE, .0039403290f, -.73753220f, .1764705800f, 0)
  private val color24 = decodeColor(NIMBUS_ORANGE, .0055067390f, -.46764207f, .1098039150f, 0)
  private val color25 = decodeColor(NIMBUS_ORANGE, .0042127445f, -.18595415f, .0470588200f, 0)
  private val color26 = decodeColor(NIMBUS_ORANGE, .0047626942f, .02094239f, .0039215684f, 0)
  private val color27 = decodeColor(NIMBUS_ORANGE, .0047626942f, -.15147138f, .1607843000f, 0)
  private val color28 = decodeColor(NIMBUS_ORANGE, .0106654760f, -.27317524f, .2509803800f, 0)
  private val ctx = PaintContext(Insets(5, 5, 5, 5), Dimension(29, 19), false)
  private var rect: Rectangle2D = Rectangle2D.Float()
  private var path: Path2D = Path2D.Float()

  public override fun doPaint(
    g: Graphics2D,
    c: JComponent,
    width: Int,
    height: Int,
    extendedCacheKeys: Array<Any>?,
  ) {
    path = decodePath1()
    g.paint = color17
    g.fill(path)
    rect = decodeRect3()
    g.paint = decodeGradient5(rect)
    g.fill(rect)
    rect = decodeRect4()
    g.paint = decodeGradient6(rect)
    g.fill(rect)
  }

  public override fun getPaintContext() = ctx

  private fun decodePath1(): Path2D {
    path.reset()
    path.moveTo(decodeX(1f).toDouble(), decodeY(.21111111f).toDouble())
    path.curveTo(
      decodeAnchorX(1f, -2f).toDouble(),
      decodeAnchorY(.21111111f, 0f).toDouble(),
      decodeAnchorX(.21111111f, 0f).toDouble(),
      decodeAnchorY(1f, -2f).toDouble(),
      decodeX(.21111111f).toDouble(),
      decodeY(1f).toDouble(),
    )
    path.curveTo(
      decodeAnchorX(.21111111f, 0f).toDouble(),
      decodeAnchorY(1f, 2f).toDouble(),
      decodeAnchorX(.21111111f, 0f).toDouble(),
      decodeAnchorY(2f, -2f).toDouble(),
      decodeX(.21111111f).toDouble(),
      decodeY(2f).toDouble(),
    )
    path.curveTo(
      decodeAnchorX(.21111111f, 0f).toDouble(),
      decodeAnchorY(2f, 2f).toDouble(),
      decodeAnchorX(1f, -2f).toDouble(),
      decodeAnchorY(2.8222225f, 0f).toDouble(),
      decodeX(1f).toDouble(),
      decodeY(2.8222225f).toDouble(),
    )
    path.curveTo(
      decodeAnchorX(1f, 2f).toDouble(),
      decodeAnchorY(2.8222225f, 0f).toDouble(),
      decodeAnchorX(3f, 0f).toDouble(),
      decodeAnchorY(2.8222225f, 0f).toDouble(),
      decodeX(3f).toDouble(),
      decodeY(2.8222225f).toDouble(),
    )
    path.lineTo(decodeX(3f).toDouble(), decodeY(2.3333333f).toDouble())
    path.lineTo(decodeX(.6666667f).toDouble(), decodeY(2.3333333f).toDouble())
    path.lineTo(decodeX(.6666667f).toDouble(), decodeY(.6666667f).toDouble())
    path.lineTo(decodeX(3f).toDouble(), decodeY(.6666667f).toDouble())
    path.lineTo(decodeX(3f).toDouble(), decodeY(.2f).toDouble())
    path.curveTo(
      decodeAnchorX(3f, 0f).toDouble(),
      decodeAnchorY(.2f, 0f).toDouble(),
      decodeAnchorX(1f, 2f).toDouble(),
      decodeAnchorY(.21111111f, 0f).toDouble(),
      decodeX(1f).toDouble(),
      decodeY(.21111111f).toDouble(),
    )
    path.closePath()
    return path
  }

  private fun decodeRect3() = rect.also {
    it.setRect(
      decodeX(.4f).toDouble(),
      decodeY(.4f).toDouble(),
      (decodeX(3f) - decodeX(.4f)).toDouble(),
      (decodeY(2.6f) - decodeY(.4f)).toDouble(),
    )
  }

  private fun decodeRect4() = rect.also {
    it.setRect(
      decodeX(.6f).toDouble(),
      decodeY(.6f).toDouble(),
      (decodeX(2.8f) - decodeX(.6f)).toDouble(),
      (decodeY(2.4f) - decodeY(.6f)).toDouble(),
    )
  }

  private fun decodeGradient5(s: Shape): Paint {
    val bounds = s.bounds2D
    val x = bounds.x.toFloat()
    val y = bounds.y.toFloat()
    val w = bounds.width.toFloat()
    val h = bounds.height.toFloat()
    return decodeGradient(
      x + .5f * w,
      y,
      x + .5f * w,
      y + h,
      floatArrayOf(
        .038709678f,
        .05483871f,
        .07096774f,
        .28064516f,
        .4903226f,
        .6967742f,
        .9032258f,
        .9241935f,
        .9451613f,
      ),
      arrayOf(
        color18,
        decodeColor(color18, color19, .5f),
        color19,
        decodeColor(color19, color20, .5f),
        color20,
        decodeColor(color20, color21, .5f),
        color21,
        decodeColor(color21, color22, .5f),
        color22,
      ),
    )
  }

  private fun decodeGradient6(s: Shape): Paint {
    val bounds = s.bounds2D
    val x = bounds.x.toFloat()
    val y = bounds.y.toFloat()
    val w = bounds.width.toFloat()
    val h = bounds.height.toFloat()
    return decodeGradient(
      x + .5f * w,
      y,
      x + .5f * w,
      y + h,
      floatArrayOf(
        .038709678f,
        .061290324f,
        .08387097f,
        .27258065f,
        .46129033f,
        .4903226f,
        .5193548f,
        .71774197f,
        .91612905f,
        .92419356f,
        .93225807f,
      ),
      arrayOf(
        color23,
        decodeColor(color23, color24, .5f),
        color24,
        decodeColor(color24, color25, .5f),
        color25,
        decodeColor(color25, color26, .5f),
        color26,
        decodeColor(color26, color27, .5f),
        color27,
        decodeColor(color27, color28, .5f),
        color28,
      ),
    )
  }

  companion object {
    private const val NIMBUS_ORANGE = "nimbusOrange"
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
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
