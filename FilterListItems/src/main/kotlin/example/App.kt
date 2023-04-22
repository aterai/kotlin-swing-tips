package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import java.util.regex.Pattern
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private val defaultModel = arrayOf(
  ListItem("example/wi0009-32.png"),
  ListItem("example/wi0054-32.png"),
  ListItem("example/wi0062-32.png"),
  ListItem("example/wi0063-32.png"),
  ListItem("example/wi0064-32.png"),
  ListItem("example/wi0096-32.png"),
  ListItem("example/wi0111-32.png"),
  ListItem("example/wi0122-32.png"),
  ListItem("example/wi0124-32.png"),
  ListItem("example/wi0126-32.png")
)
private val model = DefaultListModel<ListItem>()
private val list = object : JList<ListItem>(model) {
  override fun updateUI() {
    selectionForeground = null // Nimbus
    selectionBackground = null // Nimbus
    cellRenderer = null
    super.updateUI()
    layoutOrientation = HORIZONTAL_WRAP
    visibleRowCount = 0
    fixedCellWidth = 82
    fixedCellHeight = 64
    border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
    cellRenderer = ListItemListCellRenderer()
    selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
  }
}
private val field = JTextField(15)

fun makeUI(): Component {
  for (item in defaultModel) {
    model.addElement(item)
  }
  val listener = object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent) {
      filter()
    }

    override fun removeUpdate(e: DocumentEvent) {
      filter()
    }

    override fun changedUpdate(e: DocumentEvent) {
      // not needed
    }
  }
  field.document.addDocumentListener(listener)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(field, BorderLayout.NORTH)
    it.add(JScrollPane(list))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getPattern() = field.text
  ?.takeIf { it.isNotEmpty() }
  ?.let {
    runCatching {
      Pattern.compile(it)
    }.getOrNull()
  }

private fun filter() {
  getPattern()?.also { pattern ->
    // val selected = list.selectedValuesList
    model.clear()
    defaultModel
      .filter { item -> pattern.matcher(item.title).find() }
      .forEach { element -> model.addElement(element) }
    for (item in list.selectedValuesList) {
      val i = model.indexOf(item)
      list.addSelectionInterval(i, i)
    }
  }
}

private data class ListItem(val iconFile: String) {
  val img = makeImage(iconFile)
  val icon = ImageIcon(img)
  val selectedIcon: ImageIcon
  val title: String

  init {
    val ip = FilteredImageSource(img.source, SelectedImageFilter())
    selectedIcon = ImageIcon(Toolkit.getDefaultToolkit().createImage(ip))
    title = iconFile.split("/").last()
  }
}

private fun makeImage(path: String): Image {
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  return url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
}

private fun makeMissingImage(): Image {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class SelectedImageFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int) =
    argb and 0xFF_FF_FF_00.toInt() or (argb and 0xFF shr 1)
}

private class ListItemListCellRenderer : ListCellRenderer<ListItem> {
  private val renderer = JPanel(BorderLayout())
  private val icon = JLabel(null as? Icon?, SwingConstants.CENTER)
  private val label = JLabel("", SwingConstants.CENTER)
  private val focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
  private val noFocusBorder = UIManager.getBorder("List.noFocusBorder") ?: getSynthNoFocusBorder()

  init {
    icon.isOpaque = false
    label.foreground = renderer.foreground
    label.background = renderer.background
    label.border = noFocusBorder
    renderer.isOpaque = false
    renderer.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    renderer.add(icon)
    renderer.add(label, BorderLayout.SOUTH)
  }

  private fun getSynthNoFocusBorder(): Border {
    val i = focusBorder.getBorderInsets(label)
    return BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right)
  }

  override fun getListCellRendererComponent(
    list: JList<out ListItem>,
    value: ListItem?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    if (value != null) {
      label.text = value.title
      label.border = if (cellHasFocus) focusBorder else noFocusBorder
      if (isSelected) {
        icon.icon = value.selectedIcon
        label.foreground = list.selectionForeground
        label.background = list.selectionBackground
        label.isOpaque = true
      } else {
        icon.icon = value.icon
        label.foreground = list.foreground
        label.background = list.background
        label.isOpaque = false
      }
    }
    return renderer
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
