package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.html.HTMLEditorKit

private const val SITE = "https://ateraimemo.com/"
private const val HREF = "<html><a href='$SITE'>$SITE</a>"

fun makeUI(): Component {
  val p = JPanel(GridLayout(3, 1))
  p.add(makeUrlPanel("Default", HREF))
  val kit = HTMLEditorKit()
  val styleSheet = kit.styleSheet
  styleSheet.addRule("a{color:#FF0000;}")
  p.add(makeUrlPanel("styleSheet.addRule(\"a{color:#FF0000;}\")", HREF))
  p.add(makeUrlPanel(
    "<a style='color:#00FF00'...",
    "<html><a style='color:#00FF00' href='$SITE'>$SITE</a>"
  ))
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeUrlPanel(title: String, href: String): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val editor = JEditorPane("text/html", href)
  editor.isOpaque = false
  editor.isEditable = false
  editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  val c = GridBagConstraints()
  c.insets = Insets(5, 5, 5, 0)
  c.gridx = 0
  c.anchor = GridBagConstraints.LINE_END
  p.add(JLabel("JLabel:"), c)
  p.add(JLabel("JEditorPane:"), c)
  c.gridx = 1
  c.weightx = 1.0
  c.anchor = GridBagConstraints.LINE_START
  p.add(JLabel(href), c)
  p.add(editor, c)
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
