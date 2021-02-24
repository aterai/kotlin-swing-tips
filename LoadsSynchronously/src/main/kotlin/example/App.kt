package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.Element
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.ImageView
import kotlin.math.roundToInt

private const val TEXT = "<span style='background-color:red'>1111111111111111111</span><br />\n"
private val label = JLabel("screenshot")

fun makeUI(): Component {
  label.verticalAlignment = SwingConstants.CENTER
  label.verticalTextPosition = SwingConstants.BOTTOM
  label.horizontalAlignment = SwingConstants.CENTER
  label.horizontalTextPosition = SwingConstants.CENTER

  val cl = Thread.currentThread().contextClassLoader
  val path = cl.getResource("example/GIANT_TCR1_2013.jpg")
  val html1 = TEXT.repeat(50)
  val html2 = TEXT.repeat(3)
  val tabs = JTabbedPane()
  val img0 = "<p><img src='$path'></p>"
  val st0 = html1 + img0 + html2
  val editor0 = JEditorPane()
  editor0.editorKit = HTMLEditorKit()
  editor0.text = st0
  tabs.addTab("default", JScrollPane(editor0))

  val w = 2048
  val h = 1360
  val img1 = "<p><img src='$path' width='$w' height='$h'></p>"
  val st1 = html1 + img1 + html2
  val editor1 = JEditorPane()
  editor1.editorKit = HTMLEditorKit()
  editor1.text = st1
  tabs.addTab("<img width='%d' ...", JScrollPane(editor1))

  // [JDK-8223384] ImageView incorrectly calculates size when synchronously loaded - Java Bug System
  // https://bugs.openjdk.java.net/browse/JDK-8223384
  val editor2 = JEditorPane()
  editor2.editorKit = ImageLoadSynchronouslyHtmlEditorKit()
  tabs.addTab("LoadsSynchronously", JScrollPane(editor2))
  tabs.addChangeListener {
    when (tabs.selectedIndex) {
      2 -> {
        editor2.text = st0
        saveImage(editor2)
      }
      1 -> {
        editor1.text = st1
        saveImage(editor1)
      }
      else -> {
        editor0.text = st0
        saveImage(editor0)
      }
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.add(label, BorderLayout.EAST)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun saveImage(c: JComponent) {
  EventQueue.invokeLater {
    val s = .02f
    val w = (c.width * s).roundToInt()
    val h = (c.height * s).roundToInt()
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g2 = image.createGraphics()
    g2.scale(s.toDouble(), s.toDouble())
    c.print(g2)
    g2.dispose()
    try {
      val tmp = File.createTempFile("jst_tmp", ".jpg")
      tmp.deleteOnExit()
      ImageIO.write(image, "jpeg", tmp)
      label.icon = ImageIcon(tmp.absolutePath)
    } catch (ex: IOException) {
      ex.printStackTrace()
      label.icon = null
      Toolkit.getDefaultToolkit().beep()
    }
  }
}

private class ImageLoadSynchronouslyHtmlEditorKit : HTMLEditorKit() {
  override fun getViewFactory() = object : HTMLFactory() {
    override fun create(elem: Element) = super.create(elem).also {
      if (it is ImageView) {
        it.loadsSynchronously = true
      }
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
