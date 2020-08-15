package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val textPane = JTextPane()
  val cl = Thread.currentThread().contextClassLoader
  makeFont(cl.getResource("example/mona.ttf"))?.also {
    textPane.font = it.deriveFont(10f)
  }

  val url = cl.getResource("example/bar.utf8.txt")
  if (url != null) {
    runCatching {
      InputStreamReader(url.openStream(), StandardCharsets.UTF_8).use { reader ->
        textPane.read(reader, "text")
      }
    }.onFailure {
      textPane.text = it.message
      UIManager.getLookAndFeel().provideErrorFeedback(textPane)
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textPane))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeFont(url: URL?) = runCatching {
  url?.openStream().use {
    Font.createFont(Font.TRUETYPE_FONT, it).deriveFont(12f)
  }
}.getOrNull()

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
