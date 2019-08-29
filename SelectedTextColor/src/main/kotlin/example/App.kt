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

val editor1 = JEditorPane()
val editor2 = JEditorPane().also {
  it.setSelectedTextColor(null)
  it.setSelectionColor(Color(0x64_88_AA_AA, true))
  // TEST: it.setSelectionColor(null);
}
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

  listOf(editor1, editor2).forEach {
    it.setEditorKit(htmlEditorKit)
    it.setEditable(false)
    // it.setSelectionColor(new Color(0x64_88_AA_AA, true));
    it.setBackground(Color(0xEE_EE_EE))
  }

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

  val p = JPanel(GridLayout(2, 1)).also {
    it.add(JScrollPane(editor1))
    it.add(JScrollPane(editor2))
  }
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.setPreferredSize(Dimension(320, 240))
  }
}

fun loadFile(path: String) {
  // try {
  //   // val txt = Files.lines(Paths.get(path), StandardCharsets.UTF_8).map {
  //   //   it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
  //   // }.collect(Collectors.joining("\n"))
  //   // By default uses UTF-8 charset.
  //   val txt = File(path).useLines { it.toList() }.map {
  //     it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
  //   }.joinToString("\n")
  //   val html = "<pre>${prettify(engine, txt)}\n</pre>"
  //   editor1.setText(html)
  //   editor2.setText(html)
  // } catch (ex: IOException) {
  //   ex.printStackTrace()
  // }
  val html = runCatching {
    File(path).useLines { it.toList() }.joinToString("\n") {
      it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
  }.fold(
    onSuccess = { prettify(engine, it) },
    onFailure = { it.message }
  )
  editor1.setText("<pre>$html\n</pre>")
  editor2.setText("<pre>$html\n</pre>")
}

fun createEngine(): ScriptEngine? {
  // val manager = ScriptEngineManager()
  val engine = ScriptEngineManager().getEngineByName("JavaScript")
  // String p = "https://raw.githubusercontent.com/google/code-prettify/" +
  //            "f5ad44e3253f1bc8e288477a36b2ce5972e8e161/src/prettify.js";
  // URL url = new URL(p);

  // Files.newBufferedReader(path, StandardCharsets.UTF_8).use { r ->
  //   engine.eval("var window={}, navigator=null;")
  //   engine.eval(r)
  //   return engine
  // }

  val cl = Thread.currentThread().getContextClassLoader()
  val url = cl.getResource("example/prettify.js") ?: return null
  return runCatching {
    BufferedReader(InputStreamReader(url.openStream(), StandardCharsets.UTF_8)).use { r ->
      engine.eval("var window={}, navigator=null;")
      engine.eval(r)
    }
    engine
  }.onFailure { it.printStackTrace() }.getOrNull()

  // try {
  //   BufferedReader(InputStreamReader(url.openStream(), StandardCharsets.UTF_8)).use { r ->
  //     engine.eval("var window={}, navigator=null;")
  //     engine.eval(r)
  //     return engine
  //   }
  // } catch (ex: IOException) {
  //   ex.printStackTrace()
  // } catch (ex: ScriptException) {
  //   ex.printStackTrace()
  // }
  // return null
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
