package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.html.HTMLEditorKit

fun makeUI(): Component {
  val editor = JEditorPane("text/html", "")
  editor.font = Font("Serif", Font.PLAIN, 16)

  //  val htmlEditorKit = HTMLEditorKit()
  //  val styleSheet = StyleSheet()
  //  styleSheet.addRule("body {font-size: 16pt;}")
  //  styleSheet.addRule("h1 {font-size: 64pt;}")
  //  htmlEditorKit.styleSheet = styleSheet
  //  editor.editorKit = htmlEditorKit

  val htmlEditorKit = editor.editorKit as? HTMLEditorKit ?: HTMLEditorKit().also {
    editor.editorKit = it
  }

  val buf = StringBuilder(300)
  buf.append("<html>JEditorPane#setFont(new Font('Serif', Font.PLAIN, 16));<br />")
  val styles = htmlEditorKit.styleSheet
  val rules = styles.styleNames
  while (rules.hasMoreElements()) {
    val name = rules.nextElement().toString()
    if ("body" == name) {
      val rule = styles.getRule(name)
      val attrs = rule.attributeNames
      while (attrs.hasMoreElements()) {
        val a = attrs.nextElement()
        buf.append("$a: ${rule.getAttribute(a)}<br />")
      }
    }
  }
  editor.text = buf.toString()

  val check = JCheckBox("JEditorPane.HONOR_DISPLAY_PROPERTIES")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, b)
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
