package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun makeUI(): Component {
  val log = JTextArea()
  val p = JPanel(GridLayout(1, 2, 5, 5))
  p.add(makeTitledPanel("Default", makeButton1(log)))
  p.add(makeTitledPanel("Disabled OK button", makeButton2(log)))
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private fun makeButton1(log: JTextArea): JButton {
  val field1 = JTextField()
  val button1 = JButton("show")
  button1.addActionListener {
    val p1: Component = log.rootPane
    val ret = JOptionPane.showConfirmDialog(
      p1,
      field1,
      "Input text",
      JOptionPane.OK_CANCEL_OPTION,
      JOptionPane.PLAIN_MESSAGE
    )
    if (ret == JOptionPane.OK_OPTION) {
      log.text = field1.text
    }
  }
  return button1
}

private fun makeButton2(log: JTextArea): JButton {
  val panel2 = JPanel(GridLayout(2, 1))
  val field2 = JTextField()
  val enabledBorder = field2.border
  val i = enabledBorder.getBorderInsets(field2)
  val disabledBorder = BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(Color.RED),
    BorderFactory.createEmptyBorder(i.top - 1, i.left - 1, i.bottom - 1, i.right - 1)
  )
  val disabledMessage = "Text is required to create ..."
  val label2 = JLabel(" ")
  label2.foreground = Color.RED
  panel2.add(field2)
  panel2.add(label2)
  if (field2.text.isEmpty()) {
    field2.border = disabledBorder
    label2.text = disabledMessage
  }
  field2.addHierarchyListener { e ->
    val c = e.component
    if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && c.isShowing) {
      EventQueue.invokeLater { c.requestFocusInWindow() }
    }
  }
  field2.document.addDocumentListener(object : DocumentListener {
    private fun update() {
      val verified = field2.text.isNotEmpty()
      val b = field2.rootPane.defaultButton
      if (verified) {
        b.isEnabled = true
        field2.border = enabledBorder
        label2.text = " "
      } else {
        b.isEnabled = false
        field2.border = disabledBorder
        label2.text = disabledMessage
      }
    }

    override fun insertUpdate(e: DocumentEvent) {
      update()
    }

    override fun removeUpdate(e: DocumentEvent) {
      update()
    }

    override fun changedUpdate(e: DocumentEvent) {
      update()
    }
  })
  val button2 = JButton("show")
  button2.addActionListener {
    val p2: Component = log.rootPane
    EventQueue.invokeLater {
      val b = field2.rootPane.defaultButton
      if (b != null && field2.text.isEmpty()) {
        b.isEnabled = false
      }
    }
    val ret = JOptionPane.showConfirmDialog(
      p2,
      panel2,
      "Input text",
      JOptionPane.OK_CANCEL_OPTION,
      JOptionPane.PLAIN_MESSAGE
    )
    if (ret == JOptionPane.OK_OPTION) {
      log.text = field2.text
    }
  }
  return button2
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
