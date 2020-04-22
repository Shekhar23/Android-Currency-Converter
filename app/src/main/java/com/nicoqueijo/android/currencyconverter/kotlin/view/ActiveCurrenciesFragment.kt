package com.nicoqueijo.android.currencyconverter.kotlin.view

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.forEachIndexed
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.jmedeisis.draglinearlayout.DragLinearLayout
import com.nicoqueijo.android.currencyconverter.R
import com.nicoqueijo.android.currencyconverter.kotlin.model.Currency
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.copyToClipboard
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.hasOnlyOneElement
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.hide
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.isViewVisible
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.show
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.vibrate
import com.nicoqueijo.android.currencyconverter.kotlin.viewmodel.ActiveCurrenciesViewModel

class ActiveCurrenciesFragment : Fragment() {

    private lateinit var viewModel: ActiveCurrenciesViewModel

    private lateinit var emptyList: LinearLayout
    private lateinit var dragLinearLayout: DragLinearLayout
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var keyboard: DecimalNumberKeyboard
    private lateinit var scrollView: ScrollView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_active_currencies, container, false)
        viewModel = ViewModelProvider(this).get(ActiveCurrenciesViewModel::class.java)
        viewModel.initDefaultCurrencies()
        initViews(view)
        observeObservables()
        return view
    }

    private fun initViews(view: View) {
        emptyList = view.findViewById(R.id.empty_list)
        scrollView = view.findViewById(R.id.scroll_view)
        keyboard = view.findViewById(R.id.keyboard)
        initDragLinearLayout(view)
        initFloatingActionButton(view)
        initKeyboardListener()
        if (viewModel.wasListConstructed) {
            restoreActiveCurrencies()
        }
    }

    private fun initDragLinearLayout(view: View) {
        dragLinearLayout = view.findViewById<DragLinearLayout>(R.id.drag_linear_layout).apply {
            setContainerScrollView(scrollView)
            setOnViewSwapListener { _, startPosition, _, endPosition ->
                viewModel.swapCurrencies(startPosition, endPosition)
            }
        }
    }

    private fun initFloatingActionButton(view: View) {
        floatingActionButton = view.findViewById<FloatingActionButton>(R.id.floating_action_button).apply {
            setOnClickListener {
                findNavController().navigate(R.id.action_activeCurrenciesFragment_to_selectableCurrenciesFragment)
            }
        }
    }

    /**
     * If the [button] is a Button we know that belongs to chars 0-9 or the decimal
     * separator as those were declared as Buttons.
     * If the [button] is an ImageButton that can only mean that it is the backspace
     * key as that was the only one declared as an ImageButton.
     *
     * On each key click event, we want to validate the input against what already is in the
     * TextView. If it is valid we want to run the conversion of that value against all other
     * currencies and update the TextView of all other currencies.
     */
    private fun initKeyboardListener() {
        keyboard.onKeyClickedListener { button ->
            scrollToFocusedCurrency()
            if (viewModel.processKeyboardInput(button)) {
                viewModel.runConversions()
                updateRows()
            } else {
                vibrateAndShake()
            }
        }
        keyboard.onKeyLongClickedListener {
            scrollToFocusedCurrency()
            viewModel.clearConversions()
            updateRows()
        }
    }

    private fun updateRows() {
        dragLinearLayout.children
                .forEachIndexed { i, row ->
                    row as RowActiveCurrency
                    row.conversion.text = viewModel.memoryActiveCurrencies[i].conversion.conversionText
                }
    }

    private fun vibrateAndShake() {
        keyboard.context.vibrate()
        viewModel.run {
            val focusedRow = (dragLinearLayout[memoryActiveCurrencies
                    .indexOf(focusedCurrency.value)] as RowActiveCurrency)
            focusedRow.conversion.startAnimation(AnimationUtils
                    .loadAnimation(getApplication(), R.anim.shake))
        }
    }

    private fun scrollToFocusedCurrency() {
        viewModel.focusedCurrency.value?.let {
            val focusedRow = dragLinearLayout.getChildAt(viewModel.memoryActiveCurrencies.indexOf(it))
            if (!scrollView.isViewVisible(focusedRow)) {
                scrollView.smoothScrollTo(0, focusedRow.top)
            }
        }
    }

    private fun restoreActiveCurrencies() {
        viewModel.memoryActiveCurrencies.forEach { addRow(it) }
    }

    private fun observeObservables() {
        viewModel.databaseActiveCurrencies.observe(viewLifecycleOwner, Observer { databaseActiveCurrencies ->
            if (databaseActiveCurrencies.isNotEmpty()) {
                initActiveCurrencies(databaseActiveCurrencies)
                styleRows()
                toggleEmptyListViewVisibility()
            }
        })
        viewModel.focusedCurrency.observe(viewLifecycleOwner, Observer {
            updateHints()
        })
    }

    /**
     * Determines how it should inflate the list of currencies when the database storing the state
     * of the currencies emits updates.
     */
    private fun initActiveCurrencies(databaseActiveCurrencies: List<Currency>) {
        viewModel.run {
            if (!wasListConstructed) {
                constructActiveCurrencies(databaseActiveCurrencies)
            }
            if (wasCurrencyAddedViaFab(databaseActiveCurrencies)) {
                databaseActiveCurrencies.takeLast(1).single().let {
                    memoryActiveCurrencies.add(it)
                    addRow(it)
                    if (!memoryActiveCurrencies.hasOnlyOneElement()) {
                        runConversions()
                        updateRows()
                        scrollToFocusedCurrency()
                    }
                }
                this@ActiveCurrenciesFragment.updateHints()
            }
            setDefaultFocus()
        }
    }

    private fun updateHints() {
        viewModel.focusedCurrency.value?.let {
            viewModel.updateHints()
            dragLinearLayout.children
                    .forEachIndexed { i, row ->
                        row as RowActiveCurrency
                        row.conversion.hint = viewModel.memoryActiveCurrencies[i].conversion.conversionHint
                    }
        }
    }

    /**
     * This inflates the DragLinearLayout with the active currencies from the database when the
     * activity starts for the first time.
     */
    private fun constructActiveCurrencies(databaseActiveCurrencies: List<Currency>) {
        databaseActiveCurrencies.forEach { currency ->
            viewModel.memoryActiveCurrencies.add(currency)
            addRow(currency)
        }
        viewModel.wasListConstructed = true
    }

    private fun styleRows() {
        dragLinearLayout.forEachIndexed { i, row ->
            styleRow(viewModel.memoryActiveCurrencies[i], row as RowActiveCurrency)
        }
    }

    /**
     * Styles the row in accordance to the focus state of its Currency. A row containing a focused
     * Currency should have blinking cursor at the end of it's conversion field and a dark gray background.
     */
    private fun styleRow(currency: Currency, row: RowActiveCurrency) {
        row.run {
            if (currency.isFocused) {
                rowCanvas.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_gray))
                blinkingCursor.startAnimation(AnimationUtils.loadAnimation(viewModel.getApplication(), R.anim.blink))
            } else {
                rowCanvas.background = ContextCompat.getDrawable(context, R.drawable.row_active_currency_background)
                blinkingCursor.clearAnimation()
            }
        }
    }

    /**
     * Creates a row from a [currency], adds that row to the DragLinearLayout, and sets up
     * its listeners so it could be dragged, removed, and restored.
     */
    private fun addRow(currency: Currency) {
        RowActiveCurrency(activity).run row@{
            initRow(currency)
            dragLinearLayout.run {
                addView(this@row)
                setViewDraggable(this@row, this@row)
                /**
                 * Removes this Currency and adjusts the state accordingly.
                 */
                currencyCode.setOnLongClickListener {
                    val currencyToRemove = viewModel.memoryActiveCurrencies[indexOfChild(this@row)]
                    val positionOfCurrencyToRemove = currencyToRemove.order
                    viewModel.handleRemove(currencyToRemove, positionOfCurrencyToRemove)
                    activity?.vibrate()
                    layoutTransition = LayoutTransition()
                    removeDragView(this@row)
                    layoutTransition = null
                    styleRows()
                    toggleEmptyListViewVisibility()
                    /**
                     * Re-adds the removed Currency and restores the state before the Currency was removed.
                     */
                    Snackbar.make(this, R.string.item_removed, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.undo) {
                                layoutTransition = LayoutTransition()
                                addDragView(this@row, this@row, positionOfCurrencyToRemove)
                                layoutTransition = null
                                viewModel.handleUndo(currencyToRemove, positionOfCurrencyToRemove)
                                toggleEmptyListViewVisibility()
                            }.show()
                    true
                }
                conversion.setOnLongClickListener {
                    activity?.copyToClipboard(this@row.conversion.text)
                    true
                }
                conversion.setOnClickListener {
                    viewModel.changeFocusedCurrency(indexOfChild(this@row))
                    styleRows()
                }
            }
        }
    }

    private fun toggleEmptyListViewVisibility() {
        val numOfVisibleRows = dragLinearLayout.children.asSequence()
                .filter { it.isVisible }
                .count()
        when (numOfVisibleRows) {
            0 -> emptyList.show()
            else -> emptyList.hide()
        }
    }
}
