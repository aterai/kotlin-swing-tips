package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.JTextComponent

class MainPanel : JPanel(BorderLayout(5, 5)) {
  private val field0 = JTextField("9999999999999999")
  private val field1 = JTextField("1111111111111111")
  private val field2 = JTextField("9876543210987654")

  init {
    EventQueue.invokeLater { field0.requestFocusInWindow() }
    val al = ActionListener {
      val c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
      println(c)
      listOf(field0, field1, field2).forEach { it.setText("") }
    }
    val button0 = JButton("Default")
    button0.addActionListener(al)
    // button0.setVerifyInputWhenFocusTarget(true)

    val button1 = JButton("setFocusable(false)")
    button1.addActionListener(al)
    button1.setFocusable(false)

    val button2 = JButton("setVerifyInputWhenFocusTarget(false)")
    button2.addActionListener(al)
    button2.setVerifyInputWhenFocusTarget(false)

    val verifier: InputVerifier = IntegerInputVerifier()
    listOf(field0, field1, field2).forEach {
      it.setHorizontalAlignment(SwingConstants.RIGHT)
      it.setInputVerifier(verifier)
    }

    val b0 = JButton("setText(0)")
    b0.addActionListener {
      listOf(field0, field1, field2).forEach { it.setText("0") }
      field0.requestFocusInWindow()
    }

    val b1 = JButton("setText(Integer.MAX_VALUE+1)")
    b1.addActionListener {
      listOf(field0, field1, field2).forEach { it.setText("2147483648") }
      field0.requestFocusInWindow()
    }

    val bp = JPanel()
    bp.add(b0)
    bp.add(b1)

    val box = Box.createVerticalBox()
    box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    box.add(bp)
    box.add(Box.createVerticalStrut(5))
    box.add(field0)
    box.add(Box.createVerticalStrut(5))
    box.add(field1)
    box.add(Box.createVerticalStrut(5))
    box.add(field2)
    box.add(Box.createVerticalStrut(5))
    box.add(Box.createVerticalGlue())

    val p1 = JPanel()
    p1.add(button0)
    p1.add(button1)

    val p2 = JPanel()
    p2.add(button2)

    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createTitledBorder("clear all"))
    p.add(p1, BorderLayout.NORTH)
    p.add(p2, BorderLayout.SOUTH)

    add(box, BorderLayout.NORTH)
    add(p, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }
}

// Validating Text and Filtering Documents and Accessibility and the Java Access Bridge Tech Tips
// http://java.sun.com/developer/JDCTechTips/2005/tt0518.html
// Validating with Input Verifiers
class IntegerInputVerifier : InputVerifier() {
  override fun verify(c: JComponent): Boolean {
    var verified = false
    if (c is JTextComponent) {
      runCatching {
        c.getText().toInt()
        verified = true
      }.onFailure {
        println("InputVerifier#verify: false")
        UIManager.getLookAndFeel().provideErrorFeedback(c)
      }
    }
    return verified
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
