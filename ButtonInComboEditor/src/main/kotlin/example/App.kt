package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val image1 = ImageIcon(cl.getResource("example/favicon.png"))
  val image2 = ImageIcon(cl.getResource("example/16x16.png"))
  val rss = ImageIcon(cl.getResource("example/feed-icon-14x14.png")) // http://feedicons.com/

  val combo01 = JComboBox(makeModel(image1, image2))
  initComboBox(combo01)

  val combo02 = SiteItemComboBox(makeModel(image1, image2), rss)
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

private fun makeModel(i1: Icon, i2: Icon) = DefaultComboBoxModel<SiteItem>().also {
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

private class SiteItemComboBox(model: DefaultComboBoxModel<SiteItem>, rss: ImageIcon) : JComboBox<SiteItem>(model) {
  init {
    val feedButton = makeRssButton(rss)
    val favicon = makeLabel()
    layout = SiteComboBoxLayout(favicon, feedButton)
    add(feedButton)
    add(favicon)

    val fl = object : FocusListener {
      override fun focusGained(e: FocusEvent) {
        // field.setBorder(BorderFactory.createEmptyBorder(0, 16 + 4, 0, 0));
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
    updateFavicon(model, favicon)
  }

  private fun updateFavicon(model: ComboBoxModel<SiteItem>, label: JLabel) {
    EventQueue.invokeLater {
      getSiteItemFromModel(model, selectedItem)?.also { label.icon = it.favicon }
    }
  }

  private fun makeRssButton(rss: ImageIcon) = JButton(rss).also {
    val ip = FilteredImageSource(rss.image.source, SelectedImageFilter())
    it.rolloverIcon = ImageIcon(Toolkit.getDefaultToolkit().createImage(ip))
    // it.setRolloverIcon(makeFilteredImage(rss));
    // it.setRolloverIcon(makeFilteredImage2(rss));
    it.addActionListener { println("clicked...") }
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

  private fun getSiteItemFromModel(model: ComboBoxModel<SiteItem>, o: Any?): SiteItem? {
    if (o is SiteItem) {
      return o
    }
    val str = o?.toString() ?: ""
    return (0 until model.size).map { model.getElementAt(it) }.firstOrNull { it.url == str }
  }
}

private class SiteComboBoxLayout(private val favicon: JLabel?, private val feedButton: JButton?) : LayoutManager {
  override fun addLayoutComponent(name: String, comp: Component) { /* not needed */
  }

  override fun removeLayoutComponent(comp: Component) { /* not needed */
  }

  override fun preferredLayoutSize(parent: Container): Dimension? = parent.preferredSize

  override fun minimumLayoutSize(parent: Container): Dimension? = parent.minimumSize

  override fun layoutContainer(parent: Container) {
    val cb = parent as? JComboBox<*> ?: return
    val width = cb.width
    val height = cb.height
    val ins = cb.insets
    val arrowHeight = height - ins.top - ins.bottom
    var arrowWidth = arrowHeight
    var faviconWidth = arrowHeight
    var feedWidth = 0

    // Arrow Icon JButton
    (cb.getComponent(0) as? JButton)?.also {
      val arrowInsets = it.insets
      arrowWidth = it.preferredSize.width + arrowInsets.left + arrowInsets.right
      it.setBounds(width - ins.right - arrowWidth, ins.top, arrowWidth, arrowHeight)
    }

    // Favicon JLabel
    favicon?.also {
      val faviconInsets = it.insets
      faviconWidth = it.preferredSize.width + faviconInsets.left + faviconInsets.right
      it.setBounds(ins.left, ins.top, faviconWidth, arrowHeight)
    }

    // Feed Icon JButton
    feedButton?.takeIf { it.isVisible }?.also {
      val feedInsets = it.insets
      feedWidth = it.preferredSize.width + feedInsets.left + feedInsets.right
      it.setBounds(width - ins.right - feedWidth - arrowWidth, ins.top, feedWidth, arrowHeight)
    }

    // JComboBox Editor
    cb.editor.editorComponent?.also {
      it.setBounds(
        ins.left + faviconWidth,
        ins.top,
        width - ins.left - ins.right - arrowWidth - faviconWidth - feedWidth,
        height - ins.top - ins.bottom
      )
    }
  }
}

private data class SiteItem(val url: String, val favicon: Icon, val hasRss: Boolean) {
  override fun toString() = url
}

private class SelectedImageFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int): Int {
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
