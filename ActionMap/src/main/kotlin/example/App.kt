package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.text.DefaultEditorKit

fun makeUI(): Component {
  val pf1 = JTextField(25)
  pf1.actionMap = pf1.actionMap.also {
    val beep = DefaultEditorKit.BeepAction()
    it.put(DefaultEditorKit.cutAction, beep)
    it.put(DefaultEditorKit.copyAction, beep)
    it.put(DefaultEditorKit.pasteAction, beep)
  }

  val pf2 = object : JTextField() {
    override fun copy() {
      UIManager.getLookAndFeel().provideErrorFeedback(this)
    }

    override fun cut() {
      UIManager.getLookAndFeel().provideErrorFeedback(this)
    }
  }
  pf2.actionMap = pf2.actionMap.also {
    val a = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        EventQueue.invokeLater {
          Toolkit.getDefaultToolkit().beep()
          JOptionPane.showMessageDialog(
            (e.source as? JComponent)?.rootPane,
            "paste is disabled",
            "title",
            JOptionPane.ERROR_MESSAGE
          )
        }
      }
    }
    it.put(DefaultEditorKit.pasteAction, a)
  }

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createTitledBorder("E-mail Address")
  box.add(pf1)
  box.add(Box.createVerticalStrut(5))
  box.add(JLabel("Please enter your email address twice for confirmation:"))
  box.add(pf2)
  box.add(Box.createVerticalStrut(5))

  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(box, BorderLayout.NORTH)
  p.add(JScrollPane(JTextArea("JTextArea")))
  p.preferredSize = Dimension(320, 240)
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
