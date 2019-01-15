package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.IOException
import java.net.URISyntaxException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.spi.FileSystemProvider
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

class MainPanel : JPanel(BorderLayout()) {
  private val editor1 = JEditorPane()
  private val editor2 = JEditorPane()
  private val engine = createEngine()

  init {
    val styleSheet = StyleSheet()
    styleSheet.addRule(".str {color:#008800}")
    styleSheet.addRule(".kwd {color:#000088}")
    styleSheet.addRule(".com {color:#880000}")
    styleSheet.addRule(".typ {color:#660066}")
    styleSheet.addRule(".lit {color:#006666}")
    styleSheet.addRule(".pun {color:#666600}")
    styleSheet.addRule(".pln {color:#000000}")
    styleSheet.addRule(".tag {color:#000088}")
    styleSheet.addRule(".atn {color:#660066}")
    styleSheet.addRule(".atv {color:#008800}")
    styleSheet.addRule(".dec {color:#660066}")

    val htmlEditorKit = HTMLEditorKit()
    htmlEditorKit.setStyleSheet(styleSheet)

    Stream.of(editor1, editor2).forEach({ e ->
      e.setEditorKit(htmlEditorKit)
      e.setEditable(false)
      // e.setSelectionColor(new Color(0x64_88_AA_AA, true));
      e.setBackground(Color(200, 200, 200))
    })

    editor2.setSelectedTextColor(null)
    editor2.setSelectionColor(Color(0x6488AAAA, true))
    // TEST: editor2.setSelectionColor(null);

    val button = JButton("open")
    button.addActionListener({
      val fileChooser = JFileChooser()
      val ret = fileChooser.showOpenDialog(getRootPane())
      if (ret == JFileChooser.APPROVE_OPTION) {
        loadFile(fileChooser.getSelectedFile().getAbsolutePath())
  }
    })

    val box = Box.createHorizontalBox()
    box.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
    box.add(Box.createHorizontalGlue())
    box.add(button)

    val p = JPanel(GridLayout(2, 1))
    p.add(JScrollPane(editor1))
    p.add(JScrollPane(editor2))
    add(p)
    add(box, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun loadFile(path: String) {
    try {
      Files.lines(Paths.get(path), Charset.forName("UTF-8")).use({ lines ->
        val txt = lines.map({ s -> s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") }).collect(Collectors.joining("\n"))
        val html = "<pre>" + prettify(engine!!, txt) + "\n</pre>"
        editor1.setText(html)
        editor2.setText(html)
      })
    } catch (ex: IOException) {
      ex.printStackTrace()
    }
  }

  companion object {

    private fun createEngine(): ScriptEngine? {
      val manager = ScriptEngineManager()
      val engine = manager.getEngineByName("JavaScript")
      try {
        val uri = MainPanel::class.java.getResource("prettify.js").toURI()
        // https://stackoverflow.com/questions/22605666/java-access-files-in-jar-causes-java-nio-file-filesystemnotfoundexception
        if ("jar".equals(uri.getScheme())) {
          for (provider in FileSystemProvider.installedProviders()) {
            if (provider.getScheme().equals("jar", ignoreCase = true)) {
              try {
                provider.getFileSystem(uri)
              } catch (e: FileSystemNotFoundException) {
                // in this case we need to initialize it first:
                provider.newFileSystem(uri, emptyMap<String, Any>())
              }
            }
          }
        }
        val path = Paths.get(uri)
        // Path path = Paths.get(MainPanel.class.getResource("prettify.js").toURI());

        // String p = "https://raw.githubusercontent.com/google/code-prettify/f5ad44e3253f1bc8e288477a36b2ce5972e8e161/src/prettify.js";
        // try (Reader r = new BufferedReader(new InputStreamReader(new URL(p).openStream(), StandardCharsets.UTF_8))) {
        Files.newBufferedReader(path, StandardCharsets.UTF_8).use({ r ->
          engine.eval("var window={}, navigator=null;")
          engine.eval(r)
          return engine
        })
      } catch (ex: IOException) {
        ex.printStackTrace()
      } catch (ex: ScriptException) {
        ex.printStackTrace()
      } catch (ex: URISyntaxException) {
        ex.printStackTrace()
      }

      return null
    }

    private fun prettify(engine: ScriptEngine, src: String): String {
      try {
        val w = engine.get("window")
        return ((engine as Invocable).invokeMethod(w, "prettyPrintOne", src) as String)
      } catch (ex: ScriptException) {
        ex.printStackTrace()
        return ""
      } catch (ex: NoSuchMethodException) {
        ex.printStackTrace()
        return ""
      }
    }
  }
}

fun main() {
  EventQueue.invokeLater({
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  })
}
