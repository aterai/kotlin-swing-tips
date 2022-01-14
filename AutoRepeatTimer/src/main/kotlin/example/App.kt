package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.math.BigInteger
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val field = object : JTextField("0") {
    override fun getMaximumSize() = preferredSize

    override fun getPreferredSize() = Dimension(100, 50)
  }
  field.horizontalAlignment = SwingConstants.CENTER
  field.font = field.font.deriveFont(30f)
  field.isEditable = false

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(makeButton(-5, field))
  box.add(makeButton(-1, field))
  box.add(field)
  box.add(makeButton(+1, field))
  box.add(makeButton(+5, field))
  box.add(Box.createHorizontalGlue())

  return JPanel(GridBagLayout()).also {
    it.add(box)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeButton(extent: Int, view: JTextField): JButton {
  val title = "%+d".format(extent)
  val button = object : JButton(title) {
    override fun getMaximumSize() = Dimension(50, 50)
  }
  val handler = AutoRepeatHandler(extent, view)
  button.addActionListener(handler)
  button.addMouseListener(handler)
  return button
}

private class AutoRepeatHandler(
  extent: Int,
  private val view: JTextComponent
) : MouseAdapter(), ActionListener {
  private val autoRepeatTimer = Timer(60, this)
  private val extent = BigInteger.valueOf(extent.toLong())
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
          arrowButton = null
        }
      }
    } else if (o is JButton) {
      arrowButton = o
    }
    val i = BigInteger(view.text)
    view.text = i.add(extent).toString()
  }

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e) && e.component.isEnabled) {
      autoRepeatTimer.start()
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    autoRepeatTimer.stop()
    arrowButton = null
  }

  override fun mouseExited(e: MouseEvent) {
    if (autoRepeatTimer.isRunning) {
      autoRepeatTimer.stop()
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
