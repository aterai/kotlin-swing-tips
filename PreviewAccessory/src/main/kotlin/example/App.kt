package example

import java.awt.*
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.File
import javax.swing.*
import kotlin.math.roundToInt

fun makeUI(): Component {
  val chooser = object : JFileChooser() {
    override fun updateUI() {
      super.updateUI()
      accessory = ImagePreview(this)
    }
  }

  val button = JButton("Open JFileChooser")
  button.addActionListener {
    SwingUtilities.updateComponentTreeUI(chooser)
    chooser.showOpenDialog(button.rootPane)
  }

  return JPanel(GridBagLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(button)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ImagePreview(fc: JFileChooser) : JComponent(), PropertyChangeListener {
  private var thumbnail: ImageIcon? = null
  private var file: File? = null

  init {
    fc.addPropertyChangeListener(this)
  }

  override fun getPreferredSize() = Dimension(PREVIEW_WIDTH + PREVIEW_MARGIN * 2, 50)

  override fun propertyChange(e: PropertyChangeEvent) {
    val update = when (e.propertyName) {
      JFileChooser.DIRECTORY_CHANGED_PROPERTY -> {
        file = null
        true
      }
      JFileChooser.SELECTED_FILE_CHANGED_PROPERTY -> {
        file = e.newValue as? File
        true
      }
      else -> {
        file = null
        false
      }
    }
    if (update) {
      thumbnail = null
      if (isShowing) {
        thumbnail = getImageThumbnail(file)
        repaint()
      }
    }
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    thumbnail = (thumbnail ?: getImageThumbnail(file))?.also {
      val x = PREVIEW_MARGIN.coerceAtLeast(width / 2 - it.iconWidth / 2)
      val y = 0.coerceAtLeast(height / 2 - it.iconHeight / 2)
      it.paintIcon(this, g, x, y)
    }
  }

  private fun getImageThumbnail(file: File?): ImageIcon? {
    if (file == null || !file.exists()) {
      return null
    }
    val tmpIcon = ImageIcon(file.path)
    return if (tmpIcon.iconWidth > PREVIEW_WIDTH) {
      val scale = PREVIEW_WIDTH / tmpIcon.iconWidth.toFloat()
      val newW = (tmpIcon.iconWidth * scale).roundToInt()
      val newH = (tmpIcon.iconHeight * scale).roundToInt()
      val img = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
      val g2 = img.createGraphics()
      g2.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR,
      )
      g2.drawImage(tmpIcon.image, 0, 0, newW, newH, null)
      g2.dispose()
      ImageIcon(img)
    } else {
      tmpIcon
    }
  }

  companion object {
    private const val PREVIEW_WIDTH = 90
    private const val PREVIEW_MARGIN = 5
  }
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
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
