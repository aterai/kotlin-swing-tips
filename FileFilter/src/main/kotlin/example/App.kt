package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.File
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter

fun makeUI(): Component {
  val fileChooser = JFileChooser()
  fileChooser.addChoosableFileFilter(PngFileFilter())
  fileChooser.addChoosableFileFilter(JpgFileFilter())

  val filter = FileNameExtensionFilter("*.jpg, *.jpeg", "jpg", "jpeg")
  fileChooser.addChoosableFileFilter(filter)
  fileChooser.fileFilter = filter

  val log = JTextArea()
  val button = JButton("showOpenDialog")
  button.addActionListener {
    val retValue = fileChooser.showOpenDialog(button.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.append("${fileChooser.selectedFile}\n")
    }
  }

  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder("JFileChooser#showOpenDialog(...)")
  p.add(button)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class PngFileFilter : FileFilter() {
  override fun accept(file: File) =
    file.isDirectory || file.name.toLowerCase(Locale.ENGLISH).endsWith(".png")

  override fun getDescription() = "PNG(*.png)"
}

private class JpgFileFilter : FileFilter() {
  override fun accept(file: File) =
    file.isDirectory || file.name.toLowerCase(Locale.ENGLISH).endsWith(".jpg")

  override fun getDescription() = "JPEG(*.jpg)"
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
