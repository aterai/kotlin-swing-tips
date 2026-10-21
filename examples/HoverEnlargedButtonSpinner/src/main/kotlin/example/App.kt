package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.plaf.basic.BasicArrowButton

private fun createUI(): Component {
  val p1 = JPanel(BorderLayout(5, 5))
  p1.setBorder(BorderFactory.createTitledBorder("Default JSpinner"))
  p1.add(JSpinner(SpinnerNumberModel(50, 0, 100, 1)))

  val p2 = JPanel(BorderLayout(5, 5))
  p2.setBorder(BorderFactory.createTitledBorder("EnlargedButtonSpinner"))
  p2.add(EnlargedButtonSpinner(SpinnerNumberModel(50, 0, 100, 1)))

  val panel = JPanel(BorderLayout(10, 10))
  panel.add(p1, BorderLayout.NORTH)
  panel.add(p2, BorderLayout.SOUTH)
  panel.setBorder(BorderFactory.createEmptyBorder(10, 80, 10, 80))

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.setJMenuBar(mb) }
    it.add(panel, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class EnlargedButtonSpinner(
  model: SpinnerModel,
) : JSpinner(model) {
  private var listener: MouseAdapter? = null

  override fun updateUI() {
    val field = (editor as? DefaultEditor)?.textField
    if (field != null) {
      field.removeMouseListener(listener)
      super.updateUI()
      // Recreate the listener so that its JPopupMenu follows the new LookAndFeel
      listener = ArrowButtonEnlargeListener()
      field.addMouseListener(listener)
    } else {
      super.updateUI()
    }
  }
}

private class ArrowButtonEnlargeListener : MouseAdapter() {
  private val popup = JPopupMenu()

  init {
    popup.setLayout(GridLayout(2, 1))
    popup.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY))
    popup.setFocusable(false)
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    val spinner = SwingUtilities.getAncestorOfClass(JSpinner::class.java, c)
    if (SwingUtilities.isLeftMouseButton(e) && spinner is JSpinner) {
      popup.removeAll()
      popup.add(createArrowButton(spinner, true))
      popup.add(createArrowButton(spinner, false))
      popup.pack()
      // Show the popup to the right of the editor, vertically centered on it
      val editor = spinner.editor
      val r = SwingUtilities.calculateInnerArea(editor, null)
      val px = r.maxX.toInt()
      val py = r.centerY.toInt() - popup.preferredSize.height / 2
      popup.show(editor, px, py)
    }
  }

  private fun createArrowButton(spinner: JSpinner, isNext: Boolean): JButton {
    val direction = if (isNext) SwingConstants.NORTH else SwingConstants.SOUTH
    val arrowButton = object : BasicArrowButton(direction) {
      override fun getPreferredSize(): Dimension {
        val d = super.getPreferredSize()
        d.width *= 4
        d.height *= 2
        return d
      }
    }
    val name = if (isNext) "increment" else "decrement"
    val handler = ArrowButtonHandler(spinner, name, isNext)
    arrowButton.addActionListener(handler)
    arrowButton.addMouseListener(handler)
    return arrowButton
  }
}

private class ArrowButtonHandler(
  private val spinner: JSpinner,
  name: String,
  private val isNext: Boolean,
) : AbstractAction(name),
  MouseListener {
  private val repeatTimer = Timer(60, this)
  private var arrowButton: JButton? = null

  init {
    repeatTimer.setInitialDelay(300)
  }

  override fun actionPerformed(e: ActionEvent) {
    val released = arrowButton?.model?.isPressed == false
    if (e.source is Timer && released) {
      // The button was released without a mouseReleased event, stop repeating
      // and do not change the value
      repeatTimer.stop()
      setArrowButton(null)
      return
    }
    val value = if (isNext) spinner.nextValue else spinner.previousValue
    if (value != null) {
      spinner.value = value
    }
  }

  private fun setArrowButton(button: JButton?) {
    this.arrowButton = button
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    if (SwingUtilities.isLeftMouseButton(e) && c.isEnabled && c is JButton) {
      setArrowButton(c)
      repeatTimer.start()
    }
  }

  override fun mouseReleased(e: MouseEvent?) {
    repeatTimer.stop()
    setArrowButton(null)
  }

  override fun mouseClicked(e: MouseEvent?) {
    // not needed
  }

  override fun mouseEntered(e: MouseEvent?) {
    // Resume repeating if the mouse comes back while the button is still pressed
    if (arrowButton?.model?.isPressed == true && !repeatTimer.isRunning) {
      repeatTimer.start()
    }
  }

  override fun mouseExited(e: MouseEvent?) {
    repeatTimer.stop()
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
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
