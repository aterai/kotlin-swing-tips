package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun makeUI(): Component {
  val log = JTextArea()

  val button = JButton("JButton")
  button.addActionListener { append(log, "JButton clicked") }

  val check = JCheckBox("setDefaultButton")
  check.addActionListener { button.rootPane.defaultButton = button }

  val textField1 = JTextField("addDocumentListener")
  textField1.document.addDocumentListener(object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent) {
      append(log, "insertUpdate")
    }

    override fun removeUpdate(e: DocumentEvent) {
      append(log, "removeUpdate")
    }

    override fun changedUpdate(e: DocumentEvent) {
      // not needed
    }
  })

  val textField2 = JTextField("addActionListener")
  textField2.addActionListener { e ->
    val str = (e.source as? JTextField)?.text ?: ""
    append(log, str)
  }

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  box.add(JTextField("JTextField"))
  box.add(Box.createVerticalStrut(10))
  box.add(textField1)
  box.add(Box.createVerticalStrut(10))
  box.add(textField2)

  val p = JPanel(FlowLayout(FlowLayout.RIGHT))
  p.add(check)
  p.add(button)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun append(log: JTextArea, text: String) {
  log.append("$text\n")
  log.caretPosition = log.document.length
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
