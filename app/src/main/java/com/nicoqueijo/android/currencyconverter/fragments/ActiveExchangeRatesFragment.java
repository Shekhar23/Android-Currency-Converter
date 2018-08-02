package com.nicoqueijo.android.currencyconverter.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nicoqueijo.android.currencyconverter.R;
import com.nicoqueijo.android.currencyconverter.adapters.ActiveExchangeRatesRecyclerViewAdapter;

public class ActiveExchangeRatesFragment extends Fragment {

    private static final String TAG = ActiveExchangeRatesFragment.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private FloatingActionButton mFloatingActionButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_active_exchange_rates, container, false);

        mRecyclerView = mView.findViewById(R.id.recycler_view_active_rates);
        mAdapter = new ActiveExchangeRatesRecyclerViewAdapter();
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mFloatingActionButton = mView.findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(v, "FAB clicked", Snackbar.LENGTH_SHORT).show();
                // TODO: Open dialog fragment containing list of all possible currencies
                // This dialog fragment should contain a SearchView on top and a
                // RecyclerView below it. Currencies that are already in the
                // ActiveExchangeRatesFragment should not be contenders for selection.
            }
        });
        return mView;
    }
}