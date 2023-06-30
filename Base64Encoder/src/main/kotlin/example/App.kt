package example

import java.awt.*
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.Base64
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

private val textArea = JTextArea().also {
  it.isEditable = false
  it.lineWrap = true
}
private val label = JLabel(" ", SwingConstants.CENTER)

fun makeUI(): Component {
  val sp1 = JScrollPane(textArea)
  sp1.border = BorderFactory.createTitledBorder("File -> String")

  val sp2 = JScrollPane(label)
  sp2.border = BorderFactory.createTitledBorder("JTextArea -> ImageIcon")

  val p = JPanel(GridLayout(2, 1, 5, 5))
  p.add(sp1)
  p.add(sp2)

  val encode = JButton("encode")
  encode.addActionListener {
    val chooser = JFileChooser()
    chooser.addChoosableFileFilter(FileNameExtensionFilter("PNG (*.png)", "png"))
    val retValue = chooser.showOpenDialog(encode)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      val path = chooser.selectedFile.toPath()
      runCatching {
        textArea.text = Base64.getEncoder().encodeToString(Files.readAllBytes(path))
      }.onFailure {
        textArea.text = it.message
      }
    }
  }

  val decode = JButton("decode")
  decode.addActionListener {
    val b64 = textArea.text
    if (b64.isNotEmpty()) {
      runCatching {
        // val dec = Base64.getDecoder().decode(b64.toByteArray(StandardCharsets.ISO_8859_1))
        val dec = Base64.getDecoder().decode(b64)
        ByteArrayInputStream(dec).use {
          label.icon = ImageIcon(ImageIO.read(it))
        }
      }.onFailure {
        label.icon = null
        label.text = it.message
      }
    }
  }

  val box = JPanel(GridLayout(1, 2, 5, 5)).also {
    it.border = BorderFactory.createTitledBorder("java.util.Base64")
    it.add(encode)
    it.add(decode)
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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
