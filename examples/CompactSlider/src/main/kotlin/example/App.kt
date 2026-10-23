package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.NumberFormat
import java.text.ParseException
import javax.swing.*
import javax.swing.Timer
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicProgressBarUI
import javax.swing.plaf.basic.BasicSliderUI
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter

fun createUI(): Component {
  val p = JPanel(GridLayout(0, 1, 15, 15))
  p.isOpaque = false
  p.add(createCompactSlider1())
  p.add(createCompactSlider2())
  p.add(createCompactSlider3())
  p.add(createCompactSlider4())
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.setBackground(Color.WHITE)
    it.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createCompactSlider1(): Component {
  val m = DefaultBoundedRangeModel(50, 0, 0, 100)
  val progressBar = CompactProgressBar(m)
  val spinner = makeSpinner(progressBar)
  initListener(spinner, progressBar)
  return spinner
}

private fun makeSpinner(progressBar: JProgressBar): JSpinner {
  val m = progressBar.model
  val value = m.value
  val min = m.minimum
  val max = m.maximum
  // The transparent settings of the editor are inherited from FlatSpinner
  return object : FlatSpinner(SpinnerNumberModel(value, min, max, 5)) {
    private val renderer = JPanel()

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      val r = editor.bounds
      SwingUtilities.paintComponent(g2, progressBar, renderer, r)
      g2.dispose()
    }
  }
}

private fun createCompactSlider2(): Component {
  val m = DefaultBoundedRangeModel(50, 0, 0, 100)
  val progressBar = CompactProgressBar(m)
  val spinner = makeSpinner2(m)
  initListener(spinner, progressBar)
  val layerUI = object : LayerUI<JSpinner>() {
    private val renderer = JPanel()

    override fun paint(g: Graphics, c: JComponent) {
      // super.paint(g, c)
      if (c is JLayer<*>) {
        val view = c.view
        if (view is JSpinner) {
          val editor = view.editor
          val r = editor.bounds
          val g2 = g.create() as? Graphics2D ?: return
          SwingUtilities.paintComponent(g2, progressBar, renderer, r)
          g2.dispose()
        }
      }
      super.paint(g, c)
    }
  }
  return JLayer(spinner, layerUI)
}

private fun makeSpinner2(m: BoundedRangeModel): JSpinner {
  val min = m.minimum
  val max = m.maximum
  val value = m.value
  return FlatSpinner(SpinnerNumberModel(value, min, max, 5))
}

private fun initListener(spinner: JSpinner, progressBar: JProgressBar) {
  spinner.addChangeListener { e ->
    (e.source as? JSpinner)?.also { spinner ->
      (spinner.value as? Int)?.also { iv ->
        progressBar.value = iv
      }
    }
  }
  spinner.addMouseWheelListener { e ->
    val source = e.component as? JSpinner
    val model = source?.model
    if (source is JSpinner && model is SpinnerNumberModel) {
      val oldValue = source.value as? Number ?: 0
      val newValue = oldValue.toInt() - e.wheelRotation * model.stepSize.toInt()
      // Clamp so that min and max can be reached even if the value is not a multiple of step
      val max = model.maximum as? Number ?: 0
      val min = model.minimum as? Number ?: 0
      source.value = newValue.coerceIn(min.toInt(), max.toInt())
    }
  }
}

private fun createCompactSlider3(): Component {
  val m = DefaultBoundedRangeModel(50, 0, 0, 100)
  val progressBar = CompactProgressBar(m)
  val field = makeSpinner3(progressBar)
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(field)
  box.add(createButton(-5, m))
  box.add(createButton(+5, m))
  box.add(Box.createHorizontalGlue())
  return box
}

private fun makeSpinner3(progressBar: JProgressBar): JTextField {
  val field = object : JFormattedTextField() {
    private val renderer = JPanel()

    override fun updateUI() {
      super.updateUI()
      isOpaque = false
      formatterFactory = NumberFormatterFactory()
      horizontalAlignment = RIGHT
    }

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      val r = SwingUtilities.calculateInnerArea(this, null)
      SwingUtilities.paintComponent(g2, progressBar, renderer, r)
      g2.dispose()
      super.paintComponent(g)
    }

    @Throws(ParseException::class)
    override fun commitEdit() {
      super.commitEdit()
      (value as? Number)?.also {
        progressBar.value = it.toInt()
      }
    }
  }
  field.columns = 16
  val m = progressBar.model
  field.value = m.value
  // Keep the text field in sync when the value is changed by the buttons or the mouse wheel
  m.addChangeListener { field.value = m.value }
  // BoundedRangeModel#setValue(...) clamps the value to the range
  field.addMouseWheelListener { e -> m.value = m.value - e.wheelRotation }
  return field
}

private fun createButton(
  step: Int,
  model: BoundedRangeModel,
): JButton {
  val title = "%+d".format(step)
  val button = JButton(title)
  val handler = AutoRepeatHandler(step, model)
  button.addActionListener(handler)
  button.addMouseListener(handler)
  return button
}

private fun createCompactSlider4(): Component {
  val slider = object : JSlider(0, 100, 50) {
    override fun updateUI() {
      super.updateUI()
      foreground = Color.LIGHT_GRAY
      setUI(FlatSliderUI(this))
      isFocusable = false
      alignmentX = RIGHT_ALIGNMENT
    }
  }
  val field = object : JFormattedTextField() {
    override fun updateUI() {
      super.updateUI()
      formatterFactory = NumberFormatterFactory()
      horizontalAlignment = RIGHT
      isOpaque = false
      border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    }

    @Throws(ParseException::class)
    override fun commitEdit() {
      super.commitEdit()
      (value as? Number)?.also {
        slider.value = it.toInt()
      }
    }

    override fun getMaximumSize() = super.getPreferredSize()
  }
  field.columns = 3
  field.value = slider.value
  field.alignmentX = JComponent.RIGHT_ALIGNMENT
  slider.addChangeListener { e ->
    (e.source as? JSlider)?.also {
      field.value = it.value
      // BasicSliderUI repaints only the thumb area while dragging,
      // so repaint the whole slider to update the filled track
      it.repaint()
    }
  }
  // JSlider#setValue(...) clamps the value to the range
  slider.addMouseWheelListener { e ->
    (e.source as? JSlider)?.also {
      it.value = it.value - e.wheelRotation
    }
  }
  val p = object : JPanel() {
    override fun isOptimizedDrawingEnabled() = false

    override fun getPreferredSize() = slider.preferredSize
  }
  p.layout = OverlayLayout(p)
  p.isOpaque = false
  p.border = BorderFactory.createLineBorder(Color.GRAY)
  p.add(field)
  p.add(slider)
  val box = Box.createHorizontalBox()
  box.add(p)
  box.add(Box.createHorizontalStrut(2))
  box.add(createButton(-5, slider.model))
  box.add(createButton(+5, slider.model))
  box.add(Box.createHorizontalGlue())
  val panel = JPanel(BorderLayout())
  panel.add(p)
  panel.add(box, BorderLayout.EAST)
  return panel
}

private class CompactProgressBar(
  m: BoundedRangeModel,
) : JProgressBar(m) {
  override fun updateUI() {
    super.updateUI()
    setUI(BasicProgressBarUI())
    isOpaque = false
    border = BorderFactory.createEmptyBorder()
  }
}

private open class FlatSpinner(
  model: SpinnerModel,
) : JSpinner(model) {
  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    val editor = editor as? DefaultEditor ?: return
    editor.isOpaque = false
    val field = editor.textField
    field.isOpaque = false
    field.border = BorderFactory.createEmptyBorder()
    val d = UIDefaults()
    val painter = Painter { _: Graphics2D?, _: JComponent?, _: Int, _: Int -> }
    val key = "Spinner:Panel:\"Spinner.formattedTextField\""
    d["$key[Enabled].backgroundPainter"] = painter
    d["$key[Focused].backgroundPainter"] = painter
    d["$key[Selected].backgroundPainter"] = painter
    field.putClientProperty("Nimbus.Overrides", d)
    field.putClientProperty("Nimbus.Overrides.InheritDefaults", true)
  }
}

// An AbstractFormatter is installed into a single JFormattedTextField,
// so create a new instance for each factory instead of sharing a static one
private class NumberFormatterFactory : DefaultFormatterFactory(createFormatter()) {
  companion object {
    private fun createFormatter() =
      NumberFormatter().also {
        it.valueClass = Integer::class.java
        (it.format as? NumberFormat)?.isGroupingUsed = false
      }
  }
}

private class AutoRepeatHandler(
  private val step: Int,
  private val model: BoundedRangeModel,
) : MouseAdapter(),
  ActionListener {
  private val autoRepeatTimer = Timer(60, this)
  private var arrowButton: JButton? = null

  init {
    autoRepeatTimer.initialDelay = 300
  }

  override fun actionPerformed(e: ActionEvent) {
    val o = e.source
    if (o is Timer) {
      arrowButton?.also {
        if (!it.model.isPressed && autoRepeatTimer.isRunning) {
          autoRepeatTimer.stop()
        }
      }
    } else if (o is JButton) {
      arrowButton = o
    }
    // The text field is updated by the ChangeListener of the model,
    // and BoundedRangeModel#setValue(...) clamps the value to the range
    model.value = model.value + step
  }

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e) && e.component.isEnabled) {
      autoRepeatTimer.start()
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    autoRepeatTimer.stop()
  }

  override fun mouseExited(e: MouseEvent) {
    if (autoRepeatTimer.isRunning) {
      autoRepeatTimer.stop()
    }
  }
}

private class FlatSliderUI(
  slider: JSlider,
) : BasicSliderUI(slider) {
  override fun paintThumb(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = slider.foreground
    val r = SwingUtilities.calculateInnerArea(slider, null)
    g2.fillRect(thumbRect.x, r.y, thumbRect.width, r.height)
    g2.dispose()
  }

  override fun paintTrack(g: Graphics) {
    if (slider.orientation == SwingConstants.HORIZONTAL) {
      val g2 = g.create() as? Graphics2D ?: return
      val middleOfThumb = thumbRect.x + thumbRect.width / 2
      val r = SwingUtilities.calculateInnerArea(slider, null)
      if (drawInverted()) {
        // Fill from the thumb to the right edge
        r.width = r.x + r.width - middleOfThumb
        r.x = middleOfThumb
      } else {
        r.width = middleOfThumb - r.x
      }
      g2.paint = slider.foreground
      g2.fill(r)
      g2.dispose()
    } else {
      super.paintTrack(g)
    }
  }

  override fun paintFocus(g: Graphics) {
    // super.paintFocus(g)
  }
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
