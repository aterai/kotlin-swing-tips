package example

import java.awt.*
import javax.swing.*
import javax.swing.text.JTextComponent

private val p = JPanel(BorderLayout(5, 5))
private val panel = JPanel(BorderLayout(2, 2))
private val textArea = JTextArea()
private val nb = JButton("NORTH")
private val sb = JButton("SOUTH")
private val wb = JButton("WEST")
private val eb = JButton("EAST")
private val scroll = JScrollPane(textArea)
private val box = Box.createHorizontalBox()
private val check = JCheckBox("setEditable", true)

fun makeUI(): Component {
  panel.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  panel.add(scroll)
  panel.add(nb, BorderLayout.NORTH)
  panel.add(sb, BorderLayout.SOUTH)
  panel.add(wb, BorderLayout.WEST)
  panel.add(eb, BorderLayout.EAST)

  p.isFocusTraversalPolicyProvider = true

  val policy0 = p.focusTraversalPolicy
  val r0 = JRadioButton("Default", true)
  r0.addActionListener {
    p.focusTraversalPolicy = policy0
    debugPrint()
  }

  val policy1 = CustomFocusTraversalPolicy(listOf(eb, wb, sb, nb))
  val r1 = JRadioButton("Custom")
  r1.addActionListener {
    p.focusTraversalPolicy = policy1
    debugPrint()
  }

  val policy2 = object : LayoutFocusTraversalPolicy() {
    override fun accept(c: Component) = if (c is JTextComponent) c.isEditable else super.accept(c)
  }
  val r2 = JRadioButton("Layout")
  r2.addActionListener {
    p.focusTraversalPolicy = policy2
    debugPrint()
  }

  val bg = ButtonGroup()
  box.border = BorderFactory.createTitledBorder("FocusTraversalPolicy")
  listOf(r0, r1, r2).forEach {
    bg.add(it)
    box.add(it)
    box.add(Box.createHorizontalStrut(3))
  }
  box.add(Box.createHorizontalGlue())
  check.horizontalAlignment = SwingConstants.RIGHT
  check.addActionListener {
    textArea.isEditable = check.isSelected
    debugPrint()
  }

  p.add(panel)
  p.add(box, BorderLayout.NORTH)
  p.add(check, BorderLayout.SOUTH)
  p.preferredSize = Dimension(320, 240)
  EventQueue.invokeLater { debugPrint() }
  return p
}

fun debugPrint() {
  val w = p.topLevelAncestor
  val builder = StringBuilder()
  builder
    .append(debugString("JFrame", w))
    .append("\n")
    .append(debugString("this", p))
    .append("\n")
    .append(debugString("JPanel", panel))
    .append("\n")
    .append(debugString("Box", box))
    .append("\n")
    .append(debugString("JScrollPane", scroll))
    .append("\n")
    .append(debugString("JTextArea", textArea))
    .append("\n")
    .append(debugString("eb", eb))
    .append("\n")
  textArea.text = builder.toString()
}

private fun debugString(label: String, c: Container) = """
  ---- $label ----
    isFocusCycleRoot: ${c.isFocusCycleRoot}
    isFocusTraversalPolicySet: ${c.isFocusTraversalPolicySet}
    isFocusTraversalPolicyProvider: ${c.isFocusTraversalPolicyProvider}
  """.trimIndent()

private class CustomFocusTraversalPolicy(
  private val order: List<Component>,
) : FocusTraversalPolicy() {
  override fun getFirstComponent(focusCycleRoot: Container) = order[0]

  override fun getLastComponent(focusCycleRoot: Container) = order[order.size - 1]

  override fun getComponentAfter(focusCycleRoot: Container, cmp: Component) =
    order[(order.indexOf(cmp) + 1) % order.size]

  override fun getComponentBefore(focusCycleRoot: Container, cmp: Component) =
    order[(order.indexOf(cmp) - 1 + order.size) % order.size]

  override fun getDefaultComponent(focusCycleRoot: Container) = order[0]
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
