package example

import java.awt.*
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import javax.swing.*
import javax.swing.event.HyperlinkEvent

private const val SITE = "https://ateraimemo.com/"

fun makeUI(): Component {
  val textArea = JTextArea()
  val editor = JEditorPane("text/html", "<html><a href='$SITE'>$SITE</a>")
  editor.isOpaque = false
  editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  editor.isEditable = false
  editor.addHyperlinkListener { e ->
    if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
      BrowserLauncher.openUrl(SITE)
      textArea.text = e.toString()
    }
  }
  val p = JPanel()
  p.add(editor)
  p.border = BorderFactory.createTitledBorder("BrowserLauncher.openUrl(...)")
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

// Bare Bones Browser Launch
// Version 1.5
// December 10, 2005
// Supports: Mac OS X, GNU/Linux, Unix, Windows XP
// Example Usage:
//   String url = "https://centerkey.com/";
//   BareBonesBrowserLaunch.openUrl(url);
// Public Domain Software -- Free to Use as You Like
private object BrowserLauncher {
  private const val ERR_MSG = "Error attempting to launch web browser"

  fun openUrl(url: String) {
    val osName = System.getProperty("os.name")
    runCatching {
      when {
        osName.startsWith("Mac OS") -> macOpenUrl(url)
        osName.startsWith("Windows") -> windowsOpenUrl(url)
        else -> linuxOpenUrl(url)
      }
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
      val msg = "$ERR_MSG: ${it.localizedMessage}"
      JOptionPane.showMessageDialog(null, msg, "title", JOptionPane.ERROR_MESSAGE)
    }
  }

  @Throws(
    ClassNotFoundException::class,
    NoSuchMethodException::class,
    IllegalAccessException::class,
    InvocationTargetException::class
  )
  private fun macOpenUrl(url: String) {
    val fileMgr = Class.forName("com.apple.eio.FileManager")
    val openUrl = fileMgr.getDeclaredMethod("openURL", String::class.java)
    openUrl.invoke(null, url)
  }

  @Throws(IOException::class)
  private fun windowsOpenUrl(url: String) {
    Runtime.getRuntime().exec("rundll32 url.dll, FileProtocolHandler $url")
  }

  @Throws(InterruptedException::class, IOException::class)
  private fun linuxOpenUrl(url: String) {
    val browsers = arrayOf("firefox", "mozilla", "netscape")
    var browser: String? = null
    var count = 0
    while (count < browsers.size && browser == null) {
      val cmd = arrayOf("which", browsers[count])
      if (Runtime.getRuntime().exec(cmd).waitFor() == 0) {
        browser = browsers[count]
      }
      count++
    }
    if (browser != null) {
      Runtime.getRuntime().exec(arrayOf(browser, url))
    } else {
      throw UnsupportedOperationException("Could not find web browser")
    }
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
