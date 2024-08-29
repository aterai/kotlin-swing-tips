package example

import java.awt.*
import java.awt.event.ActionListener
import javax.swing.*

private const val KEY = "Button.defaultButtonFollowsFocus"
private val b1 = JButton("Button1")
private val b2 = JButton("Button2")

fun makeUI(): Component {
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createTitledBorder(KEY)
  val r1 = JRadioButton("TRUE")
  val r2 = JRadioButton("FALSE")
  if (UIManager.getBoolean(KEY)) {
    r1.isSelected = true
  } else {
    r2.isSelected = true
  }
  val bg = ButtonGroup()
  val al = ActionListener { UIManager.put(KEY, r1 == it.source) }
  listOf(r1, r2).forEach {
    it.addActionListener(al)
    bg.add(it)
    box.add(it)
  }
  box.add(Box.createHorizontalGlue())
  val p = JPanel(GridLayout(2, 1))
  p.add(makeRadioPane())
  p.add(box)
  return JPanel(BorderLayout(5, 5)).also {
    it.add(p, BorderLayout.NORTH)
    it.add(makeSampleTextComponent())
    it.add(makeDefaultButtonPanel(), BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeDefaultButtonPanel(): Box {
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(0, 5, 5, 5)
  box.add(Box.createHorizontalGlue())
  box.add(b1)
  box.add(Box.createHorizontalStrut(5))
  box.add(b2)
  b2.addActionListener { Toolkit.getDefaultToolkit().beep() }
  return box
}

private fun makeRadioPane(): Box {
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createTitledBorder("JRootPane#setDefaultButton(...)")
  val map = LinkedHashMap<String, JButton?>()
  map["null"] = null
  map["Button1"] = b1
  map["Button2"] = b2
  val bg = ButtonGroup()
  val al = ActionListener {
    box.rootPane?.defaultButton = map[it.actionCommand]
  }
  map.forEach { (key, _) ->
    val r = JRadioButton(key)
    r.actionCommand = key
    r.addActionListener(al)
    bg.add(r)
    box.add(r)
  }
  box.add(Box.createHorizontalGlue())
  bg.elements.nextElement().isSelected = true
  return box
}

private fun makeSampleTextComponent(): Component {
  val p = JPanel(BorderLayout(2, 2))
  p.border = BorderFactory.createTitledBorder("Sample TextComponent")
  p.add(JTextField(), BorderLayout.NORTH)
  p.add(JScrollPane(JTextArea()))
  return p
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
