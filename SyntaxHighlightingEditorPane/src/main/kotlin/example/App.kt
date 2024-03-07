package example

import java.awt.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.swing.*
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
  htmlEditorKit.styleSheet = styleSheet
  editor.editorKit = htmlEditorKit
  editor.isEditable = false

  val button = JButton("open")
  button.addActionListener {
    val fileChooser = JFileChooser()
    val ret = fileChooser.showOpenDialog(button.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      loadFile(fileChooser.selectedFile.absolutePath)
    }
  }

  val box = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.add(Box.createHorizontalGlue())
    it.add(button)
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(editor))
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun loadFile(path: String) {
  val html = runCatching {
    File(path).useLines { it.toList() }.joinToString("\n") {
      it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
  }.fold(
    onSuccess = { prettify(engine, it) },
    onFailure = { it.message },
  )
  editor.text = "<pre>$html\n</pre>"
}

fun createEngine(): ScriptEngine? {
  val engine = ScriptEngineManager().getEngineByName("JavaScript")
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/prettify.js") ?: return null
  return runCatching {
    Files.newBufferedReader(Paths.get(url.toURI())).use {
      engine.eval("var window={}, navigator=null;")
      engine.eval(it)
    }
    engine
  }.onFailure { it.printStackTrace() }.getOrNull()
}

fun prettify(engine: ScriptEngine?, src: String) = runCatching {
  (engine as? Invocable)?.invokeMethod(engine["window"], "prettyPrintOne", src) as? String
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
