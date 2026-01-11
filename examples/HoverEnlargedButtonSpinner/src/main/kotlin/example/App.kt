package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.plaf.basic.BasicArrowButton

private fun makeUI(): Component {
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
    (editor as? DefaultEditor)?.textField?.also {
      it.removeMouseListener(listener)
      super.updateUI()
      listener = ArrowButtonEnlargeListener()
      it.addMouseListener(listener)
    }
  }
}

private class ArrowButtonEnlargeListener : MouseAdapter() {
  private val popup = JPopupMenu()

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    val spinner = SwingUtilities.getAncestorOfClass(JSpinner::class.java, c)
    if (SwingUtilities.isLeftMouseButton(e) && spinner is JSpinner) {
      val bigNextBtn = makeArrowButton(spinner, true)
      val bigPrevBtn = makeArrowButton(spinner, false)
      popup.setLayout(GridLayout(2, 1))
      popup.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY))
      popup.setFocusable(false)
      popup.removeAll()
      popup.add(bigNextBtn)
      popup.add(bigPrevBtn)
      popup.pack()
      val editor = spinner.editor
      val r = SwingUtilities.calculateInnerArea(editor, null)
      val px = r.maxX.toInt()
      val py = r.centerY.toInt() - bigNextBtn.getPreferredSize().height
      popup.show(editor, px, py)
    }
  }

  private fun makeArrowButton(spinner: JSpinner, isNext: Boolean): JButton {
    val direction = if (isNext) SwingConstants.NORTH else SwingConstants.SOUTH
    val arrowButton: JButton = object : BasicArrowButton(direction) {
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
    val src = e.getSource()
    if (src is Timer) {
      val b = arrowButton?.getModel()?.isPressed
      if (b == false && repeatTimer.isRunning) {
        repeatTimer.stop()
        setArrowButton(null)
      }
    } else {
      if (src is JButton) {
        setArrowButton(src)
      }
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
    if (SwingUtilities.isLeftMouseButton(e) && e.component.isEnabled) {
      repeatTimer.start()
    }
  }

  override fun mouseReleased(e: MouseEvent?) {
    repeatTimer.stop()
    setArrowButton(null)
  }

  override fun mouseClicked(e: MouseEvent?) {
    // no need
  }

  override fun mouseEntered(e: MouseEvent?) {
    // if (!repeatTimer.isRunning()) {
    //   repeatTimer.start();
    // }
  }

  override fun mouseExited(e: MouseEvent?) {
    if (repeatTimer.isRunning) {
      repeatTimer.stop()
    }
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
