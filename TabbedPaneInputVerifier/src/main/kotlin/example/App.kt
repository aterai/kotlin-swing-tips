package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val field0 = JTextField("---")
  val field1 = JTextField("100")
  val list = listOf(field0, field1)
  EventQueue.invokeLater { field0.requestFocusInWindow() }
  list.forEach {
    it.horizontalAlignment = SwingConstants.RIGHT
    it.inputVerifier = IntegerInputVerifier()
  }
  val button0 = JButton("JButton")

  val button1 = JButton("setText(0)")
  button1.addActionListener {
    list.forEach { it.text = "0" }
  }
  button1.verifyInputWhenFocusTarget = false

  val bp = JPanel()
  bp.add(button0)
  bp.add(button1)

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(field0)
  box.add(Box.createVerticalStrut(5))
  box.add(field1)
  box.add(Box.createVerticalStrut(5))
  box.add(bp)
  box.add(Box.createVerticalGlue())

  val p = JPanel(BorderLayout())
  p.add(box, BorderLayout.NORTH)
  p.inputVerifier = object : InputVerifier() {
    override fun verify(c: JComponent) =
      !c.isShowing || list.all { it.inputVerifier.verify(it) }
  }

  val tabbedPane = JTabbedPane()
  tabbedPane.addTab("Integer only", JScrollPane(p))
  tabbedPane.addTab("JTree", JScrollPane(JTree()))
  tabbedPane.addTab("JSplitPane", JSplitPane())
  tabbedPane.selectedIndex = 0

  val check = JCheckBox("override SingleSelectionModel#setSelectedIndex(int)")
  val ssm = tabbedPane.model
  check.addActionListener { e ->
    if ((e.source as? JCheckBox)?.isSelected == true) {
      tabbedPane.model = object : DefaultSingleSelectionModel() {
        override fun setSelectedIndex(index: Int) {
          val verifier = p.inputVerifier
          if (verifier?.shouldYieldFocus(p) != true) {
            UIManager.getLookAndFeel().provideErrorFeedback(p)
            JOptionPane.showMessageDialog(p, "InputVerifier#verify(...): false")
            return
          }
          super.setSelectedIndex(index)
        }
      }
    } else {
      tabbedPane.model = ssm
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private class IntegerInputVerifier : InputVerifier() {
  override fun verify(c: JComponent) = (c as? JTextComponent)?.let {
    runCatching { it.text.toInt() }.isSuccess
  } ?: false

  override fun shouldYieldFocus(input: JComponent) = verify(input).also {
    if (!it) {
      UIManager.getLookAndFeel().provideErrorFeedback(input)
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
