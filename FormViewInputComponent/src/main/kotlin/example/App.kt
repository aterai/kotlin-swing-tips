package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.net.URLDecoder
import java.nio.charset.Charset
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.html.FormSubmitEvent
import javax.swing.text.html.HTMLEditorKit

private const val FORM1 = """
  <form id='form1' action='#'>
    <div>Username:
      <input type='text' id='username' name='username'>
    </div>
    <div>Password:
      <input type='password' id='password' name='password'>
    </div>
    <input type='submit' value='Submit'>
  </form>
"""

private const val FORM3 = """
  <form id='form3' action='#'>
    <div>
      <select name='select1' size='5' multiple>
        <option value='' selected='selected'>selected</option>
        <option value='option1'>option1</option>
        <option value='option2'>option2</option>
      </select>
    </div>
    <br />
    <div>
      <select name='select2'>
        <option value='option0'>option0</option>
        <option value='option1'>option1</option>
        <option value='option2'>option2</option>
      </select>
    </div>
    <br />
    <div>
      <textarea name='textarea1' cols='50' rows='5'>
    </div>
  </form>
"""

fun makeUI(): Component {
  val editor = JEditorPane()
  val kit = HTMLEditorKit()
  kit.isAutoFormSubmission = false
  editor.editorKit = kit
  editor.isEditable = false
  editor.addHyperlinkListener { e ->
    if (e is FormSubmitEvent) {
      val charset = Charset.defaultCharset().toString()
      runCatching {
        println(URLDecoder.decode(e.data, charset))
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(editor)
      }
    }
  }
  editor.text = makeHtml()
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(editor))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeHtml(): String {
  val cl = Thread.currentThread().contextClassLoader
  val src = cl.getResource("example/16x16.png")?.toString() ?: "not found"
  return """
    <html>
    <body>
      $FORM1
      <br />
      <hr />
      <form id='form2' action='#'>
        <div>button:
          <input type='button' value='JButton'>
        </div>
        <div>checkbox:
          <input type='checkbox' id='checkbox1' name='checkbox1'>
        </div>
        <div>image:
          <input type='image' id='image1' name='image1' src='$src'>
        </div>
        <div>password:
          <input type='password' id='password1' name='password1'>
        </div>
        <div>radio:
          <input type='radio' id='radio1' name='radio1'>
        </div>
        <div>reset:
          <input type='reset' id='reset1' name='reset1'>
        </div>
        <div>submit:
          <input type='submit' id='submit1' name='submit1'>
        </div>
        <div>text:
          <input type='text' id='text1' name='text1'>
        </div>
        <div>file:
          <input type='file' id='file1' name='file1'>
        </div>
        <div>
          <isindex id='search1' name='search1' action='#'>
        </div>
        <div>
          <isindex id='search2' name='search2' prompt='search: ' action='#'>
        </div>
      </form>
      <br />
      <hr />
      $FORM3
    </body>
    </html>
  """.trimIndent()
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
