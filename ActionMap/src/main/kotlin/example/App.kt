package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultEditorKit

fun makeUI(): Component {
  val pf1 = JTextField(25)
  pf1.setActionMap(pf1.getActionMap().also {
    val beep = DefaultEditorKit.BeepAction()
    it.put(DefaultEditorKit.cutAction, beep)
    it.put(DefaultEditorKit.copyAction, beep)
    it.put(DefaultEditorKit.pasteAction, beep)
  })

  val pf2 = object : JTextField() {
    override fun copy() {
      UIManager.getLookAndFeel().provideErrorFeedback(this)
    }

    override fun cut() {
      UIManager.getLookAndFeel().provideErrorFeedback(this)
    }
  }
  pf2.setActionMap(pf2.getActionMap().also {
    it.put(DefaultEditorKit.pasteAction, object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val c = e.getSource() as? JComponent ?: return@actionPerformed
        EventQueue.invokeLater {
          Toolkit.getDefaultToolkit().beep()
          JOptionPane.showMessageDialog(
            c.getRootPane(),
            "paste is disabled",
            "title",
            JOptionPane.ERROR_MESSAGE
          )
        }
      }
    })
  })

  val box = Box.createVerticalBox()
  box.setBorder(BorderFactory.createTitledBorder("E-mail Address"))
  box.add(pf1)
  box.add(Box.createVerticalStrut(5))
  box.add(JLabel("Please enter your email address twice for confirmation:"))
  box.add(pf2)
  box.add(Box.createVerticalStrut(5))

  val p = JPanel(BorderLayout())
  p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  p.add(box, BorderLayout.NORTH)
  p.add(JScrollPane(JTextArea("Dummy")))
  p.setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
