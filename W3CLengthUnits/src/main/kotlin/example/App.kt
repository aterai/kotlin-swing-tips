package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.html.HTMLEditorKit

fun makeUI(): Component {
  val editor = JEditorPane("text/html", "")
  val htmlEditorKit = editor.editorKit as HTMLEditorKit
  val styles = htmlEditorKit.styleSheet
  styles.addRule(".number {font-size: 14}")
  styles.addRule(".pt {font-size: 14pt}")
  styles.addRule(".em {font-size: 1.2em}")
  styles.addRule(".percentage {font-size: 120%}")
  val html =
    """
      <html>
        <h3>h3 {font-size: medium}</h3>
        <h3 class='number'>h3 {font-size: 14}</h3>
        <h3 class='pt'>h3 {font-size: 14pt}</h3>
        <h3 class='em'>h3 {font-size: 1.2em}</h3>
        <h3 class='percentage'>h3 {font-size: 120%}</h3>
      </html>
      """
  editor.text = html

  val check = JCheckBox("JEditorPane.W3C_LENGTH_UNITS")
  check.addActionListener { e ->
    editor.putClientProperty(JEditorPane.W3C_LENGTH_UNITS, (e.source as? JCheckBox)?.isSelected == true)
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(JScrollPane(editor))
    it.preferredSize = Dimension(320, 240)
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
