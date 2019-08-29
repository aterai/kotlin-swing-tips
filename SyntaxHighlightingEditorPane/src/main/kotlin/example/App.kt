package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

val editor = JEditorPane()
val engine = createEngine()

fun makeUI(): Component {
  val styleSheet = StyleSheet().also {
    it.addRule(".str {color:#008800}")
    it.addRule(".kwd {color:#000088}")
    it.addRule(".com {color:#880000}")
    it.addRule(".typ {color:#660066}")
    it.addRule(".lit {color:#006666}")
    it.addRule(".pun {color:#666600}")
    it.addRule(".pln {color:#000000}")
    it.addRule(".tag {color:#000088}")
    it.addRule(".atn {color:#660066}")
    it.addRule(".atv {color:#008800}")
    it.addRule(".dec {color:#660066}")
  }

  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.setStyleSheet(styleSheet)
  editor.setEditorKit(htmlEditorKit)
  editor.setEditable(false)

  val button = JButton("open")
  button.addActionListener {
    val fileChooser = JFileChooser()
    val ret = fileChooser.showOpenDialog(button.getRootPane())
    if (ret == JFileChooser.APPROVE_OPTION) {
      loadFile(fileChooser.getSelectedFile().getAbsolutePath())
    }
  }

  val box = Box.createHorizontalBox().also {
    it.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
    it.add(Box.createHorizontalGlue())
    it.add(button)
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(editor))
    it.add(box, BorderLayout.SOUTH)
    it.setPreferredSize(Dimension(320, 240))
  }
}

fun loadFile(path: String) {
  val html = runCatching {
    File(path).useLines { it.toList() }.joinToString("\n") {
      it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
  }.fold(
    onSuccess = { prettify(engine, it) },
    onFailure = { it.message }
  )
  editor.setText("<pre>$html\n</pre>")
}

fun createEngine(): ScriptEngine? {
  val engine = ScriptEngineManager().getEngineByName("JavaScript")
  val cl = Thread.currentThread().getContextClassLoader()
  val url = cl.getResource("example/prettify.js")
  return runCatching {
    BufferedReader(InputStreamReader(url.openStream(), StandardCharsets.UTF_8)).use { r ->
      engine.eval("var window={}, navigator=null;")
      engine.eval(r)
    }
    engine
  }.onFailure { it.printStackTrace() }.getOrNull()
}

fun prettify(engine: ScriptEngine?, src: String) = runCatching {
  (engine as? Invocable)?.invokeMethod(engine.get("window"), "prettyPrintOne", src) as? String
}.getOrNull() ?: "error"

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
