package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultEditorKit.PasteAction

fun makeUI(): Component {
  val pasteAction = PasteAction()
  pasteAction.putValue(Action.LARGE_ICON_KEY, ColorIcon(Color.GREEN))

  val button = JButton("text")
  button.isFocusable = false
  button.action = pasteAction
  button.icon = ColorIcon(Color.RED)
  button.addActionListener { Toolkit.getDefaultToolkit().beep() }

  val p = JPanel()
  p.add(button)
  p.add(Box.createRigidArea(Dimension(1, 26)))

  val r0 = JRadioButton("setAction", true)
  r0.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      button.action = pasteAction
    }
  }

  val r1 = JRadioButton("null")
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      button.action = null
    }
  }

  val r2 = JRadioButton("setText")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      button.text = "text"
      button.icon = ColorIcon(Color.RED)
    }
  }

  val check = JCheckBox("setHideActionText")
  check.addActionListener { e -> button.hideActionText = (e.source as? JCheckBox)?.isSelected == true }

  val bg = ButtonGroup()
  val box = Box.createHorizontalBox()
  listOf(r0, r1, r2).forEach {
    bg.add(it)
    box.add(it)
  }

  val pp = JPanel(GridLayout(0, 1))
  pp.add(check)
  pp.add(box)
  pp.add(p)

  return JPanel(BorderLayout()).also {
    it.add(pp, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea()))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
