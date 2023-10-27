package example

import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val image1 = makeImage("example/favicon.png")
  val image2 = makeImage("example/16x16.png")
  val combo01 = JComboBox(makeModel(image1, image2))
  initComboBox(combo01)

  val combo02 = SiteItemComboBox(makeModel(image1, image2))
  initComboBox(combo02)

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createTitledBorder("setEditable(true)")
  box.add(Box.createVerticalStrut(2))
  box.add(combo01)
  box.add(Box.createVerticalStrut(5))
  box.add(combo02)
  box.add(Box.createVerticalStrut(2))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea()))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeImage(path: String): Image {
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  return url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
}

private fun makeMissingImage(): Image {
  val missingIcon = UIManager.getIcon("html.missingImage")
  val iw = missingIcon.iconWidth
  val ih = missingIcon.iconHeight
  val bi = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, (16 - iw) / 2, (16 - ih) / 2)
  g2.dispose()
  return bi
}

private fun makeModel(i1: Image, i2: Image) = DefaultComboBoxModel<SiteItem>().also {
  it.addElement(SiteItem("https://ateraimemo.com/", i1, true))
  it.addElement(SiteItem("https://ateraimemo.com/Swing.html", i1, true))
  it.addElement(SiteItem("https://ateraimemo.com/Kotlin.html", i1, true))
  it.addElement(SiteItem("https://github.com/aterai/java-swing-tips", i2, true))
  it.addElement(SiteItem("https://java-swing-tips.blogspot.com/", i2, true))
  it.addElement(SiteItem("http://www.example.com/", i2, false))
}

private fun initComboBox(combo: JComboBox<SiteItem>) {
  combo.isEditable = true
  val renderer = combo.renderer
  combo.setRenderer { list, value, index, isSelected, cellHasFocus ->
    renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
      (it as? JLabel)?.icon = value?.favicon
    }
  }
}

private class SiteItemComboBox(
  model: DefaultComboBoxModel<SiteItem>,
) : JComboBox<SiteItem>(model) {
  init {
    val favicon = makeLabel()
    val feedButton = makeRssButton()
    layout = SiteComboBoxLayout(favicon, feedButton)
    add(feedButton)
    add(favicon)

    val fl = object : FocusListener {
      override fun focusGained(e: FocusEvent) {
        feedButton.isVisible = false
      }

      override fun focusLost(e: FocusEvent) {
        val field = e.component as? JTextField ?: return
        getSiteItemFromModel(model, field.text)?.also { item ->
          model.removeElement(item)
          model.insertElementAt(item, 0)
          favicon.icon = item.favicon
          feedButton.isVisible = item.hasRss
          selectedIndex = 0
        }
      }
    }
    (getEditor().editorComponent as? JTextField)?.addFocusListener(fl)

    addItemListener { e ->
      if (e.stateChange == ItemEvent.SELECTED) {
        updateFavicon(model, favicon)
      }
    }
    EventQueue.invokeLater {
      updateFavicon(model, favicon)
    }
  }

  private fun updateFavicon(model: ComboBoxModel<SiteItem>, label: JLabel) {
    getSiteItemFromModel(model, selectedItem)?.also { label.icon = it.favicon }
  }

  private fun makeRssButton() = JButton().also {
    val rss = makeImage("example/feed-icon-14x14.png") // http://feedicons.com/
    it.icon = ImageIcon(rss)
    val ip = FilteredImageSource(rss.source, SelectedImageFilter())
    it.rolloverIcon = ImageIcon(it.toolkit.createImage(ip))
    it.addActionListener { Toolkit.getDefaultToolkit().beep() }
    it.isFocusPainted = false
    it.isBorderPainted = false
    it.isContentAreaFilled = false
    it.cursor = Cursor.getDefaultCursor()
    it.border = BorderFactory.createEmptyBorder(0, 1, 0, 2)
  }

  private fun makeLabel(): JLabel {
    val label = JLabel()
    label.cursor = Cursor.getDefaultCursor()
    label.border = BorderFactory.createEmptyBorder(0, 1, 0, 2)
    val ml = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        EventQueue.invokeLater {
          (editor.editorComponent as? JTextField)?.also {
            it.requestFocusInWindow()
            it.selectAll()
          }
        }
      }
    }
    label.addMouseListener(ml)
    return label
  }

  private fun getSiteItemFromModel(
    model: ComboBoxModel<SiteItem>,
    o: Any?,
  ): SiteItem? {
    if (o is SiteItem) {
      return o
    }
    val str = o?.toString() ?: ""
    return (0 until model.size).map { model.getElementAt(it) }.firstOrNull { it.url == str }
  }
}

private class SiteComboBoxLayout(
  private val favicon: JLabel?,
  private val feedButton: JButton?,
) : LayoutManager {
  override fun addLayoutComponent(
    name: String,
    comp: Component,
  ) {
    // not needed
  }

  override fun removeLayoutComponent(comp: Component) {
    // not needed
  }

  override fun preferredLayoutSize(parent: Container): Dimension? = parent.preferredSize

  override fun minimumLayoutSize(parent: Container): Dimension? = parent.minimumSize

  override fun layoutContainer(parent: Container) {
    val cb = parent as? JComboBox<*> ?: return
    val r = SwingUtilities.calculateInnerArea(cb, null)

    // Arrow Icon JButton
    var arrowWidth = 0
    (cb.getComponent(0) as? JButton)?.also {
      arrowWidth = it.preferredSize.width
      it.setBounds(r.x + r.width - arrowWidth, r.y, arrowWidth, r.height)
    }

    // Favicon JLabel
    var faviconWidth = 0
    favicon?.also {
      faviconWidth = it.preferredSize.width
      it.setBounds(r.x, r.y, faviconWidth, r.height)
    }

    // Feed Icon JButton
    var feedWidth = 0
    feedButton?.takeIf { it.isVisible }?.also {
      feedWidth = it.preferredSize.width
      it.setBounds(r.x + r.width - feedWidth - arrowWidth, r.y, feedWidth, r.height)
    }

    // JComboBox Editor
    cb.editor.editorComponent?.also {
      val w = r.width - arrowWidth - faviconWidth - feedWidth
      it.setBounds(r.x + faviconWidth, r.y, w, r.height)
    }
  }
}

private data class SiteItem(val url: String, val image: Image, val hasRss: Boolean) {
  val favicon = ImageIcon(image)

  override fun toString() = url
}

private class SelectedImageFilter : RGBImageFilter() {
  override fun filterRGB(
    x: Int,
    y: Int,
    argb: Int,
  ): Int {
    val r = minOf(0xFF, ((argb shr 16 and 0xFF) * SCALE).toInt())
    val g = minOf(0xFF, ((argb shr 8 and 0xFF) * SCALE).toInt())
    val b = minOf(0xFF, ((argb and 0xFF) * SCALE).toInt())
    // return argb and -0x1000000 or (r shl 16) or (g shl 8) or b
    return argb and 0xFF_00_00_00.toInt() or (r shl 16) or (g shl 8) or b
  }

  companion object {
    private const val SCALE = 1.2f
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
