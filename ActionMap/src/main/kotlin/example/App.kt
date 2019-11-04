package example

import java.awt.*  // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultEditorKit

class MainPanel : JPanel(BorderLayout()) {
  init {
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
          EventQueue.invokeLater {
            Toolkit.getDefaultToolkit().beep()
            JOptionPane.showMessageDialog(
              getRootPane(),
              "paste is disabled",
              "title",
              JOptionPane.ERROR_MESSAGE
            )
          }
        }
      })
    })

    val panel = Box.createVerticalBox()
    panel.setBorder(BorderFactory.createTitledBorder("E-mail Address"))
    panel.add(pf1)
    panel.add(Box.createVerticalStrut(5))
    panel.add(JLabel("Please enter your email address twice for confirmation:"))
    panel.add(pf2)
    panel.add(Box.createVerticalStrut(5))

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(panel, BorderLayout.NORTH)
    add(JScrollPane(JTextArea("Dummy")))
    setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
