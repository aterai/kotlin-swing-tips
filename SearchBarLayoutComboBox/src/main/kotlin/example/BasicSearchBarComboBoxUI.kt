package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

@Suppress("TooManyFunctions")
class BasicSearchBarComboBoxUI : SearchBarComboBoxUI() {
  protected var popupMenuListener: PopupMenuListener? = null
  protected var loupeButton: JButton? = null
  protected var loupeAction: Action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      comboBox.setPopupVisible(false)
      val o = listBox.getSelectedValue() ?: comboBox.getItemAt(0)
      println(o)
      // println(o + ": " + comboBox?.getEditor()?.getItem())
    }
  }

  // protected boolean isEditable = true;
  protected override fun installDefaults() {
    super.installDefaults()
    // comboBox.setEditable(true)
    comboBox.putClientProperty("JComboBox.isTableCellEditor", java.lang.Boolean.TRUE)
    // comboBox.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT)
  }

  protected override fun installListeners() {
    super.installListeners()
    popupMenuListener = createPopupMenuListener()
    comboBox.addPopupMenuListener(popupMenuListener)
  }

  protected override fun uninstallListeners() {
    super.uninstallListeners()
    comboBox.removePopupMenuListener(popupMenuListener)
  }

  protected fun createPopupMenuListener(): PopupMenuListener? {
    if (popupMenuListener == null) {
      popupMenuListener = object : PopupMenuListener {
        private var str: String? = null
        override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
          val combo = e.getSource() as JComboBox<*>
          str = combo.getEditor().getItem().toString()
        }

        override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
          val combo = e.getSource() as? JComboBox<*> ?: return
          val se = listBox.getSelectedValue() as? SearchEngine ?: return
          arrowButton.setIcon(se.favicon)
          arrowButton.setRolloverIcon(makeRolloverIcon(se.favicon))
          combo.getEditor().setItem(str)
        }

        override fun popupMenuCanceled(e: PopupMenuEvent) { /* not needed */
        }
      }
    }
    return popupMenuListener
  }

  // // NullPointerException at BasicComboBoxUI#isNavigationKey(int keyCode, int modifiers)
  // private static class DummyKeyAdapter extends KeyAdapter { /* dummy */ }
  // @Override protected KeyListener createKeyListener() {
  //   if (Objects.isNull(keyListener)) {
  //     keyListener = new DummyKeyAdapter();
  //   }
  //   return keyListener;
  // }

  protected override fun configureEditor() {
    // super.configureEditor()
    // Should be in the same state as the combobox
    editor.setEnabled(comboBox.isEnabled())
    editor.setFocusable(comboBox.isFocusable())
    editor.setFont(comboBox.getFont())
    // editor.addFocusListener(getHandler())
    // comboBox.getEditor().addActionListener(getHandler())
    (editor as? JComponent)?.also {
      // it.putClientProperty("doNotCancelPopup", HIDE_POPUP_KEY)
      it.setInheritsPopupMenu(true)
      it.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0))
      it.getActionMap().put("loupe", loupeAction)
      val im = it.getInputMap(JComponent.WHEN_FOCUSED)
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "loupe")
    }
    comboBox.configureEditor(comboBox.getEditor(), comboBox.getSelectedItem())
    editor.addPropertyChangeListener(propertyChangeListener)
  }

  protected override fun createArrowButton() = TriangleArrowButton()

  override fun configureArrowButton() {
    super.configureArrowButton()
    arrowButton?.also {
      it.setBackground(UIManager.getColor("Panel.background"))
      it.setHorizontalAlignment(SwingConstants.LEFT)
      it.setOpaque(true)
      it.setFocusPainted(false)
      it.setContentAreaFilled(false)
      it.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createMatteBorder(0, 0, 0, 1, Color(0x7F9DB9)),
          BorderFactory.createEmptyBorder(1, 1, 1, 1)))
    }
  }

  protected override fun installComponents() {
    // super.installComponents();
    arrowButton = createArrowButton()
    comboBox.add(arrowButton)
    configureArrowButton()

    loupeButton = createLoupeButton()
    comboBox.add(loupeButton)
    configureLoupeButton()

    // if (comboBox.isEditable())
    addEditor()
    comboBox.add(currentValuePane)
  }

  protected override fun uninstallComponents() {
    if (loupeButton != null) {
      unconfigureLoupeButton()
    }
    loupeButton = null
    super.uninstallComponents()
  }

  protected fun createLoupeButton(): JButton {
    val button = JButton(loupeAction)
    val loupe = ImageIcon(BasicSearchBarComboBoxUI::class.java.getResource("loupe.png"))
    button.setIcon(loupe)
    button.setRolloverIcon(makeRolloverIcon(loupe))
    return button
  }

  fun configureLoupeButton() {
    loupeButton?.also {
      it.setName("ComboBox.loupeButton")
      it.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
      it.setEnabled(comboBox.isEnabled())
      it.setFocusable(comboBox.isFocusable())
      it.setOpaque(false)
      it.setRequestFocusEnabled(false)
      it.setFocusPainted(false)
      it.setContentAreaFilled(false)
      // it.addMouseListener(popup.getMouseListener())
      // it.addMouseMotionListener(popup.getMouseMotionListener())
      it.resetKeyboardActions()
      // it.putClientProperty("doNotCancelPopup", HIDE_POPUP_KEY)
      it.setInheritsPopupMenu(true)
    }
  }

  protected fun unconfigureLoupeButton() {
    loupeButton?.setAction(null)
  }

  protected override fun createRenderer() = SearchEngineListCellRenderer<SearchEngine>()

  protected override fun createLayoutManager() = SearchBarLayout()

  protected fun makeRolloverIcon(srcIcon: Icon): Icon {
    val op = RescaleOp(floatArrayOf(1.2f, 1.2f, 1.2f, 1f), floatArrayOf(0f, 0f, 0f, 0f), null)
    val img = BufferedImage(srcIcon.getIconWidth(), srcIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB)
    val g = img.getGraphics()
    // g.drawImage(srcIcon.getImage(), 0, 0, null)
    srcIcon.paintIcon(null, g, 0, 0)
    g.dispose()
    return ImageIcon(op.filter(img, null))
  }

//  companion object {
//    // "Accidental override" reported when a @JvmStatic method in a Kotlin class has
//    //   the same signature as a static method in a Java base class : KT-12993
//    // https://youtrack.jetbrains.com/issue/KT-12993
//    @JvmStatic
//    fun createUI(c: JComponent): ComponentUI {
//      return BasicSearchBarComboBoxUI()
//    }
//  }
}
